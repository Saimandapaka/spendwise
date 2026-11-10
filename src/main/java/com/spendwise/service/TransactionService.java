package com.spendwise.service;

import com.spendwise.dto.TransactionItem;
import com.spendwise.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

    public TransactionService(TransactionRepository transactionRepository, CategoryService categoryService) {
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
    }

    public record InsertResult(
        BigDecimal amount, String categoryLabel, String description,
        boolean needsReview, boolean possibleDuplicate
    ) {}

    public List<InsertResult> recordAll(long userId, String rawMessage, List<TransactionItem> items) {
        return items.stream()
            .map(item -> recordOne(userId, rawMessage, item))
            .toList();
    }

    private InsertResult recordOne(long userId, String rawMessage, TransactionItem item) {
        BigDecimal amount = item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;
        String direction = "credit".equalsIgnoreCase(item.getDirection()) ? "credit" : "debit";
        String type = "credit".equals(direction) ? "income" : "expense";

        LocalDate date;
        try {
            date = LocalDate.parse(item.getDate());
        } catch (Exception e) {
            date = LocalDate.now();
        }

        boolean lowConfidence = item.getConfidence() == null
            || "low".equalsIgnoreCase(item.getConfidence())
            || "none".equalsIgnoreCase(item.getConfidence());

        // Same user, same direction, same amount, same exact date =
        // treated as a likely duplicate (e.g. the same chat message sent
        // twice by mistake). We still insert it — never silently drop
        // real data, since two genuinely identical transactions in one
        // day is entirely possible — but flag it for review so it's easy
        // to spot and delete from the Transactions screen if it was a
        // mistake.
        boolean possibleDuplicate = transactionRepository.existsSimilar(userId, direction, amount, date);

        Optional<Long> categoryId = categoryService.resolveCategoryId(
            userId, item.getCategory(), item.getSuggestedNewCategory(), type
        );

        boolean needsReview = lowConfidence || categoryId.isEmpty() || possibleDuplicate;

        transactionRepository.insert(
            userId, categoryId.orElse(null), amount, direction,
            item.getDescription(), rawMessage, date,
            item.getConfidence(), needsReview, "chat"
        );

        String categoryLabel = categoryId.isPresent()
            ? (item.getCategory() != null ? item.getCategory() : item.getSuggestedNewCategory())
            : null;

        return new InsertResult(amount, categoryLabel, item.getDescription(), needsReview, possibleDuplicate);
    }

    /** Plain-text summary of what was just inserted — fed to the model as
     *  the ONLY facts it's allowed to use when writing the confirmation
     *  reply. Not shown to the user directly. */
    public static String factsForModel(List<InsertResult> results) {
        StringBuilder facts = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            var r = results.get(i);
            if (i > 0) facts.append("; ");
            facts.append(ReplyBuilder.formatMoney(r.amount()))
                .append(r.needsReview() ? " (category not yet set, needs review)" : " under " + r.categoryLabel());
            if (r.description() != null && !r.description().isBlank()) {
                facts.append(" for ").append(r.description());
            }
            if (r.possibleDuplicate()) {
                facts.append(" [NOTE: this matches an existing transaction already logged with the same amount, direction, and date — mention to the user that this might be a duplicate, and that they can check/delete it from the Transactions screen if so]");
            }
        }
        return "Transactions just logged: " + facts;
    }
}
