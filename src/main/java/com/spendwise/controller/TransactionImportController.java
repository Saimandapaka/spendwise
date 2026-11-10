package com.spendwise.controller;

import com.spendwise.dto.ImportSummary;
import com.spendwise.service.FileImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/transactions/import")
public class TransactionImportController {

    private final FileImportService fileImportService;

    public TransactionImportController(FileImportService fileImportService) {
        this.fileImportService = fileImportService;
    }

    @PostMapping
    public ResponseEntity<?> importFile(@RequestParam Long userId, @RequestParam MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty.");
        }
        try {
            ImportSummary summary = fileImportService.importFile(userId, file);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Could not read that file: " + e.getMessage());
        }
    }
}
