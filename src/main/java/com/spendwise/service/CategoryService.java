package com.spendwise.service;

import com.spendwise.repository.CategoryRepository;
import com.spendwise.util.CategoryMatcher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<String> allNamesForUser(long userId) {
        return categoryRepository.findAllNamesForUser(userId);
    }

    public Optional<Long> resolveCategoryId(long userId, String extractedCategory, String suggestedNewCategory, String type) {
        List<String> existing = allNamesForUser(userId);

        Optional<String> matched = CategoryMatcher.findExisting(extractedCategory, existing);
        if (matched.isPresent()) {
            return categoryRepository.findIdByName(userId, matched.get());
        }

        if (CategoryMatcher.isWorthCreating(suggestedNewCategory, existing)) {
            long newId = categoryRepository.createUserCategory(userId, suggestedNewCategory, type);
            return Optional.of(newId);
        }

        return Optional.empty();
    }
}
