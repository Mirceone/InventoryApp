package com.mirceone.inventoryapp.api.workorders;

import com.mirceone.inventoryapp.service.workorders.BatchUploadResult;
import com.mirceone.inventoryapp.service.workorders.FileSummary;
import com.mirceone.inventoryapp.service.workorders.WorkOrderContracts;
import org.springframework.data.domain.Page;

import java.util.List;

public final class FileWebMapper {

    private FileWebMapper() {
    }

    public static FileResponse toResponse(FileSummary s) {
        return new FileResponse(
                s.id(),
                s.workOrderId(),
                s.folderId(),
                s.folderPath(),
                s.displayName(),
                s.extension(),
                s.mimeType(),
                s.sizeBytes(),
                s.createdAt(),
                s.uploadedByUserId(),
                s.uploadedByEmail()
        );
    }

    public static Page<FileResponse> toResponsePage(Page<FileSummary> page) {
        return page.map(FileWebMapper::toResponse);
    }

    public static WorkOrderContracts.UpdateFileSpec toUpdateFileSpec(UpdateFileRequest request) {
        return new WorkOrderContracts.UpdateFileSpec(request.displayName(), request.folderId());
    }

    public static BatchUploadResponse toBatchResponse(BatchUploadResult result) {
        List<BatchUploadResponse.BatchUploadItemResponse> accepted = result.accepted().stream()
                .map(item -> new BatchUploadResponse.BatchUploadItemResponse(
                        item.id(), item.displayName(), item.folderId(), item.folderPath()))
                .toList();
        List<BatchUploadResponse.BatchUploadErrorResponse> errors = result.errors().stream()
                .map(error -> new BatchUploadResponse.BatchUploadErrorResponse(
                        error.originalFilename(), error.message()))
                .toList();
        return new BatchUploadResponse(accepted, errors);
    }
}
