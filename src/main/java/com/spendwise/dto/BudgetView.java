package com.spendwise.dto;

import java.math.BigDecimal;

public class BudgetView {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private BigDecimal monthlyLimit;
    private BigDecimal spentThisMonth;
    private int percentUsed;
    private String status; // "ok" | "warning" | "over"

    public BudgetView(Long id, Long categoryId, String categoryName, BigDecimal monthlyLimit,
                       BigDecimal spentThisMonth, int percentUsed, String status) {
        this.id = id;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.monthlyLimit = monthlyLimit;
        this.spentThisMonth = spentThisMonth;
        this.percentUsed = percentUsed;
        this.status = status;
    }

    public Long getId() { return id; }
    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public BigDecimal getMonthlyLimit() { return monthlyLimit; }
    public BigDecimal getSpentThisMonth() { return spentThisMonth; }
    public int getPercentUsed() { return percentUsed; }
    public String getStatus() { return status; }
}
