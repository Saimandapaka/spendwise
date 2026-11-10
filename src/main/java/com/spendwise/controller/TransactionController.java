package com.spendwise.controller;

import com.spendwise.dto.TransactionCategoryUpdateRequest;
import com.spendwise.dto.TransactionUpdateRequest;
import com.spendwise.dto.TransactionView;
import com.spendwise.service.TransactionHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionHistoryService transactionHistoryService;

    public TransactionController(TransactionHistoryService transactionHistoryService) {
        this.transactionHistoryService = transactionHistoryService;
    }

    /** GET /api/transactions?userId=1&categoryId=3&start=2026-08-01&end=2026-09-01&search=swiggy
     *  Every filter param is optional. */
    @GetMapping
    public List<TransactionView> list(
        @RequestParam Long userId,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) LocalDate start,
        @RequestParam(required = false) LocalDate end,
        @RequestParam(required = false) String search
    ) {
        return transactionHistoryService.list(userId, categoryId, start, end, search);
    }

    @GetMapping("/needs-review")
    public List<TransactionView> needsReview(@RequestParam Long userId) {
        return transactionHistoryService.needsReview(userId);
    }

    @PatchMapping("/{id}/category")
    public void fixCategory(
        @PathVariable Long id,
        @RequestParam Long userId,
        @RequestBody TransactionCategoryUpdateRequest request
    ) {
        transactionHistoryService.fixCategory(userId, id, request.getCategoryId());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
        @PathVariable Long id,
        @RequestParam Long userId,
        @RequestBody TransactionUpdateRequest request
    ) {
        LocalDate date;
        try {
            date = LocalDate.parse(request.getTransactionDate());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
        boolean updated = transactionHistoryService.update(
            userId, id, request.getAmount(), request.getDirection(),
            request.getDescription(), request.getCategoryId(), date
        );
        return updated ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam Long userId) {
        boolean deleted = transactionHistoryService.delete(userId, id);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
