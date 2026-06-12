package com.mirceone.inventoryapp.service.workorders;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Canonical form for extension rules and file extensions: lowercase, no leading dot.
 */
public final class ExtensionNormalizer {

    public static final int MAX_LENGTH = 16;

    private static final Pattern EXTENSION_PATTERN = Pattern.compile("^[a-z0-9]{1," + MAX_LENGTH + "}$");

    private ExtensionNormalizer() {
    }

    /**
     * Normalizes a user-supplied rule extension ("pdf", ".PDF" -> "pdf"); throws on invalid input.
     */
    public static String normalizeRuleExtension(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Extension is required");
        }
        String ext = raw.strip().toLowerCase(Locale.ROOT);
        if (ext.startsWith(".")) {
            ext = ext.substring(1);
        }
        if (!EXTENSION_PATTERN.matcher(ext).matches()) {
            throw new IllegalArgumentException("Invalid extension: " + raw);
        }
        return ext;
    }

    /**
     * Extracts the normalized extension from a filename, or empty string when absent/invalid.
     */
    public static String fromFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!EXTENSION_PATTERN.matcher(ext).matches()) {
            return "";
        }
        return ext;
    }
}
