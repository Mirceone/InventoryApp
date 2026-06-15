package com.mirceone.inventoryapp.service.workorders;

import java.util.regex.Pattern;

public final class WorkOrderTextSanitizer {

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;
    private static final Pattern COLLAPSE_SPACES = Pattern.compile("\\s+");

    private WorkOrderTextSanitizer() {
    }

    public static String sanitizeRequiredName(String raw, String fieldLabel) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(fieldLabel + " is required");
        }
        String value = collapseAndTrim(raw);
        if (value.isEmpty()) {
            throw new IllegalArgumentException(fieldLabel + " is required");
        }
        return truncate(value, MAX_NAME_LENGTH);
    }

    public static String sanitizeOptionalDescription(String raw) {
        if (raw == null) {
            return null;
        }
        String value = collapseAndTrim(raw);
        if (value.isEmpty()) {
            return null;
        }
        return truncate(value, MAX_DESCRIPTION_LENGTH);
    }

    private static String collapseAndTrim(String raw) {
        return COLLAPSE_SPACES.matcher(raw.strip()).replaceAll(" ");
    }

    private static String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
