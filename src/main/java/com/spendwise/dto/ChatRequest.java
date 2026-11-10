package com.spendwise.dto;

public class ChatRequest {
    private Long userId;
    private String message;

    public ChatRequest() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
