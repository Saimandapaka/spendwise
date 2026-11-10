package com.spendwise.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.util.List;

/**
 * Defines the two tools the model can call. The API enforces these
 * schemas structurally — the model fills in real, typed fields rather
 * than typing free-text JSON we hope is valid.
 */
public class ToolSchemas {

    public static ArrayNode buildTools(ObjectMapper mapper, List<String> categoryNames) {
        ArrayNode tools = mapper.createArrayNode();
        tools.add(logTransactionsTool(mapper));
        tools.add(querySpendingTool(mapper, categoryNames));
        return tools;
    }

    private static ObjectNode logTransactionsTool(ObjectMapper mapper) {
        ObjectNode tool = mapper.createObjectNode();
        tool.put("name", "log_transactions");
        tool.put("description",
            "Log one or more financial transactions the user describes — money they spent or received. " +
            "If the message mentions more than one transaction, include one entry per transaction.");

        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode itemSchema = mapper.createObjectNode();
        itemSchema.put("type", "object");
        ObjectNode itemProps = mapper.createObjectNode();

        itemProps.set("amount", typeNode(mapper, "number", "The transaction amount"));
        itemProps.set("description", typeNode(mapper, "string", "Short description, e.g. merchant or what it was for"));
        itemProps.set("direction", enumNode(mapper, List.of("debit", "credit"), "debit = money spent, credit = money received"));
        itemProps.set("date", typeNode(mapper, "string", "YYYY-MM-DD, resolve relative dates like 'yesterday'"));
        itemProps.set("category", typeNode(mapper, "string", "One of the existing categories. Omit if genuinely unclear."));
        itemProps.set("confidence", enumNode(mapper, List.of("high", "low", "none"), "How sure you are about the category"));
        itemProps.set("suggested_new_category", typeNode(mapper, "string", "Only if no existing category fits well"));
        itemSchema.set("properties", itemProps);
        itemSchema.set("required", mapper.createArrayNode().add("amount").add("direction").add("date").add("confidence"));

        ObjectNode transactionsProp = mapper.createObjectNode();
        transactionsProp.put("type", "array");
        transactionsProp.set("items", itemSchema);

        ObjectNode props = mapper.createObjectNode();
        props.set("transactions", transactionsProp);
        schema.set("properties", props);
        schema.set("required", mapper.createArrayNode().add("transactions"));

        tool.set("input_schema", schema);
        return tool;
    }

    private static ObjectNode querySpendingTool(ObjectMapper mapper, List<String> categoryNames) {
        ObjectNode tool = mapper.createObjectNode();
        tool.put("name", "query_spending");
        tool.put("description", "Answer a question about the user's past spending or income by running a real query against their data.");

        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode props = mapper.createObjectNode();
        props.set("category", typeNode(mapper, "string",
            "One of the existing categories: " + String.join(", ", categoryNames) + ". Omit if the question isn't category-specific."));
        props.set("range", typeNode(mapper, "string",
            "One of: this_month, last_month, this_week, last_week, all_time, a specific YYYY-MM, " +
            "or last_N_months (e.g. last_3_months, last_6_months, last_12_months) for a multi-month span. " +
            "For any 'monthly' request with no explicit period given, default to last_6_months rather than asking."));
        props.set("query_kind", enumNode(mapper, List.of("total", "trend", "monthly_trend", "top_categories", "compare"),
            "total = one number. trend = weekly breakdown (short ranges). " +
            "monthly_trend = spending grouped by calendar month (use this for any 'monthly spending' or 'spending by month' request — this is a BAR CHART). " +
            "top_categories = breakdown by category. compare = current vs previous period."));
        schema.set("properties", props);
        schema.set("required", mapper.createArrayNode().add("range").add("query_kind"));

        tool.set("input_schema", schema);
        return tool;
    }

    private static ObjectNode typeNode(ObjectMapper mapper, String type, String description) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", type);
        node.put("description", description);
        return node;
    }

    private static ObjectNode enumNode(ObjectMapper mapper, List<String> values, String description) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "string");
        ArrayNode enumArray = mapper.createArrayNode();
        values.forEach(enumArray::add);
        node.set("enum", enumArray);
        node.put("description", description);
        return node;
    }

    public static String buildUserMessage(String userMessage, List<String> categoryNames) {
        return """
            Today's date is %s. The user's existing categories are: %s

            If the user's message describes money spent or received, call log_transactions.
            If it's a question about their spending or income, call query_spending.
            If it's neither (small talk, unrelated), just reply normally — don't call a tool.

            IMPORTANT: Prefer making a reasonable assumption over asking a clarifying question.
            If the user's request is broadly clear but leaves a detail unspecified (e.g. "monthly
            spendings" with no explicit date range, or "how am I doing" with no category), pick a
            sensible default (e.g. last_6_months, or all categories) and just answer. Only ask a
            follow-up question if the request is so ambiguous that any tool call would be a guess
            at the user's actual goal, not just a detail.

            User message: "%s"
            """.formatted(LocalDate.now(), String.join(", ", categoryNames), userMessage);
    }
}
