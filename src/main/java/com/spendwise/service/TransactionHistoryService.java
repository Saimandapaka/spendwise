package com.spendwise.service;

import com.spendwise.dto.TransactionView;
import com.spendwise.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionHistoryService {

    private final TransactionRepository transactionRepository;

    public TransactionHistoryService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<TransactionView> list(long userId, Long categoryId, LocalDate start, LocalDate end, String search) {
        return transactionRepository.findAll(userId, categoryId, start, end, search);
    }

    public List<TransactionView> needsReview(long userId) {
        return transactionRepository.findNeedsReview(userId);
    }

    public void fixCategory(long userId, long transactionId, long categoryId) {
        transactionRepository.updateCategory(userId, transactionId, categoryId);
    }

    /** @return true if the edit actually applied — false means the
     *  transaction doesn't exist or doesn't belong to this user, so the
     *  controller can return 404 instead of a false "success". */
    public boolean update(long userId, long id, BigDecimal amount, String direction,
                           String description, Long categoryId, LocalDate date) {
        return transactionRepository.update(userId, id, amount, direction, description, categoryId, date) > 0;
    }

    public boolean delete(long userId, long id) {
        return transactionRepository.delete(userId, id) > 0;
    }
}
