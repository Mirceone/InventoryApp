package com.mirceone.inventoryapp.api.workorders;

import com.mirceone.inventoryapp.api.support.CurrentUserId;
import com.mirceone.inventoryapp.service.workorders.BatchUploadResult;
import com.mirceone.inventoryapp.service.workorders.FileSummary;
import com.mirceone.inventoryapp.service.workorders.WorkOrderFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/firms/{firmId}/work-orders/{workOrderId}/files")
@Tag(name = "Work order files", description = "Files classified into the work order folder tree by extension rules")
@SecurityRequirement(name = "bearerAuth")
public class FileController {

    private final WorkOrderFileService fileService;

    public FileController(WorkOrderFileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping
    @Operation(summary = "List files in work order (newest first), optional folder filter")
    public Page<FileResponse> listFiles(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId,
            @RequestParam(required = false) UUID folderId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<FileSummary> result = fileService.listFiles(
                userId, firmId, workOrderId, folderId, PageRequest.of(page, size));
        return FileWebMapper.toResponsePage(result);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a single file (classified synchronously into a folder)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created with folder assignment"),
            @ApiResponse(responseCode = "413", description = "File too large"),
            @ApiResponse(responseCode = "429", description = "Too many uploads")
    })
    public ResponseEntity<FileResponse> uploadFile(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId,
            @RequestPart("file") MultipartFile file
    ) {
        FileSummary created = fileService.upload(userId, firmId, workOrderId, file);
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/firms/{firmId}/work-orders/{workOrderId}/files/{fileId}")
                .buildAndExpand(firmId, workOrderId, created.id())
                .toUri();
        return ResponseEntity.created(location).body(FileWebMapper.toResponse(created));
    }

    @PostMapping(path = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload multiple files (each classified into a folder)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Per-file results"),
            @ApiResponse(responseCode = "413", description = "Batch too large")
    })
    public BatchUploadResponse uploadFilesBatch(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId,
            @RequestPart("files") MultipartFile[] files
    ) {
        BatchUploadResult result = fileService.uploadBatch(userId, firmId, workOrderId, files);
        return FileWebMapper.toBatchResponse(result);
    }

    @GetMapping("/{fileId}/content")
    @Operation(summary = "Download file bytes")
    public ResponseEntity<Resource> downloadContent(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId,
            @PathVariable UUID fileId
    ) {
        var download = fileService.openForDownload(userId, firmId, workOrderId, fileId);
        MediaType mediaType = MediaType.parseMediaType(download.mimeType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        WorkOrderFileService.contentDispositionAttachment(download.displayName()))
                .body(download.resource());
    }

    @PatchMapping("/{fileId}")
    @Operation(summary = "Rename a file and/or move it to another folder")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Empty body or invalid name"),
            @ApiResponse(responseCode = "409", description = "Name conflict in target folder")
    })
    public FileResponse updateFile(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId,
            @PathVariable UUID fileId,
            @Valid @RequestBody UpdateFileRequest request
    ) {
        return FileWebMapper.toResponse(fileService.updateFile(
                userId, firmId, workOrderId, fileId, FileWebMapper.toUpdateFileSpec(request)));
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "Delete a file")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFile(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId,
            @PathVariable UUID fileId
    ) {
        fileService.deleteFile(userId, firmId, workOrderId, fileId);
    }
}
