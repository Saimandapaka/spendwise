package com.spendwise.dto;

import java.util.List;

public class ImportSummary {
    private int totalRows;
    private int imported;
    private int needsReview;
    private List<String> errors;

    public ImportSummary(int totalRows, int imported, int needsReview, List<String> errors) {
        this.totalRows = totalRows;
        this.imported = imported;
        this.needsReview = needsReview;
        this.errors = errors;
    }

    public int getTotalRows() { return totalRows; }
    public int getImported() { return imported; }
    public int getNeedsReview() { return needsReview; }
    public List<String> getErrors() { return errors; }
}
