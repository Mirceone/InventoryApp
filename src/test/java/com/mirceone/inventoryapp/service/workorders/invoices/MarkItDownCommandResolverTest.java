package com.mirceone.inventoryapp.service.workorders.invoices;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkItDownCommandResolverTest {

    private static final Path VENV_MARKITDOWN =
            Path.of(System.getProperty("user.dir"), ".venv-markitdown", "bin", "markitdown");

    @Test
    void defaultResolvesToExplicitConfiguredCommand() {
        List<String> resolved = MarkItDownCommandResolver.resolve("/custom/bin/markitdown");
        assertEquals("/custom/bin/markitdown", resolved.getFirst());
    }

    @Test
    @EnabledIf("venvMarkItDownExists")
    void defaultPrefersProjectVenvOverBareMarkitdown() {
        List<String> resolved = MarkItDownCommandResolver.resolve("markitdown");
        assertEquals(VENV_MARKITDOWN.toAbsolutePath().toString(), resolved.getFirst());
        assertTrue(MarkItDownCommandResolver.probeHelp(resolved));
    }

    @SuppressWarnings("unused")
    static boolean venvMarkItDownExists() {
        return Files.isExecutable(VENV_MARKITDOWN);
    }
}
