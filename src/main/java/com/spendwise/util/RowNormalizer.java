package com.spendwise.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Turns one raw row (header name -> value, from CSV or Excel) into a
 * normalized transaction. Column names are matched flexibly, since real
 * bank statement exports vary a lot ("Narration" vs "Description" vs
 * "Particulars", "Txn Date" vs "Value Date", etc).
 */
public class RowNormalizer {

    public record NormalizedRow(
        LocalDate date, BigDecimal amount, String direction, String description
    ) {}

    /** Thrown when a row genuinely can't be parsed (bad/missing date,
     *  no usable amount) — the caller records this as a skipped row
     *  rather than failing the whole import. */
    public static class RowParseException extends RuntimeException {
        public RowParseException(String message) { super(message); }
    }

    private static final List<String> DATE_KEYS = List.of("date", "transaction date", "txn date", "value date");
    private static final List<String> DESCRIPTION_KEYS = List.of("description", "narration", "details", "particulars", "remarks");
    private static final List<String> AMOUNT_KEYS = List.of("amount");
    private static final List<String> DEBIT_KEYS = List.of("debit", "withdrawal", "debit amount");
    private static final List<String> CREDIT_KEYS = List.of("credit", "deposit", "credit amount");
    private static final List<String> TYPE_KEYS = List.of("type", "direction", "dr/cr");

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("dd MMM yyyy")
    );

    public static NormalizedRow normalize(Map<String, String> row) {
        Map<String, String> lower = lowercaseKeys(row);

        String dateStr = findFirst(lower, DATE_KEYS)
            .orElseThrow(() -> new RowParseException("No date column found"));
        LocalDate date = parseDate(dateStr);

        String description = findFirst(lower, DESCRIPTION_KEYS).orElse("Imported transaction");

        // Two layouts are common: a single Debit and Credit column
        // (bank-statement style), or one Amount column plus a Type
        // column saying which it is.
        Optional<String> debit = findFirst(lower, DEBIT_KEYS);
        Optional<String> credit = findFirst(lower, CREDIT_KEYS);

        BigDecimal amount;
        String direction;

        if (debit.isPresent() || credit.isPresent()) {
            BigDecimal debitAmt = parseAmount(debit.orElse(""));
            BigDecimal creditAmt = parseAmount(credit.orElse(""));
            if (debitAmt.signum() > 0) {
                amount = debitAmt;
                direction = "debit";
            } else if (creditAmt.signum() > 0) {
                amount = creditAmt;
                direction = "credit";
            } else {
                throw new RowParseException("Both debit and credit are empty/zero");
            }
        } else {
            String amountStr = findFirst(lower, AMOUNT_KEYS)
                .orElseThrow(() -> new RowParseException("No amount, debit, or credit column found"));
            BigDecimal rawAmount = parseAmount(amountStr);
            if (rawAmount.signum() == 0) {
                throw new RowParseException("Amount is zero or unparseable: " + amountStr);
            }
            // Negative amount, or a "Type"/"Direction" column saying so, means debit.
            String typeValue = findFirst(lower, TYPE_KEYS).orElse("").toLowerCase();
            boolean isCredit = rawAmount.signum() > 0 && (typeValue.contains("credit") || typeValue.equals("cr"));
            boolean isDebitByType = typeValue.contains("debit") || typeValue.equals("dr");

            if (rawAmount.signum() < 0) {
                direction = "debit";
            } else if (isCredit) {
                direction = "credit";
            } else if (isDebitByType) {
                direction = "debit";
            } else {
                direction = "debit"; // no sign, no type column — assume spend, the common case
            }
            amount = rawAmount.abs();
        }

        return new NormalizedRow(date, amount, direction, description);
    }

    private static Map<String, String> lowercaseKeys(Map<String, String> row) {
        Map<String, String> result = new java.util.LinkedHashMap<>();
        row.forEach((k, v) -> result.put(k.trim().toLowerCase(), v));
        return result;
    }

    private static Optional<String> findFirst(Map<String, String> lowerRow, List<String> keys) {
        for (String key : keys) {
            String value = lowerRow.get(key);
            if (value != null && !value.isBlank()) return Optional.of(value.trim());
        }
        return Optional.empty();
    }

    private static LocalDate parseDate(String raw) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(raw, fmt);
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        throw new RowParseException("Could not parse date: " + raw);
    }

    private static BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return BigDecimal.ZERO;
        // Strip common currency symbols/commas real spreadsheets include.
        String cleaned = raw.replaceAll("[₹$,\\s]", "");
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
