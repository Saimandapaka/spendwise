package com.spendwise.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionItem {

    private BigDecimal amount;
    private String description;
    private String direction;
    private String date;
    private String category;
    private String confidence;

    @JsonProperty("suggested_new_category")
    private String suggestedNewCategory;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }

    public String getSuggestedNewCategory() { return suggestedNewCategory; }
    public void setSuggestedNewCategory(String suggestedNewCategory) { this.suggestedNewCategory = suggestedNewCategory; }
}
