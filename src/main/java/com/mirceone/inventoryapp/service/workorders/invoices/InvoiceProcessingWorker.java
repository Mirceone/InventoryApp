package com.mirceone.inventoryapp.service.workorders.invoices;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class InvoiceProcessingWorker {

    private final AppIntegrationProperties props;
    private final InvoiceProcessingService processingService;

    public InvoiceProcessingWorker(AppIntegrationProperties props, InvoiceProcessingService processingService) {
        this.props = props;
        this.processingService = processingService;
    }

    @Scheduled(fixedDelayString = "${app.invoices.processing-poll-interval:2s}")
    public void pollPendingInvoices() {
        int batchSize = Math.max(1, props.getInvoices().getProcessingBatchSize());
        processingService.processPendingBatch(batchSize);
    }
}
