package com.mirceone.inventoryapp.service.workorders.invoices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Logs whether the resolved MarkItDown command is usable at startup.
 */
@Component
@ConditionalOnProperty(name = "app.invoices.extractor", havingValue = "markitdown", matchIfMissing = true)
public class InvoiceExtractorStartupValidator {

    private static final Logger log = LoggerFactory.getLogger(InvoiceExtractorStartupValidator.class);

    private final MarkItDownCommandResolver commandResolver;
    private final InvoiceOcrCommandResolver ocrCommandResolver;

    public InvoiceExtractorStartupValidator(
            MarkItDownCommandResolver commandResolver,
            InvoiceOcrCommandResolver ocrCommandResolver
    ) {
        this.commandResolver = commandResolver;
        this.ocrCommandResolver = ocrCommandResolver;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateMarkItDownAvailable() {
        String resolved = String.join(" ", commandResolver.commandPrefix());
        boolean ok = MarkItDownCommandResolver.probeHelp(commandResolver.commandPrefix());


        boolean ocrOk = InvoiceOcrCommandResolver.probeOcrDeps(
                ocrCommandResolver.pythonPrefix(), ocrCommandResolver.scriptPath());


        if (!ok) {
            log.error("""
                    Invoice MarkItDown extractor is NOT available (resolved command: {}).
                    Invoices will upload but processing will FAIL until this is fixed.
                    From the project root run:
                      python3 -m venv .venv-markitdown
                      .venv-markitdown/bin/pip install 'markitdown[all]'
                    Or set APP_INVOICES_MARKITDOWN_COMMAND to a working binary path.
                    """, resolved);
        } else {
            log.info("Invoice MarkItDown extractor probe OK (command: {})", resolved);
        }

        if (!ocrOk) {
            log.warn("""
                    Invoice OCR fallback is NOT available (python: {}, script: {}).
                    Scanned PDF/image invoices may fail with "empty output" after MarkItDown.
                    Install OCR deps in the project venv:
                      .venv-markitdown/bin/pip install pymupdf easyocr
                    """,
                    String.join(" ", ocrCommandResolver.pythonPrefix()),
                    ocrCommandResolver.scriptPath().toAbsolutePath());
        } else {
            log.info("Invoice OCR fallback probe OK (python: {}, script: {})",
                    String.join(" ", ocrCommandResolver.pythonPrefix()),
                    ocrCommandResolver.scriptPath().toAbsolutePath());
        }
    }
}
