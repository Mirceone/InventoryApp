package com.mirceone.inventoryapp.service.workorders;

import java.util.regex.Pattern;

/**
 * Validates and normalizes a single folder name (one tree node, never a path).
 */
public final class FolderNameValidator {

    public static final int MAX_LENGTH = 64;

    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9ăâîșțĂÂÎȘȚ _\\-]+$");
    private static final Pattern COLLAPSE_SPACES = Pattern.compile("\\s+");

    private FolderNameValidator() {
    }

    public static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Folder name is required");
        }
        String name = COLLAPSE_SPACES.matcher(raw.strip()).replaceAll(" ");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Folder name is required");
        }
        if (name.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Folder name too long (max " + MAX_LENGTH + " characters)");
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Folder name contains invalid characters");
        }
        return name;
    }
}
