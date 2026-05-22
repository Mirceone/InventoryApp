package com.mirceone.inventoryapp.api.documents;

import com.mirceone.inventoryapp.service.documents.BatchUploadError;
import com.mirceone.inventoryapp.service.documents.BatchUploadItem;
import com.mirceone.inventoryapp.service.documents.BatchUploadResult;
import com.mirceone.inventoryapp.service.documents.DocumentSummary;
import com.mirceone.inventoryapp.service.documents.FolderStructureEntry;
import org.springframework.data.domain.Page;

import java.util.List;

public final class DocumentWebMapper {

    private DocumentWebMapper() {
    }

    public static DocumentResponse toResponse(DocumentSummary s) {
        return new DocumentResponse(
                s.id(),
                s.dossierId(),
                s.originalFilename(),
                s.mimeType(),
                s.sizeBytes(),
                s.createdAt(),
                s.uploadedByUserId(),
                s.uploadedByEmail(),
                s.folderPath(),
                s.processingStatus(),
                s.organizationSource()
        );
    }

    public static Page<DocumentResponse> toResponsePage(Page<DocumentSummary> page) {
        return page.map(DocumentWebMapper::toResponse);
    }

    public static BatchUploadResponse toBatchResponse(BatchUploadResult result) {
        List<BatchUploadResponse.BatchUploadItemResponse> accepted = result.accepted().stream()
                .map(DocumentWebMapper::toBatchItem)
                .toList();
        List<BatchUploadResponse.BatchUploadErrorResponse> errors = result.errors().stream()
                .map(DocumentWebMapper::toBatchError)
                .toList();
        return new BatchUploadResponse(accepted, errors);
    }

    private static BatchUploadResponse.BatchUploadItemResponse toBatchItem(BatchUploadItem item) {
        return new BatchUploadResponse.BatchUploadItemResponse(
                item.id(),
                item.originalFilename(),
                item.processingStatus()
        );
    }

    private static BatchUploadResponse.BatchUploadErrorResponse toBatchError(BatchUploadError error) {
        return new BatchUploadResponse.BatchUploadErrorResponse(error.originalFilename(), error.message());
    }

    public static FolderStructureResponse toFolderStructureResponse(FolderStructureEntry entry) {
        return new FolderStructureResponse(entry.path(), entry.documentCount());
    }
}
