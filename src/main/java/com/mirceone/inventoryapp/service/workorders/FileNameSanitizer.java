package com.mirceone.inventoryapp.service.workorders;

/**
 * Client filename validation for uploads (path traversal and unsafe characters).
 */
public final class FileNameSanitizer {

    private FileNameSanitizer() {
    }

    public static String sanitizeDisplayName(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Filename is required");
        }
        String name = raw.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        name = name.substring(slash + 1).strip();
        if (name.isBlank()) {
            throw new IllegalArgumentException("Invalid filename");
        }
        if (name.contains("..")) {
            throw new IllegalArgumentException("Invalid filename");
        }
        // Remove ASCII control chars and Windows-invalid characters
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < 32 || c == 127) {
                sb.append('_');
                continue;
            }
            if (c == '<' || c == '>' || c == ':' || c == '"' || c == '|' || c == '?' || c == '*'
                    || c == '/') {
                sb.append('_');
                continue;
            }
            sb.append(c);
        }
        String cleaned = sb.toString().strip();
        if (cleaned.isEmpty() || ".".equals(cleaned) || "..".equals(cleaned)) {
            throw new IllegalArgumentException("Invalid filename");
        }
        if (cleaned.length() > 200) {
            cleaned = cleaned.substring(0, 200);
        }
        return cleaned;
    }

    /**
     * Safe filename suffix for the blob key (".pdf"), or ".bin" when the extension is absent/unsafe.
     */
    public static String storageFileSuffix(String sanitizedName) {
        String ext = ExtensionNormalizer.fromFilename(sanitizedName);
        return ext.isEmpty() ? ".bin" : "." + ext;
    }
}
