package com.spendwise.repository;

import com.spendwise.dto.CategoryView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class CategoryRepository {

    private final JdbcTemplate jdbc;

    public CategoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<CategoryView> CATEGORY_VIEW_MAPPER = (rs, rowNum) -> new CategoryView(
        rs.getLong("id"),
        rs.getString("name"),
        rs.getString("type"),
        rs.getBoolean("is_system"),
        rs.getInt("transaction_count"),
        rs.getInt("needs_review_count")
    );

    public List<String> findAllNamesForUser(long userId) {
        return jdbc.queryForList(
            "SELECT name FROM categories WHERE user_id IS NULL OR user_id = ? ORDER BY name",
            String.class, userId
        );
    }

    public Optional<Long> findIdByName(long userId, String name) {
        List<Long> ids = jdbc.queryForList(
            "SELECT id FROM categories WHERE (user_id IS NULL OR user_id = ?) AND LOWER(name) = LOWER(?) LIMIT 1",
            Long.class, userId, name
        );
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
    }

    public long createUserCategory(long userId, String name, String type) {
        jdbc.update(
            "INSERT INTO categories (user_id, name, type, is_system, created_by) VALUES (?, ?, ?, false, 'llm')",
            userId, name, type
        );
        return findIdByName(userId, name).orElseThrow();
    }

    /** Every category visible to this user, with how many transactions
     *  use it and how many of those still need review — for the
     *  categories management screen. */
    public List<CategoryView> findAllWithCounts(long userId) {
        return jdbc.query("""
            SELECT c.id, c.name, c.type, c.is_system,
                   COUNT(t.id) AS transaction_count,
                   COUNT(t.id) FILTER (WHERE t.needs_review) AS needs_review_count
            FROM categories c
            LEFT JOIN transactions t ON t.category_id = c.id AND t.user_id = ?
            WHERE c.user_id IS NULL OR c.user_id = ?
            GROUP BY c.id, c.name, c.type, c.is_system
            ORDER BY c.name
            """, CATEGORY_VIEW_MAPPER, userId, userId);
    }

    /** Renames a category. Only allowed on categories that belong to this
     *  user (system categories are shared and shouldn't be renamed by
     *  one user) — enforced by the WHERE clause, not just the caller. */
    public int renameCategory(long userId, long categoryId, String newName) {
        return jdbc.update(
            "UPDATE categories SET name = ? WHERE id = ? AND user_id = ?",
            newName, categoryId, userId
        );
    }

    /** Moves every transaction from one category to another, then deletes
     *  the now-empty source category. Both steps happen in one
     *  transaction so a failure partway through can't leave transactions
     *  silently orphaned. Only the source category needs to belong to
     *  this user — merging INTO a shared system category is fine. */
    @Transactional
    public void mergeCategories(long userId, long fromCategoryId, long toCategoryId) {
        jdbc.update(
            "UPDATE transactions SET category_id = ? WHERE category_id = ? AND user_id = ?",
            toCategoryId, fromCategoryId, userId
        );
        jdbc.update(
            "DELETE FROM categories WHERE id = ? AND user_id = ?",
            fromCategoryId, userId
        );
    }
}
