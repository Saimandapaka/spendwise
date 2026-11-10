package com.spendwise.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the range strings the LLM extracts ("this_month", "last_week", a
 * specific "2026-08", "last_6_months", etc.) into concrete [start, end)
 * date bounds your SQL queries can use.
 */
public class DateRangeResolver {

    public record Range(LocalDate start, LocalDate endExclusive) {}

    private static final Pattern LAST_N_MONTHS = Pattern.compile("last_(\\d+)_months?");

    public static Range resolve(String rangeToken, LocalDate today) {
        if (rangeToken == null) rangeToken = "this_month";

        Matcher m = LAST_N_MONTHS.matcher(rangeToken);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(1));
            LocalDate start = today.withDayOfMonth(1).minusMonths(n - 1L);
            LocalDate end = today.withDayOfMonth(1).plusMonths(1); // include the current, possibly partial, month
            return new Range(start, end);
        }

        switch (rangeToken) {
            case "this_month": {
                LocalDate start = today.withDayOfMonth(1);
                return new Range(start, start.plusMonths(1));
            }
            case "last_month": {
                LocalDate start = today.withDayOfMonth(1).minusMonths(1);
                return new Range(start, start.plusMonths(1));
            }
            case "this_week": {
                LocalDate start = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                return new Range(start, start.plusWeeks(1));
            }
            case "last_week": {
                LocalDate thisWeekStart = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                LocalDate start = thisWeekStart.minusWeeks(1);
                return new Range(start, thisWeekStart);
            }
            case "all_time":
                return new Range(LocalDate.of(2000, 1, 1), today.plusDays(1));
            default:
                try {
                    YearMonth ym = YearMonth.parse(rangeToken);
                    LocalDate start = ym.atDay(1);
                    return new Range(start, start.plusMonths(1));
                } catch (Exception e) {
                    LocalDate start = today.withDayOfMonth(1);
                    return new Range(start, start.plusMonths(1));
                }
        }
    }

    /** Given a range, returns the immediately preceding equivalent period —
     *  used for "compare" questions. */
    public static Range previousEquivalent(String rangeToken, LocalDate today) {
        Matcher m = LAST_N_MONTHS.matcher(rangeToken == null ? "" : rangeToken);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(1));
            LocalDate currentStart = today.withDayOfMonth(1).minusMonths(n - 1L);
            LocalDate previousStart = currentStart.minusMonths(n);
            return new Range(previousStart, currentStart);
        }

        switch (rangeToken == null ? "this_month" : rangeToken) {
            case "this_week": return resolve("last_week", today);
            case "last_week": {
                Range lastWeek = resolve("last_week", today);
                return new Range(lastWeek.start().minusWeeks(1), lastWeek.start());
            }
            case "last_month": {
                LocalDate start = today.withDayOfMonth(1).minusMonths(2);
                return new Range(start, start.plusMonths(1));
            }
            case "this_month":
            default:
                return resolve("last_month", today);
        }
    }
}
