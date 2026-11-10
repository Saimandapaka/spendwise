package com.spendwise.service;

import com.spendwise.dto.CategoryView;
import com.spendwise.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryManagementService {

    private final CategoryRepository categoryRepository;

    public CategoryManagementService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryView> list(long userId) {
        return categoryRepository.findAllWithCounts(userId);
    }

    /** @return true if the rename actually affected a row — false means
     *  either the category doesn't exist or belongs to someone else
     *  (or is a shared system category), so the controller can tell
     *  those apart from a genuine server error. */
    public boolean rename(long userId, long categoryId, String newName) {
        return categoryRepository.renameCategory(userId, categoryId, newName) > 0;
    }

    public void merge(long userId, long fromCategoryId, long toCategoryId) {
        categoryRepository.mergeCategories(userId, fromCategoryId, toCategoryId);
    }
}
