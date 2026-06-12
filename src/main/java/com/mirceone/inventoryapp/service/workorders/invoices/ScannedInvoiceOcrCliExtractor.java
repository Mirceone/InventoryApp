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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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
            throw new IOException("Failed to start OCR command: " + String.join(" ", command), e);
        }

        // Drain the merged stdout+stderr stream concurrently so a large output cannot fill the OS
        // pipe buffer and deadlock the child process while we are blocked in waitFor.
        CompletableFuture<byte[]> outputFuture = readOutputAsync(process);

        Duration timeout = props.getInvoices().getOcrTimeout();
        boolean finished;
        try {
            finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            outputFuture.cancel(true);
            throw new IOException("OCR interrupted", e);
        }

        if (!finished) {
            process.destroyForcibly();
            outputFuture.cancel(true);
            throw new IOException("OCR timed out after " + timeout);
        }

        String output = readOutput(outputFuture).strip();

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            String message = output.isBlank() ? "exit code " + exitCode : output;
            throw new IOException("OCR failed: " + truncate(message, 500));
        }
        if (output.isBlank()) {
            throw new IOException("OCR produced empty output");
        }
        return output;
    }

    private static CompletableFuture<byte[]> readOutputAsync(Process process) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return process.getInputStream().readAllBytes();
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    private static String readOutput(CompletableFuture<byte[]> outputFuture) throws IOException {
        try {
            return new String(outputFuture.join(), StandardCharsets.UTF_8);
        } catch (CompletionException e) {
            throw new IOException("Failed to read OCR output", e.getCause());
        }
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "...";
    }
}
