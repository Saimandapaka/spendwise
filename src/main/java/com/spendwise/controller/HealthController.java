package com.spendwise.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Integer categoryCount = jdbc.queryForObject("SELECT count(*) FROM categories", Integer.class);
        Integer transactionCount = jdbc.queryForObject("SELECT count(*) FROM transactions", Integer.class);
        return Map.of(
            "status", "ok",
            "categoriesInDb", categoryCount,
            "transactionsInDb", transactionCount
        );
    }
}
