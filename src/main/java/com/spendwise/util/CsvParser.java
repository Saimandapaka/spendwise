package com.spendwise.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small, dependency-free CSV parser (handles quoted fields, commas
 * inside quotes, and escaped "" quotes — the common RFC 4180 cases real
 * bank-statement exports actually use). Returns each row as a
 * header-name -> value map, using the first line as headers.
 *
 * Kept dependency-free deliberately: this is straightforward enough that
 * pulling in a CSV library isn't worth it, and it means this class is
 * fully testable with plain Java, no Maven dependency needed.
 */
public class CsvParser {

    public static List<Map<String, String>> parse(String csvText) {
        List<String> lines = splitLines(csvText);
        if (lines.isEmpty()) return List.of();

        List<String> headers = parseLine(lines.get(0));
        List<Map<String, String>> rows = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;

            List<String> values = parseLine(line);
            Map<String, String> row = new LinkedHashMap<>();
            for (int col = 0; col < headers.size(); col++) {
                String value = col < values.size() ? values.get(col) : "";
                row.put(headers.get(col).trim(), value.trim());
            }
            rows.add(row);
        }
        return rows;
    }

    private static List<String> splitLines(String text) {
        // Handles \n, \r\n, and \r line endings without external help.
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\r\n|\r|\n")) {
            lines.add(line);
        }
        return lines;
    }

    /** Parses one CSV line into fields, respecting double-quoted fields
     *  (which may contain commas) and "" as an escaped quote inside one. */
    private static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++; // skip the escaped quote's second character
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
