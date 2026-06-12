package com.mirceone.inventoryapp.service.workorders.invoices;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "app.invoices.extractor", havingValue = "markitdown", matchIfMissing = true)
public class MarkItDownCliExtractor implements InvoiceMarkdownExtractor {

    private final AppIntegrationProperties props;
    private final MarkItDownCommandResolver commandResolver;

    public MarkItDownCliExtractor(AppIntegrationProperties props, MarkItDownCommandResolver commandResolver) {
        this.props = props;
        this.commandResolver = commandResolver;
    }

    @Override
    public String extract(Path sourceFile, String mimeType) throws IOException {
        List<String> commandParts = commandResolver.commandPrefix();
        List<String> command = new ArrayList<>(commandParts);
        command.add(sourceFile.toAbsolutePath().toString());

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            // #region agent log
            InvoiceDebugLog.write("A", "MarkItDownCliExtractor.extract",
                    "failed to start markitdown process",
                    InvoiceDebugLog.data(
                            "command", String.join(" ", command),
                            "error", e.getMessage()));
            // #endregion
            throw new IOException(
                    "Failed to start MarkItDown command: " + String.join(" ", commandParts)
                            + ". Install with: pip install 'markitdown[all]'"
                            + " or create .venv-markitdown in the project root.",
                    e);
        }

        Duration timeout = props.getInvoices().getMarkitdownTimeout();
        boolean finished;
        try {
            finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("MarkItDown interrupted", e);
        }

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("MarkItDown timed out after " + timeout);
        }

        int exitCode = process.exitValue();
        // #region agent log
        InvoiceDebugLog.write("A", "MarkItDownCliExtractor.extract",
                "markitdown process finished",
                InvoiceDebugLog.data(
                        "command", String.join(" ", command),
                        "exitCode", exitCode,
                        "outputLength", output.length(),
                        "finished", finished));
        // #endregion
        if (exitCode != 0) {
            String message = output.isBlank() ? "exit code " + exitCode : output;
            throw new IOException("MarkItDown failed: " + truncate(message, 500));
        }

        if (output.isBlank()) {
            throw new IOException("MarkItDown produced empty output");
        }

        return output;
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "...";
    }
}
