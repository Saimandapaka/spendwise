package com.spendwise.controller;

import com.spendwise.dto.ChatRequest;
import com.spendwise.service.ChatService;
import com.spendwise.service.ReplyBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/api/chat")
    public ReplyBuilder.ChatReply chat(@RequestBody ChatRequest request) {
        try {
            return chatService.handle(request.getUserId(), request.getMessage());
        } catch (Exception e) {
            System.err.println("Chat request failed for message: " + request.getMessage());
            e.printStackTrace();
            return new ReplyBuilder.ChatReply(
                "Sorry, I couldn't quite understand that — try rephrasing it.",
                "text", null
            );
        }
    }
}
