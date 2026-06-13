package com.mirceone.inventoryapp.service.workorders.invoices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Logs whether the resolved MarkItDown command is usable at startup. Scanned PDFs and image
 * invoices are handled by the VLM (whose availability is validated by the AI startup checks).
 */
@Component
@ConditionalOnProperty(name = "app.invoices.extractor", havingValue = "markitdown", matchIfMissing = true)
public class InvoiceExtractorStartupValidator {

    private static final Logger log = LoggerFactory.getLogger(InvoiceExtractorStartupValidator.class);

    private final MarkItDownCommandResolver commandResolver;

    public InvoiceExtractorStartupValidator(MarkItDownCommandResolver commandResolver) {
        this.commandResolver = commandResolver;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateMarkItDownAvailable() {
        String resolved = String.join(" ", commandResolver.commandPrefix());
        boolean ok = MarkItDownCommandResolver.probeHelp(commandResolver.commandPrefix());

        if (!ok) {
            log.error("""
                    Invoice MarkItDown extractor is NOT available (resolved command: {}).
                    Digital (text-layer) PDFs will fail until this is fixed; scanned PDFs and images
                    still work via the VLM. From the project root run:
                      python3 -m venv .venv-markitdown
                      .venv-markitdown/bin/pip install 'markitdown[all]'
                    Or set APP_INVOICES_MARKITDOWN_COMMAND to a working binary path.
                    """, resolved);
        } else {
            log.info("Invoice MarkItDown extractor probe OK (command: {})", resolved);
        }
    }
}
