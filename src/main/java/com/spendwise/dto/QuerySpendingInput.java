package com.spendwise.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QuerySpendingInput {

    private String category;
    private String range;

    @JsonProperty("query_kind")
    private String queryKind;

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getRange() { return range; }
    public void setRange(String range) { this.range = range; }

    public String getQueryKind() { return queryKind; }
    public void setQueryKind(String queryKind) { this.queryKind = queryKind; }
}
