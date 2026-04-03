package com.mirceone.inventoryapp.bootstrap;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creeaza baza de date PostgreSQL daca nu exista.
 * Ruleaza inainte de Spring, in main(), ca sa nu pice Flyway datasource init.
 */
public final class DBCreator {

    private static final Pattern POSTGRES_URL_PATTERN = Pattern.compile(
            "^jdbc:postgresql://(?<host>[^:/]+)(?::(?<port>\\d+))?/(?<db>[^?]+).*$",
            Pattern.CASE_INSENSITIVE
    );
    private static final String DOTENV_FILE = ".env";

    private DBCreator() {
    }

    public static void createDatabaseIfMissing() {
        Map<String, String> dotenvValues = loadDotenvFromProjectRoot();

        boolean enabled = getConfigAsBoolean(dotenvValues, "APP_DB_CREATE_IF_MISSING", true);
        if (!enabled) {
            return;
        }

        String targetJdbcUrl = getRequiredConfig(dotenvValues, "DB_URL");
        String username = getRequiredConfig(dotenvValues, "DB_USERNAME");
        String password = getRequiredConfig(dotenvValues, "DB_PASSWORD");

        ParsedJdbc target = parsePostgresUrl(targetJdbcUrl).orElseThrow(() ->
                new IllegalArgumentException("DB_URL invalid: " + targetJdbcUrl)
        );

        String maintenanceDb = getConfig(dotenvValues, "DB_MAINTENANCE_DB", "postgres");

        String adminJdbcUrl = buildJdbcUrl(target.host, target.port, maintenanceDb);

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("PostgreSQL driver not found on classpath", e);
        }

        try (Connection conn = DriverManager.getConnection(adminJdbcUrl, username, password)) {
            if (databaseExists(conn, target.database)) {
                System.out.println("[DBCreator] Database '" + target.database + "' already exists");
                return;
            }

            System.out.println("[DBCreator] Creating database '" + target.database + "'...");

            String safeDbName = quoteIdentifier(target.database);
            try (Statement stmt = conn.createStatement()) {
                // Folosim owner-ul curent (user-ul cu care ne conectam la DB-ul de mentenanta).
                stmt.executeUpdate("CREATE DATABASE " + safeDbName);
            }

            System.out.println("[DBCreator] Database created: '" + target.database + "'");
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Unable to create/verify database '" + target.database + "'. " +
                            "Check DB credentials (DB_USERNAME/DB_PASSWORD) and privileges. " +
                            "Admin url: " + adminJdbcUrl,
                    e
            );
        }
    }

    private static boolean databaseExists(Connection conn, String dbName) throws SQLException {
        String sql = "SELECT 1 FROM pg_database WHERE datname = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dbName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static String getConfig(Map<String, String> dotenvValues, String key, String defaultValue) {
        String fromDotenv = dotenvValues.get(key);
        if (fromDotenv != null && !fromDotenv.isBlank()) {
            return fromDotenv;
        }

        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? defaultValue : v;
    }

    private static boolean getConfigAsBoolean(Map<String, String> dotenvValues, String key, boolean defaultValue) {
        String v = getConfig(dotenvValues, key, null);
        if (v == null || v.isBlank()) return defaultValue;
        return Boolean.parseBoolean(v.trim().toLowerCase(Locale.ROOT));
    }

    private static String getRequiredConfig(Map<String, String> dotenvValues, String key) {
        String value = getConfig(dotenvValues, key, null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing required config key '" + key + "'. " +
                            "Set it in .env (project root) or as environment variable."
            );
        }
        return value;
    }

    private static Map<String, String> loadDotenvFromProjectRoot() {
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

    private static Optional<ParsedJdbc> parsePostgresUrl(String jdbcUrl) {
        Matcher m = POSTGRES_URL_PATTERN.matcher(jdbcUrl);
        if (!m.matches()) return Optional.empty();

        String host = m.group("host");
        String portStr = m.group("port");
        String db = m.group("db");
        int port = (portStr == null || portStr.isBlank()) ? 5432 : Integer.parseInt(portStr);
        return Optional.of(new ParsedJdbc(host, port, db));
    }

    private static String buildJdbcUrl(String host, int port, String dbName) {
        return "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
    }

    private static String quoteIdentifier(String identifier) {
        // Minim defensiv: doar scăpări pentru ghilimele double.
        // În teză e ok, dar pentru production aș valida whitelist strict.
        String escaped = identifier.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private record ParsedJdbc(String host, int port, String database) {
    }
}

