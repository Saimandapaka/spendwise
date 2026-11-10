package com.spendwise.repository;

import com.spendwise.dto.TransactionView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Every method here is a fixed, hand-written query. The LLM only ever
 * chooses which method to call and with what parameters — the SQL itself
 * never changes based on model output.
 */
@Repository
public class TransactionRepository {

    private final JdbcTemplate jdbc;

    public TransactionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<TransactionView> TRANSACTION_VIEW_MAPPER = (rs, rowNum) -> new TransactionView(
        rs.getLong("id"),
        rs.getBigDecimal("amount"),
        rs.getString("direction"),
        rs.getString("description"),
        rs.getObject("category_id") != null ? rs.getLong("category_id") : null,
        rs.getString("category_name"),
        rs.getObject("transaction_date", LocalDate.class),
        rs.getBoolean("needs_review")
    );

    public void insert(long userId, Long categoryId, BigDecimal amount, String direction,
                        String description, String rawText, LocalDate date,
                        String confidence, boolean needsReview, String source) {
        jdbc.update("""
            INSERT INTO transactions
              (user_id, category_id, amount, direction, description, raw_text, transaction_date, confidence, needs_review, source)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            userId, categoryId, amount, direction, description, rawText, date, confidence, needsReview, source
        );
    }

    public BigDecimal getSpending(long userId, Long categoryId, LocalDate start, LocalDate end) {
        String sql = "SELECT COALESCE(SUM(amount),0) FROM transactions " +
            "WHERE user_id = ? AND direction = 'debit' AND transaction_date >= ? AND transaction_date < ?" +
            (categoryId != null ? " AND category_id = ?" : "");

        return categoryId != null
            ? jdbc.queryForObject(sql, BigDecimal.class, userId, start, end, categoryId)
            : jdbc.queryForObject(sql, BigDecimal.class, userId, start, end);
    }

    public List<Map<String, Object>> getTopCategories(long userId, LocalDate start, LocalDate end) {
        return jdbc.queryForList("""
            SELECT c.name AS name, SUM(t.amount) AS total
            FROM transactions t JOIN categories c ON c.id = t.category_id
            WHERE t.user_id = ? AND t.direction = 'debit'
              AND t.transaction_date >= ? AND t.transaction_date < ?
            GROUP BY c.name ORDER BY total DESC
            """, userId, start, end);
    }

    /** Weekly totals — used for the short-range "trend" query kind. */
    public List<Map<String, Object>> getWeeklyTrend(long userId, LocalDate start, LocalDate end) {
        return jdbc.queryForList("""
            SELECT date_trunc('week', transaction_date)::date AS period_start, SUM(amount) AS total
            FROM transactions
            WHERE user_id = ? AND direction = 'debit'
              AND transaction_date >= ? AND transaction_date < ?
            GROUP BY period_start ORDER BY period_start
            """, userId, start, end);
    }

    /** Monthly totals — used for "monthly_trend", e.g. "bar chart of
     *  monthly spendings" over a multi-month range like last_6_months. */
    public List<Map<String, Object>> getMonthlyTrend(long userId, LocalDate start, LocalDate end) {
        return jdbc.queryForList("""
            SELECT date_trunc('month', transaction_date)::date AS period_start, SUM(amount) AS total
            FROM transactions
            WHERE user_id = ? AND direction = 'debit'
              AND transaction_date >= ? AND transaction_date < ?
            GROUP BY period_start ORDER BY period_start
            """, userId, start, end);
    }

    /** Filtered, most-recent-first transaction list for the dashboard.
     *  Every filter is optional; only non-null ones narrow the query. */
    public List<TransactionView> findAll(long userId, Long categoryId, LocalDate start, LocalDate end, String search) {
        StringBuilder sql = new StringBuilder("""
            SELECT t.id, t.amount, t.direction, t.description, t.category_id,
                   c.name AS category_name, t.transaction_date, t.needs_review
            FROM transactions t LEFT JOIN categories c ON c.id = t.category_id
            WHERE t.user_id = ?
            """);
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (categoryId != null) {
            sql.append(" AND t.category_id = ?");
            params.add(categoryId);
        }
        if (start != null) {
            sql.append(" AND t.transaction_date >= ?");
            params.add(start);
        }
        if (end != null) {
            sql.append(" AND t.transaction_date < ?");
            params.add(end);
        }
        if (search != null && !search.isBlank()) {
            sql.append(" AND t.description ILIKE ?");
            params.add("%" + search + "%");
        }

        sql.append(" ORDER BY t.transaction_date DESC, t.id DESC LIMIT 200");

        return jdbc.query(sql.toString(), TRANSACTION_VIEW_MAPPER, params.toArray());
    }

    public List<TransactionView> findNeedsReview(long userId) {
        return jdbc.query("""
            SELECT t.id, t.amount, t.direction, t.description, t.category_id,
                   c.name AS category_name, t.transaction_date, t.needs_review
            FROM transactions t LEFT JOIN categories c ON c.id = t.category_id
            WHERE t.user_id = ? AND t.needs_review = true
            ORDER BY t.transaction_date DESC
            """, TRANSACTION_VIEW_MAPPER, userId);
    }

    /** Fixes a transaction's category (e.g. resolving a needs-review item)
     *  and clears the review flag. */
    public void updateCategory(long userId, long transactionId, long categoryId) {
        jdbc.update("""
            UPDATE transactions SET category_id = ?, needs_review = false
            WHERE id = ? AND user_id = ?
            """, categoryId, transactionId, userId);
    }

    /** True if this user already has a transaction with the same
     *  direction, amount, and exact date — the signal used to flag
     *  likely duplicate entries (e.g. the same chat message sent twice). */
    public boolean existsSimilar(long userId, String direction, BigDecimal amount, LocalDate date) {
        Boolean exists = jdbc.queryForObject("""
            SELECT EXISTS (
              SELECT 1 FROM transactions
              WHERE user_id = ? AND direction = ? AND amount = ? AND transaction_date = ?
            )
            """, Boolean.class, userId, direction, amount, date);
        return Boolean.TRUE.equals(exists);
    }

    /** Full edit of a transaction — used by the Transactions screen. */
    public int update(long userId, long id, BigDecimal amount, String direction,
                       String description, Long categoryId, LocalDate date) {
        return jdbc.update("""
            UPDATE transactions
            SET amount = ?, direction = ?, description = ?, category_id = ?, transaction_date = ?, needs_review = false
            WHERE id = ? AND user_id = ?
            """, amount, direction, description, categoryId, date, id, userId);
    }

    public int delete(long userId, long id) {
        return jdbc.update("DELETE FROM transactions WHERE id = ? AND user_id = ?", id, userId);
    }
}
