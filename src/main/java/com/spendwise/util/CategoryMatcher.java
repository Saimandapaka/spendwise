package com.spendwise.util;

import java.util.List;
import java.util.Optional;

public class CategoryMatcher {

    public static Optional<String> findExisting(String candidate, List<String> existingNames) {
        if (candidate == null) return Optional.empty();
        return existingNames.stream()
            .filter(name -> name.equalsIgnoreCase(candidate.trim()))
            .findFirst();
    }

    public static boolean isWorthCreating(String suggested, List<String> existingNames) {
        if (suggested == null || suggested.isBlank()) return false;
        return findExisting(suggested, existingNames).isEmpty();
    }
}
