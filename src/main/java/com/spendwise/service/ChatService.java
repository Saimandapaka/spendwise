package com.spendwise.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.spendwise.dto.QuerySpendingInput;
import com.spendwise.dto.TransactionItem;
import com.spendwise.llm.AnthropicClient;
import com.spendwise.llm.ToolSchemas;
import com.spendwise.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates one /api/chat request as a real two-turn tool-use
 * conversation:
 *
 *   Turn 1: the model calls ZERO OR MORE tools (log_transactions and/or
 *           query_spending). For an open-ended ask like "give me
 *           insights", the model may call query_spending more than once
 *           (e.g. a breakdown AND a trend) — every tool_use block must
 *           get executed and answered, not just the first one, or the
 *           API rejects the next turn.
 *
 *   Turn 2 (only if at least one tool was called): the backend executes
 *           every real DB action, hands the model ALL the resulting
 *           facts, and the model writes one natural reply synthesizing
 *           them. Genuinely model-generated text — it's just never free
 *           to invent a number.
 */
@Service
public class ChatService {

    private final AnthropicClient anthropicClient;
    private final CategoryRepository categoryRepository;
    private final TransactionService transactionService;
    private final QueryService queryService;

    public ChatService(AnthropicClient anthropicClient, CategoryRepository categoryRepository,
                        TransactionService transactionService, QueryService queryService) {
        this.anthropicClient = anthropicClient;
        this.categoryRepository = categoryRepository;
        this.transactionService = transactionService;
        this.queryService = queryService;
    }

    public ReplyBuilder.ChatReply handle(long userId, String message) {
        ObjectMapper mapper = anthropicClient.mapper();
        List<String> categories = categoryRepository.findAllNamesForUser(userId);

        ArrayNode messages = mapper.createArrayNode();
        messages.add(userMessage(mapper, ToolSchemas.buildUserMessage(message, categories)));

        ArrayNode tools = ToolSchemas.buildTools(mapper, categories);

        // Turn 1
        JsonNode response1 = anthropicClient.sendConversation(messages, tools);
        JsonNode content1 = response1.get("content");

        List<JsonNode> toolUseBlocks = findAllBlocksByType(content1, "tool_use");

        if (toolUseBlocks.isEmpty()) {
            // No tool called — the model just replied directly. Covers
            // off-topic messages gracefully instead of forcing them
            // through an add/question classifier.
            String directText = concatenateText(content1);
            return new ReplyBuilder.ChatReply(
                directText.isBlank() ? "Sorry, I'm not sure how to help with that." : directText,
                "text", null
            );
        }

        // Execute EVERY tool call the model made, collecting one tool_result
        // per call (required by the API) and picking one chart to show.
        List<String> toolUseIds = new ArrayList<>();
        List<String> factsPerCall = new ArrayList<>();
        String render = "text";
        ReplyBuilder.ChartData chart = null;

        for (JsonNode toolUseBlock : toolUseBlocks) {
            String toolName = toolUseBlock.get("name").asText();
            String toolUseId = toolUseBlock.get("id").asText();
            JsonNode input = toolUseBlock.get("input");

            ToolExecutionResult result = executeTool(userId, message, mapper, toolName, input);
            toolUseIds.add(toolUseId);
            factsPerCall.add(result.facts());

            // First chart-bearing result wins — good enough default when
            // multiple queries were run for one open-ended "insights" ask.
            if (chart == null && result.chart() != null) {
                render = result.render();
                chart = result.chart();
            }
        }

        // Turn 2: one tool_result per tool_use id (required by the API),
        // with the "write the final reply" instruction appended onto the
        // last one.
        ArrayNode toolResults = mapper.createArrayNode();
        StringBuilder allFacts = new StringBuilder();
        for (int i = 0; i < toolUseIds.size(); i++) {
            String content = factsPerCall.get(i);
            if (i == toolUseIds.size() - 1) {
                content += " Reply to the user naturally in a few sentences, synthesizing all the facts across every tool result above. Use only these facts, never invent a number. No markdown.";
            }
            toolResults.add(toolResultBlock(mapper, toolUseIds.get(i), content));
            if (!allFacts.isEmpty()) allFacts.append(" ");
            allFacts.append(factsPerCall.get(i));
        }

        messages.add(assistantMessage(mapper, content1));
        messages.add(userContentMessage(mapper, toolResults));

        JsonNode response2 = anthropicClient.sendConversation(messages, null);
        String finalText = concatenateText(response2.get("content"));

        if (finalText.isBlank()) {
            finalText = allFacts.toString(); // last-resort safety net, never a hardcoded sentence
        }

        return new ReplyBuilder.ChatReply(finalText, render, chart);
    }

    private record ToolExecutionResult(String facts, String render, ReplyBuilder.ChartData chart) {}

    /** Runs the real backend action for one tool call. No LLM involved in
     *  this method at all — only DB reads/writes. */
    private ToolExecutionResult executeTool(long userId, String rawMessage, ObjectMapper mapper, String toolName, JsonNode input) {
        if ("log_transactions".equals(toolName)) {
            List<TransactionItem> items = mapper.convertValue(
                input.get("transactions"), new TypeReference<List<TransactionItem>>() {}
            );
            var results = transactionService.recordAll(userId, rawMessage, items);
            return new ToolExecutionResult(TransactionService.factsForModel(results), "text", null);
        }
        if ("query_spending".equals(toolName)) {
            QuerySpendingInput qsi = mapper.convertValue(input, QuerySpendingInput.class);
            var computed = queryService.compute(userId, qsi);
            return new ToolExecutionResult(computed.factsForModel(), computed.render(), computed.chart());
        }
        return new ToolExecutionResult("Something was requested that isn't supported yet.", "text", null);
    }

    private ObjectNode userMessage(ObjectMapper mapper, String text) {
        ObjectNode msg = mapper.createObjectNode();
        msg.put("role", "user");
        msg.put("content", text);
        return msg;
    }

    private ObjectNode assistantMessage(ObjectMapper mapper, JsonNode content) {
        ObjectNode msg = mapper.createObjectNode();
        msg.put("role", "assistant");
        msg.set("content", content);
        return msg;
    }

    private ObjectNode userContentMessage(ObjectMapper mapper, ArrayNode content) {
        ObjectNode msg = mapper.createObjectNode();
        msg.put("role", "user");
        msg.set("content", content);
        return msg;
    }

    private ObjectNode toolResultBlock(ObjectMapper mapper, String toolUseId, String content) {
        ObjectNode block = mapper.createObjectNode();
        block.put("type", "tool_result");
        block.put("tool_use_id", toolUseId);
        block.put("content", content);
        return block;
    }

    private List<JsonNode> findAllBlocksByType(JsonNode contentArray, String type) {
        List<JsonNode> matches = new ArrayList<>();
        if (contentArray == null) return matches;
        for (JsonNode block : contentArray) {
            if (type.equals(block.path("type").asText())) matches.add(block);
        }
        return matches;
    }

    private String concatenateText(JsonNode contentArray) {
        if (contentArray == null) return "";
        StringBuilder sb = new StringBuilder();
        for (JsonNode block : contentArray) {
            if ("text".equals(block.path("type").asText())) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(block.path("text").asText());
            }
        }
        return sb.toString().trim();
    }
}
