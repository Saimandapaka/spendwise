package com.spendwise.util;

import org.apache.poi.ss.usermodel.*;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads an .xlsx file's first sheet into the same header-name -> value
 * row shape CsvParser produces, so RowNormalizer can handle both
 * formats identically. Uses Apache POI, the standard library for this —
 * unlike CsvParser, this genuinely needs an external dependency since
 * .xlsx is a binary zip-based format, not something worth hand-parsing.
 */
public class ExcelParser {

    public static List<Map<String, String>> parse(InputStream inputStream) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<Map<String, String>> rows = new ArrayList<>();

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) return rows;

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cellToString(cell).trim());
            }

            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Map<String, String> rowMap = new LinkedHashMap<>();
                boolean anyValue = false;
                for (int c = 0; c < headers.size(); c++) {
                    Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String value = cell != null ? cellToString(cell).trim() : "";
                    if (!value.isBlank()) anyValue = true;
                    rowMap.put(headers.get(c), value);
                }
                if (anyValue) rows.add(rowMap); // skip fully blank rows
            }
            return rows;
        }
    }

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Converts any Excel cell type to a plain string, the same shape
     *  CsvParser produces, so RowNormalizer doesn't need to know which
     *  file format the data came from. Excel date cells are stored as
     *  numbers internally, so those need explicit detection — treating
     *  them as a plain number would silently corrupt every date. */
    private static String cellToString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    LocalDate date = cell.getLocalDateTimeCellValue().toLocalDate();
                    return date.format(ISO_DATE);
                }
                double num = cell.getNumericCellValue();
                // Whole numbers print as "450" not "450.0"
                if (num == Math.floor(num) && !Double.isInfinite(num)) {
                    return String.valueOf((long) num);
                }
                return String.valueOf(num);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }
}
