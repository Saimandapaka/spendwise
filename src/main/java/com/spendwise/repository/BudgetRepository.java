package com.spendwise.repository;

import com.spendwise.dto.BudgetView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Repository
public class BudgetRepository {

    private final JdbcTemplate jdbc;

    public BudgetRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<BudgetView> BUDGET_VIEW_MAPPER = (rs, rowNum) -> {
        BigDecimal limit = rs.getBigDecimal("monthly_limit");
        BigDecimal spent = rs.getBigDecimal("spent_this_month");
        int percent = limit.signum() == 0
            ? 0
            : spent.multiply(BigDecimal.valueOf(100)).divide(limit, 0, RoundingMode.HALF_UP).intValue();
        String status = percent >= 100 ? "over" : percent >= 80 ? "warning" : "ok";

        return new BudgetView(
            rs.getLong("id"),
            rs.getLong("category_id"),
            rs.getString("category_name"),
            limit,
            spent,
            percent,
            status
        );
    };

    /** Every budget for this user, joined with real spend for the
     *  CURRENT month (computed live, never stored) so the percentage and
     *  status are always accurate, not stale. */
    public List<BudgetView> findAllForUser(long userId) {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1);

        return jdbc.query("""
            SELECT b.id, b.category_id, c.name AS category_name, b.monthly_limit,
                   COALESCE(SUM(t.amount) FILTER (
                     WHERE t.direction = 'debit' AND t.transaction_date >= ? AND t.transaction_date < ?
                   ), 0) AS spent_this_month
            FROM budgets b
            JOIN categories c ON c.id = b.category_id
            LEFT JOIN transactions t ON t.category_id = b.category_id AND t.user_id = b.user_id
            WHERE b.user_id = ?
            GROUP BY b.id, b.category_id, c.name, b.monthly_limit
            ORDER BY c.name
            """, BUDGET_VIEW_MAPPER, monthStart, monthEnd, userId);
    }

    /** Creates or updates the limit for a category — one budget per
     *  category per user, so this is an upsert, not a plain insert. */
    public void upsert(long userId, long categoryId, BigDecimal monthlyLimit) {
        jdbc.update("""
            INSERT INTO budgets (user_id, category_id, monthly_limit, updated_at)
            VALUES (?, ?, ?, now())
            ON CONFLICT (user_id, category_id)
            DO UPDATE SET monthly_limit = EXCLUDED.monthly_limit, updated_at = now()
            """, userId, categoryId, monthlyLimit);
    }

    public void delete(long userId, long budgetId) {
        jdbc.update("DELETE FROM budgets WHERE id = ? AND user_id = ?", budgetId, userId);
    }
}
