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
public class ScannedInvoiceOcrCliExtractor implements InvoiceMarkdownExtractor {

    private final AppIntegrationProperties props;
    private final InvoiceOcrCommandResolver commandResolver;

    public ScannedInvoiceOcrCliExtractor(
            AppIntegrationProperties props,
            InvoiceOcrCommandResolver commandResolver
    ) {
        this.props = props;
        this.commandResolver = commandResolver;
    }

    @Override
    public String extract(Path sourceFile, String mimeType) throws IOException {
        if (!commandResolver.isAvailable()) {
            throw new IOException("OCR fallback is not available (missing python script or interpreter)");
        }

        List<String> command = new ArrayList<>(commandResolver.pythonPrefix());
        command.add(commandResolver.scriptPath().toAbsolutePath().toString());
        command.add(sourceFile.toAbsolutePath().toString());
        command.add("--languages");
        command.add(props.getInvoices().getOcrLanguages());

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            // #region agent log
            InvoiceDebugLog.write("F", "ScannedInvoiceOcrCliExtractor.extract",
                    "failed to start OCR process",
                    InvoiceDebugLog.data("command", String.join(" ", command), "error", e.getMessage()));
            // #endregion
            throw new IOException("Failed to start OCR command: " + String.join(" ", command), e);
        }

        Duration timeout = props.getInvoices().getOcrTimeout();
        boolean finished;
        try {
            finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("OCR interrupted", e);
        }

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("OCR timed out after " + timeout);
        }

        int exitCode = process.exitValue();
        // #region agent log
        InvoiceDebugLog.write("F", "ScannedInvoiceOcrCliExtractor.extract",
                "OCR process finished",
                InvoiceDebugLog.data(
                        "command", String.join(" ", command),
                        "exitCode", exitCode,
                        "outputLength", output.length(),
                        "finished", finished));
        // #endregion

        if (exitCode != 0) {
            String message = output.isBlank() ? "exit code " + exitCode : output;
            throw new IOException("OCR failed: " + truncate(message, 500));
        }
        if (output.isBlank()) {
            throw new IOException("OCR produced empty output");
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
