package com.spendwise.service;

import com.spendwise.dto.QuerySpendingInput;
import com.spendwise.repository.CategoryRepository;
import com.spendwise.repository.TransactionRepository;
import com.spendwise.util.DateRangeResolver;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Runs the real SQL for a "question" and returns the real numbers plus
 * chart data — fully deterministic, zero LLM involvement in the numbers.
 * Reply TEXT is not produced here; the model writes that in a follow-up
 * turn using `factsForModel` as its only source of truth (see ChatService).
 */
@Service
public class QueryService {

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy");

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public QueryService(TransactionRepository transactionRepository, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    public record ComputedAnswer(String factsForModel, String render, ReplyBuilder.ChartData chart) {}

    public ComputedAnswer compute(long userId, QuerySpendingInput input) {
        String queryKind = input.getQueryKind() != null ? input.getQueryKind() : "total";
        String rangeToken = input.getRange() != null ? input.getRange() : "this_month";
        String rangeLabel = humanizeRange(rangeToken);

        Long categoryId = input.getCategory() != null
            ? categoryRepository.findIdByName(userId, input.getCategory()).orElse(null)
            : null;

        DateRangeResolver.Range range = DateRangeResolver.resolve(rangeToken, LocalDate.now());

        return switch (queryKind) {
            case "trend" -> {
                List<Map<String, Object>> rows = transactionRepository.getWeeklyTrend(userId, range.start(), range.endExclusive());
                List<String> labels = rows.stream().map(r -> r.get("period_start").toString()).toList();
                List<BigDecimal> totals = rows.stream().map(r -> (BigDecimal) r.get("total")).toList();

                String facts = "Weekly spending trend " + rangeLabel + ": " + joinPairs(labels, totals) + ".";
                var chart = labels.isEmpty() ? null
                    : new ReplyBuilder.ChartData("line", labels, List.of(new ReplyBuilder.ChartSeries("Spending", totals)));
                yield new ComputedAnswer(facts, chart != null ? "chart" : "text", chart);
            }
            case "monthly_trend" -> {
                List<Map<String, Object>> rows = transactionRepository.getMonthlyTrend(userId, range.start(), range.endExclusive());
                // Go through toString() -> parse, rather than casting directly to
                // LocalDate: JdbcTemplate's default row mapping may hand back a
                // java.sql.Date for a DATE column depending on driver behavior,
                // and a direct cast to LocalDate would throw ClassCastException
                // in that case. toString() on either type is a safe "yyyy-MM-dd".
                List<String> labels = rows.stream()
                    .map(r -> LocalDate.parse(r.get("period_start").toString()).format(MONTH_LABEL))
                    .toList();
                List<BigDecimal> totals = rows.stream().map(r -> (BigDecimal) r.get("total")).toList();

                String facts = "Monthly spending " + rangeLabel + ": " + joinPairs(labels, totals) + ".";
                var chart = labels.isEmpty() ? null
                    : new ReplyBuilder.ChartData("bar", labels, List.of(new ReplyBuilder.ChartSeries("Spending", totals)));
                yield new ComputedAnswer(facts, chart != null ? "chart" : "text", chart);
            }
            case "top_categories" -> {
                List<Map<String, Object>> rows = transactionRepository.getTopCategories(userId, range.start(), range.endExclusive());
                List<String> names = rows.stream().map(r -> (String) r.get("name")).toList();
                List<BigDecimal> totals = rows.stream().map(r -> (BigDecimal) r.get("total")).toList();

                String facts = "Spending by category " + rangeLabel + ": " + joinPairs(names, totals) + ".";
                var chart = names.isEmpty() ? null
                    : new ReplyBuilder.ChartData("pie", names, List.of(new ReplyBuilder.ChartSeries("Spending", totals)));
                yield new ComputedAnswer(facts, chart != null ? "chart" : "text", chart);
            }
            case "compare" -> {
                BigDecimal current = transactionRepository.getSpending(userId, categoryId, range.start(), range.endExclusive());
                DateRangeResolver.Range prev = DateRangeResolver.previousEquivalent(rangeToken, LocalDate.now());
                BigDecimal previous = transactionRepository.getSpending(userId, categoryId, prev.start(), prev.endExclusive());

                String subject = input.getCategory() != null ? input.getCategory() + " spending" : "total spending";
                String facts = subject + ": current period " + ReplyBuilder.formatMoney(current)
                    + ", previous period " + ReplyBuilder.formatMoney(previous) + ".";
                var chart = new ReplyBuilder.ChartData("bar", List.of("Previous", "Current"),
                    List.of(new ReplyBuilder.ChartSeries(subject, List.of(previous, current))));
                yield new ComputedAnswer(facts, "chart", chart);
            }
            default -> { // "total"
                BigDecimal total = transactionRepository.getSpending(userId, categoryId, range.start(), range.endExclusive());
                String facts = (input.getCategory() != null ? "Spending on " + input.getCategory() : "Total spending")
                    + " " + rangeLabel + ": " + ReplyBuilder.formatMoney(total) + ".";
                yield new ComputedAnswer(facts, "text", null);
            }
        };
    }

    private String joinPairs(List<String> labels, List<BigDecimal> totals) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < labels.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(labels.get(i)).append(" ").append(ReplyBuilder.formatMoney(totals.get(i)));
        }
        return sb.isEmpty() ? "no spending recorded" : sb.toString();
    }

    private String humanizeRange(String token) {
        if (token != null && token.matches("last_\\d+_months?")) {
            String n = token.replaceAll("\\D+", "");
            return "over the last " + n + " months";
        }
        return switch (token) {
            case "this_month" -> "this month";
            case "last_month" -> "last month";
            case "this_week" -> "this week";
            case "last_week" -> "last week";
            case "all_time" -> "overall";
            default -> "in " + token;
        };
    }
}
