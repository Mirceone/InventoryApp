package com.mirceone.inventoryapp.service.documents;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates and normalizes relative folder paths (max 3 segments, safe characters).
 */
public final class FolderPathSanitizer {

    private static final int MAX_SEGMENTS = 3;
    private static final int MAX_SEGMENT_LENGTH = 64;
    private static final Pattern SEGMENT_PATTERN =
            Pattern.compile("^[a-zA-Z0-9ăâîșțĂÂÎȘȚ _\\-]+$");

    private FolderPathSanitizer() {
    }

    public static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String normalized = raw.replace('\\', '/').strip();
        if (normalized.contains("..")) {
            throw new IllegalArgumentException("Invalid folder path");
        }
        String[] parts = normalized.split("/");
        List<String> segments = new ArrayList<>();
        for (String part : parts) {
            String seg = part.strip();
            if (seg.isEmpty() || ".".equals(seg)) {
                continue;
            }
            if (seg.length() > MAX_SEGMENT_LENGTH) {
                seg = seg.substring(0, MAX_SEGMENT_LENGTH);
            }
            if (!SEGMENT_PATTERN.matcher(seg).matches()) {
                throw new IllegalArgumentException("Invalid folder segment: " + seg);
            }
            segments.add(seg);
            if (segments.size() > MAX_SEGMENTS) {
                throw new IllegalArgumentException("Folder path too deep");
            }
        }
        return String.join("/", segments);
    }

    public static String storageSegment(String folderPath) {
        String path = sanitize(folderPath);
        return path.isEmpty() ? "" : path + "/";
    }

    public static String normalizeForCompare(String folderPath) {
        if (folderPath == null || folderPath.isBlank()) {
            return "";
        }
        return sanitize(folderPath);
    }
}
