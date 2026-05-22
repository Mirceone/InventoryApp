package com.mirceone.inventoryapp.service.documents;

import java.util.regex.Pattern;

public final class DossierNameSanitizer {

    private static final int MAX_LENGTH = 255;
    private static final Pattern COLLAPSE_SPACES = Pattern.compile("\\s+");

    private DossierNameSanitizer() {
    }

    public static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Dossier name is required");
        }
        String name = COLLAPSE_SPACES.matcher(raw.strip()).replaceAll(" ");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Dossier name is required");
        }
        if (name.length() > MAX_LENGTH) {
            name = name.substring(0, MAX_LENGTH);
        }
        return name;
    }
}
