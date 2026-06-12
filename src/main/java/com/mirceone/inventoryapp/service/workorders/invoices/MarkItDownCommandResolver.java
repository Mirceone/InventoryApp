package com.mirceone.inventoryapp.service.workorders.invoices;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Resolves a working MarkItDown CLI invocation. When the configured value is the default {@code markitdown},
 * also probes the project-local {@code .venv-markitdown/bin/markitdown} and {@code python3 -m markitdown}.
 */
@Component
@ConditionalOnProperty(name = "app.invoices.extractor", havingValue = "markitdown", matchIfMissing = true)
public class MarkItDownCommandResolver {

    private final AppIntegrationProperties props;
    private List<String> resolvedPrefix = List.of("markitdown");

    public MarkItDownCommandResolver(AppIntegrationProperties props) {
        this.props = props;
    }

    @PostConstruct
    void resolveAtStartup() {
        resolvedPrefix = resolve(props.getInvoices().getMarkitdownCommand());
        // #region agent log
        InvoiceDebugLog.write("A", "MarkItDownCommandResolver.resolveAtStartup",
                "resolved markitdown command",
                InvoiceDebugLog.data(
                        "configured", props.getInvoices().getMarkitdownCommand(),
                        "resolved", String.join(" ", resolvedPrefix)));
        // #endregion
    }

    public List<String> commandPrefix() {
        return resolvedPrefix;
    }

    static List<String> resolve(String configured) {
        List<List<String>> candidates = new ArrayList<>();
        boolean isDefault = configured == null || configured.isBlank() || "markitdown".equals(configured.trim());

        if (!isDefault) {
            candidates.add(parseCommand(configured));
        } else {
            Path venvBinary = Path.of(System.getProperty("user.dir"), ".venv-markitdown", "bin", "markitdown");
            if (Files.isExecutable(venvBinary)) {
                candidates.add(List.of(venvBinary.toAbsolutePath().toString()));
            }
            candidates.add(List.of("markitdown"));
            candidates.add(List.of("python3", "-m", "markitdown"));
        }

        Set<String> seen = new LinkedHashSet<>();
        for (List<String> candidate : candidates) {
            String key = String.join("\0", candidate);
            if (!seen.add(key)) {
                continue;
            }
            if (probeHelp(candidate)) {
                return candidate;
            }
        }
        return candidates.isEmpty() ? List.of("markitdown") : candidates.getFirst();
    }

    static boolean probeHelp(List<String> commandPrefix) {
        List<String> command = new ArrayList<>(commandPrefix);
        command.add("--help");
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private static List<String> parseCommand(String raw) {
        return List.of(raw.trim().split("\\s+"));
    }
}
