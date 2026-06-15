package com.mirceone.inventoryapp.service.workorders.classification;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Filename and MIME heuristics ported from the legacy dossier taxonomy. Returns a suggested
 * folder name only when rules are confident; the caller must verify the folder exists in the
 * work order tree.
 */
@Component
public class FileNameHeuristicClassifier {

    public static final String DOCUMENTS = "Documents";
    public static final String RENDERS = "Renders";
    public static final String POZE = "Poze";
    public static final String FACTURI = "Facturi";

    private static final Pattern INVOICE_FILENAME = Pattern.compile(
            ".*(factur|invoice|faktur|bon\\s*fiscal).*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern RENDER_FILENAME = Pattern.compile(
            "render",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Map<String, String> SYNONYM_TO_CANONICAL = buildSynonyms();

    public Optional<String> hint(String originalFilename, String mimeType) {
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
            return Optional.of(DOCUMENTS);
        }
        return Optional.empty();
    }

    public Optional<String> canonicalFolderName(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return Optional.empty();
        }
        String stripped = candidate.strip();
        for (String canonical : SYNONYM_TO_CANONICAL.values()) {
            if (canonical.equalsIgnoreCase(stripped)) {
                return Optional.of(canonical);
            }
        }
        String synonym = SYNONYM_TO_CANONICAL.get(stripped.toLowerCase(Locale.ROOT));
        if (synonym != null) {
            return Optional.of(synonym);
        }
        int slash = stripped.lastIndexOf('/');
        if (slash >= 0 && slash < stripped.length() - 1) {
            return canonicalFolderName(stripped.substring(slash + 1));
        }
        return Optional.of(stripped);
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

    private static Map<String, String> buildSynonyms() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("documente", DOCUMENTS);
        map.put("documents", DOCUMENTS);
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
        map.put("misc", "Misc");
        map.put("sketchup", DOCUMENTS);
        map.put("cad", DOCUMENTS);
        map.put("office", DOCUMENTS);
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
