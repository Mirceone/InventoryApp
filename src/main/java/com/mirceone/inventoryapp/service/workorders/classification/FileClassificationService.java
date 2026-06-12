package com.mirceone.inventoryapp.service.workorders.classification;

import com.mirceone.inventoryapp.model.FileClassificationSource;
import com.mirceone.inventoryapp.model.FileClassificationStatus;
import com.mirceone.inventoryapp.model.WorkOrderFileEntity;
import com.mirceone.inventoryapp.repository.WorkOrderFileRepository;
import com.mirceone.inventoryapp.service.workorders.DisplayNameDeduplicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileClassificationService {

    private static final Logger log = LoggerFactory.getLogger(FileClassificationService.class);

    private final WorkOrderFileRepository fileRepository;
    private final MlxFolderClassifier mlxFolderClassifier;
    private final FileNameHeuristicClassifier heuristicClassifier;
    private final DisplayNameDeduplicator displayNameDeduplicator;

    public FileClassificationService(
            WorkOrderFileRepository fileRepository,
            MlxFolderClassifier mlxFolderClassifier,
            FileNameHeuristicClassifier heuristicClassifier,
            DisplayNameDeduplicator displayNameDeduplicator
    ) {
        this.fileRepository = fileRepository;
        this.mlxFolderClassifier = mlxFolderClassifier;
        this.heuristicClassifier = heuristicClassifier;
        this.displayNameDeduplicator = displayNameDeduplicator;
    }

    @Async
    public void processAsync(UUID fileId) {
        try {
            processFile(fileId);
        } catch (Exception e) {
            log.warn("Async file classification failed id={}: {}", fileId, e.getMessage());
        }
    }

    @Transactional
    public int processPendingBatch(int batchSize) {
        List<WorkOrderFileEntity> batch = fileRepository.lockNextPendingBatch(batchSize);
        int processed = 0;
        for (WorkOrderFileEntity file : batch) {
            try {
                processLockedFile(file);
                processed++;
            } catch (Exception e) {
                log.warn("File classification failed id={}: {}", file.getId(), e.getMessage());
            }
        }
        return processed;
    }

    @Transactional
    public void processFile(UUID fileId) {
        WorkOrderFileEntity file = fileRepository.lockPendingById(fileId).orElse(null);
        if (file == null) {
            return;
        }
        processLockedFile(file);
    }

    private void processLockedFile(WorkOrderFileEntity file) {
        Optional<String> hint = heuristicClassifier.hint(file.getDisplayName(), file.getMimeType());
        String ruleSuggestion = hint.orElse(null);
        try {
            UUID folderId = mlxFolderClassifier.classify(
                    file.getWorkOrderId(),
                    file.getDisplayName(),
                    file.getMimeType(),
                    ruleSuggestion
            );
            if (!folderId.equals(file.getFolderId())) {
                file.setFolderId(folderId);
                file.setDisplayName(displayNameDeduplicator.uniqueName(folderId, file.getDisplayName()));
            }
            file.setClassificationStatus(FileClassificationStatus.CLASSIFIED);
            file.setClassificationSource(FileClassificationSource.AI);
            file.setClassificationError(null);
            fileRepository.save(file);
        } catch (Exception e) {
            // Persist the failure in the current transaction without rethrowing: rethrowing
            // would propagate through the @Transactional boundary and roll back this very write.
            log.warn("File classification failed id={}: {}", file.getId(), e.getMessage());
            file.setClassificationStatus(FileClassificationStatus.FAILED);
            file.setClassificationError(truncate(e.getMessage(), 2000));
            fileRepository.save(file);
        }
    }

    private static String truncate(String message, int max) {
        if (message == null) {
            return null;
        }
        return message.length() <= max ? message : message.substring(0, max);
    }
}
