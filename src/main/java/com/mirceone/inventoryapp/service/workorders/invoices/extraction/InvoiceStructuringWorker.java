package com.mirceone.inventoryapp.service.workorders.invoices.extraction;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class InvoiceStructuringWorker {

    private final AppIntegrationProperties props;
    private final InvoiceStructuringService structuringService;

    public InvoiceStructuringWorker(
            AppIntegrationProperties props,
            InvoiceStructuringService structuringService
    ) {
        this.props = props;
        this.structuringService = structuringService;
    }

    @Scheduled(fixedDelayString = "${app.invoices.structuring-poll-interval:3s}")
    public void pollPendingExtractions() {
        if (!props.getFeatures().isInvoiceExtractionEnabled()) {
            return;
        }
        int batchSize = Math.max(1, props.getInvoices().getStructuringBatchSize());
        structuringService.processPendingBatch(batchSize);
    }
}
