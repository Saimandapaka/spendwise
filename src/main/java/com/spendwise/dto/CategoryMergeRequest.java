package com.spendwise.dto;

public class CategoryMergeRequest {
    private Long fromCategoryId;
    private Long toCategoryId;

    public Long getFromCategoryId() { return fromCategoryId; }
    public void setFromCategoryId(Long fromCategoryId) { this.fromCategoryId = fromCategoryId; }

    public Long getToCategoryId() { return toCategoryId; }
    public void setToCategoryId(Long toCategoryId) { this.toCategoryId = toCategoryId; }
}
