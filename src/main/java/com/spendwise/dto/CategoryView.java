package com.spendwise.dto;

public class CategoryView {
    private Long id;
    private String name;
    private String type;
    private boolean isSystem;
    private int transactionCount;
    private int needsReviewCount;

    public CategoryView(Long id, String name, String type, boolean isSystem, int transactionCount, int needsReviewCount) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.isSystem = isSystem;
        this.transactionCount = transactionCount;
        this.needsReviewCount = needsReviewCount;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public boolean isSystem() { return isSystem; }
    public int getTransactionCount() { return transactionCount; }
    public int getNeedsReviewCount() { return needsReviewCount; }
}
