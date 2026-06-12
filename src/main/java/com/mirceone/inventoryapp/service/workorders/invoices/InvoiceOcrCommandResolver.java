package com.mirceone.inventoryapp.service.workorders.invoices;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "app.invoices.extractor", havingValue = "markitdown", matchIfMissing = true)
public class InvoiceOcrCommandResolver {

    private final AppIntegrationProperties props;
    private List<String> pythonPrefix = List.of("python3");
    private Path scriptPath = Path.of("scripts", "invoice_pdf_ocr.py");

    public InvoiceOcrCommandResolver(AppIntegrationProperties props) {
        this.props = props;
    }

    @PostConstruct
    void resolveAtStartup() {
        pythonPrefix = resolvePython(props.getInvoices().getOcrPythonCommand());
        scriptPath = resolveScript(props.getInvoices().getOcrScriptPath());
    }

    public List<String> pythonPrefix() {
        return pythonPrefix;
    }

    public Path scriptPath() {
        return scriptPath;
    }

    public boolean isAvailable() {
        return Files.isExecutable(Path.of(pythonPrefix.getFirst()))
                && Files.isRegularFile(scriptPath);
    }

    static List<String> resolvePython(String configured) {
        if (configured != null && !configured.isBlank() && !"python3".equals(configured.trim())) {
            return List.of(configured.trim());
        }
        Path venvPython = Path.of(System.getProperty("user.dir"), ".venv-markitdown", "bin", "python3");
        if (Files.isExecutable(venvPython)) {
            return List.of(venvPython.toAbsolutePath().toString());
        }
        return List.of("python3");
    }

    static Path resolveScript(String configured) {
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured.trim());
        }
        return Path.of(System.getProperty("user.dir"), "scripts", "invoice_pdf_ocr.py");
    }

    static boolean probeOcrDeps(List<String> pythonPrefix, Path scriptPath) {
        if (!Files.isExecutable(Path.of(pythonPrefix.getFirst())) || !Files.isRegularFile(scriptPath)) {
            return false;
        }
        List<String> command = new ArrayList<>(pythonPrefix);
        command.add("-c");
        command.add("import easyocr, fitz");
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }
}
