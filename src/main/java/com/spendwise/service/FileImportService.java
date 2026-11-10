package com.spendwise.service;

import com.spendwise.dto.ImportSummary;
import com.spendwise.repository.CategoryRepository;
import com.spendwise.repository.TransactionRepository;
import com.spendwise.util.CategoryMatcher;
import com.spendwise.util.CsvParser;
import com.spendwise.util.ExcelParser;
import com.spendwise.util.RowNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Imports transactions from an uploaded CSV or Excel file. Deliberately
 * does NOT call the LLM for categorization — matching bank-statement rows
 * to categories is done with the same plain string matching used
 * elsewhere (CategoryMatcher), and anything that doesn't match confidently
 * is flagged needs_review, same as a low-confidence chat entry. This
 * keeps a large import (hundreds of rows) free to run, consistent with
 * how the rest of this app treats API credits.
 */
@Service
public class FileImportService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public FileImportService(TransactionRepository transactionRepository, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    public ImportSummary importFile(long userId, MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        List<Map<String, String>> rows;

        if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            rows = ExcelParser.parse(file.getInputStream());
        } else {
            // Treat anything else (including no/unknown extension) as CSV —
            // the most common export format when in doubt.
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            rows = CsvParser.parse(text);
        }

        List<String> existingCategories = categoryRepository.findAllNamesForUser(userId);

        int imported = 0;
        int needsReview = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2; // +1 for 0-index, +1 for the header row
            try {
                RowNormalizer.NormalizedRow normalized = RowNormalizer.normalize(rows.get(i));

                String categoryText = rows.get(i).entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase("category"))
                    .map(Map.Entry::getValue)
                    .findFirst().orElse(null);

                Optional<String> matchedCategory = CategoryMatcher.findExisting(categoryText, existingCategories);
                Long categoryId = matchedCategory
                    .flatMap(name -> categoryRepository.findIdByName(userId, name))
                    .orElse(null);

                boolean possibleDuplicate = transactionRepository.existsSimilar(
                    userId, normalized.direction(), normalized.amount(), normalized.date()
                );
                boolean rowNeedsReview = categoryId == null || possibleDuplicate;

                transactionRepository.insert(
                    userId, categoryId, normalized.amount(), normalized.direction(),
                    normalized.description(), "imported from " + filename, normalized.date(),
                    rowNeedsReview ? "low" : "high", rowNeedsReview, "import"
                );

                imported++;
                if (rowNeedsReview) needsReview++;

            } catch (RowNormalizer.RowParseException e) {
                errors.add("Row " + rowNumber + ": " + e.getMessage());
            } catch (Exception e) {
                errors.add("Row " + rowNumber + ": unexpected error — " + e.getMessage());
            }
        }

        return new ImportSummary(rows.size(), imported, needsReview, errors);
    }
}
