package com.mirceone.inventoryapp.service.workorders.invoices;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies project venv MarkItDown when present (post-fix smoke test). */
class MarkItDownCliExtractorVenvTest {

    private static final Path VENV_MARKITDOWN =
            Path.of(System.getProperty("user.dir"), ".venv-markitdown", "bin", "markitdown");

    @Test
    @EnabledIf("venvMarkItDownExists")
    void extractFromPlainTextFile() throws Exception {
        AppIntegrationProperties props = new AppIntegrationProperties();
        props.getInvoices().setMarkitdownCommand(VENV_MARKITDOWN.toString());

        Path temp = Files.createTempFile("invoice-smoke-", ".txt");
        Files.writeString(temp, "Invoice #123\nTotal: 100 RON\n");

        MarkItDownCommandResolver resolver = new MarkItDownCommandResolver(props);
        resolver.resolveAtStartup();
        MarkItDownCliExtractor extractor = new MarkItDownCliExtractor(props, resolver);
        String markdown = extractor.extract(temp, "text/plain");

        assertFalse(markdown.isBlank());
        assertTrue(markdown.toLowerCase().contains("invoice") || markdown.contains("100"));
        Files.deleteIfExists(temp);
    }

    @SuppressWarnings("unused")
    static boolean venvMarkItDownExists() {
        return Files.isExecutable(VENV_MARKITDOWN);
    }
}
