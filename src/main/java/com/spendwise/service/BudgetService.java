package com.spendwise.service;

import com.spendwise.dto.BudgetView;
import com.spendwise.repository.BudgetRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;

    public BudgetService(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    public List<BudgetView> list(long userId) {
        return budgetRepository.findAllForUser(userId);
    }

    public void setLimit(long userId, long categoryId, BigDecimal monthlyLimit) {
        budgetRepository.upsert(userId, categoryId, monthlyLimit);
    }

    public void delete(long userId, long budgetId) {
        budgetRepository.delete(userId, budgetId);
    }
}
