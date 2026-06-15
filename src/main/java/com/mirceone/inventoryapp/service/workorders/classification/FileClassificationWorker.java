package com.mirceone.inventoryapp.service.workorders.classification;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class FileClassificationWorker {

    private final AppIntegrationProperties props;
    private final FileClassificationService classificationService;

    public FileClassificationWorker(
            AppIntegrationProperties props,
            FileClassificationService classificationService
    ) {
        this.props = props;
        this.classificationService = classificationService;
    }

    @Scheduled(fixedDelayString = "${app.files.classification-poll-interval:2s}")
    public void pollPendingFiles() {
        if (!props.getFeatures().isWorkOrderAiEnabled()) {
            return;
        }
        int batchSize = Math.max(1, props.getFiles().getClassificationBatchSize());
        classificationService.processPendingBatch(batchSize);
    }
}
