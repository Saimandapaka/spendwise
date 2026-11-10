package com.spendwise.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * THIS IS THE ONLY CLASS IN THE APP THAT SPENDS ANTHROPIC API CREDITS.
 *
 * Uses real tool use (function calling) — the model's structured
 * arguments arrive as an already-parsed JSON object inside the response
 * envelope (a `tool_use` content block's `input` field), never as free
 * text we have to parse ourselves.
 */
@Component
public class AnthropicClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-sonnet-4-5-20250929";

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${anthropic.api.key}")
    private String apiKey;

    public JsonNode sendConversation(ArrayNode messages, ArrayNode tools) {
        try {
            var body = objectMapper.createObjectNode();
            body.put("model", MODEL);
            body.put("max_tokens", 1024);
            body.set("messages", messages);
            if (tools != null && !tools.isEmpty()) {
                body.set("tools", tools);
            }

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Anthropic API call failed (" + response.statusCode() + "): " + response.body());
            }

            return objectMapper.readTree(response.body());

        } catch (Exception e) {
            throw new RuntimeException("Failed to call Anthropic API", e);
        }
    }

    public ObjectMapper mapper() {
        return objectMapper;
    }
}
