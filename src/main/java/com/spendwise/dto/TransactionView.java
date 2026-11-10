package com.spendwise.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionView {
    private Long id;
    private BigDecimal amount;
    private String direction;
    private String description;
    private Long categoryId;
    private String categoryName;
    private LocalDate transactionDate;
    private boolean needsReview;

    public TransactionView(Long id, BigDecimal amount, String direction, String description,
                            Long categoryId, String categoryName, LocalDate transactionDate, boolean needsReview) {
        this.id = id;
        this.amount = amount;
        this.direction = direction;
        this.description = description;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.transactionDate = transactionDate;
        this.needsReview = needsReview;
    }

    public Long getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public String getDirection() { return direction; }
    public String getDescription() { return description; }
    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public boolean isNeedsReview() { return needsReview; }
}
