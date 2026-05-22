package com.mirceone.inventoryapp.service.firms;

import java.util.regex.Pattern;

public final class FirmNameSanitizer {

    private static final int MAX_LENGTH = 255;
    private static final Pattern COLLAPSE_SPACES = Pattern.compile("\\s+");

    private FirmNameSanitizer() {
    }

    public static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Firm name is required");
        }
        String name = COLLAPSE_SPACES.matcher(raw.strip()).replaceAll(" ");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Firm name is required");
        }
        if (name.length() > MAX_LENGTH) {
            name = name.substring(0, MAX_LENGTH);
        }
        return name;
    }
}
