package com.spendwise.dto;

import java.math.BigDecimal;

public class BudgetRequest {
    private Long categoryId;
    private BigDecimal monthlyLimit;

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public BigDecimal getMonthlyLimit() { return monthlyLimit; }
    public void setMonthlyLimit(BigDecimal monthlyLimit) { this.monthlyLimit = monthlyLimit; }
}
