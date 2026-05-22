package com.mirceone.inventoryapp.service.documents;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Exactly five root folders per dossier. AI and rules only assign files into these paths.
 */
public final class DocumentFolderTaxonomy {

    public static final String DOCUMENTE = "Documente";
    public static final String RENDERS = "Renders";
    public static final String POZE = "Poze";
    public static final String FACTURI = "Facturi";
    public static final String MISC = "Misc";

    private static final List<String> ALL_PATHS = List.of(
            DOCUMENTE,
            RENDERS,
            POZE,
            FACTURI,
            MISC
    );

    private static final Map<String, String> SYNONYM_TO_CANONICAL = buildSynonyms();

    private static final Pattern INVOICE_FILENAME = Pattern.compile(
            ".*(factur|invoice|faktur|bon\\s*fiscal).*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern RENDER_FILENAME = Pattern.compile(
            "render",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private DocumentFolderTaxonomy() {
    }

    public static List<String> allPaths() {
        return ALL_PATHS;
    }

    public static String fallback() {
        return MISC;
    }

    public static boolean isFinalPath(String path) {
        return path != null && ALL_PATHS.contains(path);
    }

    public static List<String> allowedForPrompt() {
        return ALL_PATHS;
    }

    /**
     * Deterministic folder from file metadata, or empty if AI should decide.
     */
    public static Optional<String> ruleFolderHint(String originalFilename, String mimeType) {
        String ext = extension(originalFilename);
        String mime = mimeType != null ? mimeType.strip().toLowerCase(Locale.ROOT) : "";

        if (looksLikeInvoice(originalFilename, ext, mime)) {
            return Optional.of(FACTURI);
        }
        if (looksLikeRenderImage(originalFilename, mime, ext)) {
            return Optional.of(RENDERS);
        }
        if (mime.startsWith("image/") || isImageExt(ext)) {
            return Optional.of(POZE);
        }
        if (".skp".equals(ext) || mime.contains("sketchup")
                || ".dwg".equals(ext) || ".dxf".equals(ext) || mime.contains("dwg") || mime.contains("dxf")
                || isOfficeExt(ext) || mime.contains("msword") || mime.contains("spreadsheet")
                || mime.contains("wordprocessing") || mime.contains("excel")
                || ".pdf".equals(ext) || "application/pdf".equals(mime)) {
            return Optional.of(DOCUMENTE);
        }
        return Optional.empty();
    }

    /**
     * Maps a stored or suggested path to one of the five canonical folders.
     */
    public static String toCanonicalFolderPath(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return storedPath;
        }
        for (String allowed : ALL_PATHS) {
            if (allowed.equalsIgnoreCase(storedPath.strip())) {
                return allowed;
            }
        }
        String synonym = SYNONYM_TO_CANONICAL.get(normalizedKey(storedPath));
        if (synonym != null) {
            return synonym;
        }
        return MISC;
    }

    public static String resolve(String candidate, Optional<String> ruleHint) {
        String canonical = canonicalFromCandidate(candidate);
        if (canonical != null) {
            return canonical;
        }
        if (ruleHint.isPresent() && isFinalPath(ruleHint.get())) {
            return ruleHint.get();
        }
        return MISC;
    }

    private static String canonicalFromCandidate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized;
        try {
            normalized = FolderPathSanitizer.sanitize(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (normalized.isEmpty()) {
            return null;
        }
        return toCanonicalFolderPath(normalized);
    }

    private static boolean looksLikeRenderImage(String filename, String mime, String ext) {
        boolean image = mime.startsWith("image/") || isImageExt(ext);
        return image && filename != null && RENDER_FILENAME.matcher(filename).find();
    }

    private static boolean looksLikeInvoice(String filename, String ext, String mime) {
        if (filename != null && INVOICE_FILENAME.matcher(filename).matches()) {
            return true;
        }
        return ".xml".equals(ext) && mime.contains("xml");
    }

    private static String normalizedKey(String path) {
        return path.strip().toLowerCase(Locale.ROOT);
    }

    private static Map<String, String> buildSynonyms() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("documente", DOCUMENTE);
        map.put("renders", RENDERS);
        map.put("render", RENDERS);
        map.put("renderi", RENDERS);
        map.put("rendering", RENDERS);
        map.put("renderizari", RENDERS);
        map.put("renderuri", RENDERS);
        map.put("poze", POZE);
        map.put("facturi", FACTURI);
        map.put("factura", FACTURI);
        map.put("factur", FACTURI);
        map.put("invoice", FACTURI);
        map.put("invoices", FACTURI);
        map.put("misc", MISC);
        map.put("sketchup", DOCUMENTE);
        map.put("cad", DOCUMENTE);
        map.put("office", DOCUMENTE);
        return Map.copyOf(map);
    }

    private static boolean isImageExt(String ext) {
        return switch (ext) {
            case ".jpg", ".jpeg", ".png", ".webp", ".heic", ".gif", ".bmp", ".tif", ".tiff" -> true;
            default -> false;
        };
    }

    private static boolean isOfficeExt(String ext) {
        return switch (ext) {
            case ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".odt", ".ods" -> true;
            default -> false;
        };
    }

    private static String extension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        return filename.substring(dot).toLowerCase(Locale.ROOT);
    }
}
