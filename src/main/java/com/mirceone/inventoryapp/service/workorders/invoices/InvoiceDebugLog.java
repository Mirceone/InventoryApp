package com.mirceone.inventoryapp.service.workorders.invoices;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

/** Temporary debug instrumentation — remove after diagnosis. */
public final class InvoiceDebugLog {

    private static final Path LOG_PATH =
            Path.of("/Users/mirceone/IdeaProjects/InventoryApp/.cursor/debug-3329fa.log");

    private InvoiceDebugLog() {
    }

    public static void write(String hypothesisId, String location, String message, Map<String, Object> data) {
        try {
            StringBuilder json = new StringBuilder();
            json.append("{\"sessionId\":\"3329fa\"");
            json.append(",\"hypothesisId\":\"").append(escape(hypothesisId)).append("\"");
            json.append(",\"location\":\"").append(escape(location)).append("\"");
            json.append(",\"message\":\"").append(escape(message)).append("\"");
            json.append(",\"timestamp\":").append(System.currentTimeMillis());
            json.append(",\"data\":").append(mapToJson(data));
            json.append("}\n");
            Files.writeString(LOG_PATH, json.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // debug only
        }
    }

    private static String mapToJson(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : data.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(escape(e.getKey())).append("\":");
            Object v = e.getValue();
            if (v == null) {
                sb.append("null");
            } else if (v instanceof Number || v instanceof Boolean) {
                sb.append(v);
            } else {
                sb.append("\"").append(escape(String.valueOf(v))).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    public static Map<String, Object> data(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }
}
