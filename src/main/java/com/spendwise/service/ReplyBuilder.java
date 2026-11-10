package com.spendwise.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

public class ReplyBuilder {

    public record ChartSeries(String name, List<BigDecimal> data) {}
    public record ChartData(String type, List<String> labels, List<ChartSeries> series) {}
    public record ChatReply(String replyText, String render, ChartData chart) {}

    public static String formatMoney(BigDecimal amount) {
        BigDecimal rounded = amount.setScale(0, RoundingMode.HALF_UP);
        return "\u20B9" + String.format(Locale.US, "%,d", rounded.longValue());
    }
}
