package com.mirceone.inventoryapp.bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Incarca variabilele din {@code .env} in system properties daca nu sunt deja setate
 * (environment sau -D). Permite Spring sa citeasca chei precum {@code RESEND_API_KEY}.
 */
public final class DotenvLoader {

    private static final String DOTENV_FILE = ".env";

    private DotenvLoader() {
    }

    public static void loadIntoSystemPropertiesIfAbsent() {
        Map<String, String> values = loadDotenvFromProjectRoot();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null || value.isBlank()) {
                continue;
            }
            if (System.getenv(key) != null && !System.getenv(key).isBlank()) {
                continue;
            }
            if (System.getProperty(key) != null && !System.getProperty(key).isBlank()) {
                continue;
            }
            System.setProperty(key, value);
        }
    }

    static Map<String, String> loadDotenvFromProjectRoot() {
        Map<String, String> values = new HashMap<>();
        Path dotenvPath = Path.of(System.getProperty("user.dir"), DOTENV_FILE);
        if (!Files.exists(dotenvPath)) {
            return values;
        }

        try {
            for (String line : Files.readAllLines(dotenvPath)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    continue;
                }

                String key = trimmed.substring(0, separator).trim();
                String value = trimmed.substring(separator + 1).trim();
                values.put(key, stripWrappingQuotes(value));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read .env file from project root: " + dotenvPath, e);
        }

        return values;
    }

    private static String stripWrappingQuotes(String value) {
        if (value.length() >= 2) {
            if ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'"))) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
