package com.spendwise.controller;

import com.spendwise.dto.BudgetRequest;
import com.spendwise.dto.BudgetView;
import com.spendwise.service.BudgetService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public List<BudgetView> list(@RequestParam Long userId) {
        return budgetService.list(userId);
    }

    /** Create or update the limit for a category — one budget per
     *  category, so POSTing an existing category's limit just updates it. */
    @PostMapping
    public void setLimit(@RequestParam Long userId, @RequestBody BudgetRequest request) {
        budgetService.setLimit(userId, request.getCategoryId(), request.getMonthlyLimit());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @RequestParam Long userId) {
        budgetService.delete(userId, id);
    }
}
