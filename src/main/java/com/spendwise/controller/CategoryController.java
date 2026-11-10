package com.spendwise.controller;

import com.spendwise.dto.CategoryMergeRequest;
import com.spendwise.dto.CategoryRenameRequest;
import com.spendwise.dto.CategoryView;
import com.spendwise.service.CategoryManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryManagementService categoryManagementService;

    public CategoryController(CategoryManagementService categoryManagementService) {
        this.categoryManagementService = categoryManagementService;
    }

    @GetMapping
    public List<CategoryView> list(@RequestParam Long userId) {
        return categoryManagementService.list(userId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> rename(
        @PathVariable Long id,
        @RequestParam Long userId,
        @RequestBody CategoryRenameRequest request
    ) {
        boolean updated = categoryManagementService.rename(userId, id, request.getName());
        // 404 rather than a silent no-op tells the UI the rename didn't
        // happen (e.g. it's a shared system category it can't rename)
        // instead of it assuming success.
        return updated ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/merge")
    public void merge(@RequestParam Long userId, @RequestBody CategoryMergeRequest request) {
        categoryManagementService.merge(userId, request.getFromCategoryId(), request.getToCategoryId());
    }
}
