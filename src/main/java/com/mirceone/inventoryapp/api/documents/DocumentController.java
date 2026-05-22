package com.mirceone.inventoryapp.api.documents;

import com.mirceone.inventoryapp.service.documents.BatchUploadResult;
import com.mirceone.inventoryapp.service.documents.DocumentService;
import com.mirceone.inventoryapp.service.documents.DocumentSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/firms/{firmId}/dossiers/{dossierId}/documents")
@Tag(name = "Electronic folder documents", description = "Files inside a user-named dossier (smart organization in background)")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    @Operation(summary = "List documents in dossier (newest first), optional AI folder filter")
    public Page<DocumentResponse> listDocuments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId,
            @PathVariable UUID dossierId,
            @RequestParam(required = false) String folder,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Page<DocumentSummary> result = documentService.listDocuments(
                userId, firmId, dossierId, folder, PageRequest.of(page, size));
        return DocumentWebMapper.toResponsePage(result);
    }

    @GetMapping("/folders")
    @Operation(summary = "List folder paths that contain classified documents in this dossier")
    public List<String> listFolders(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId,
            @PathVariable UUID dossierId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return documentService.listFolders(userId, firmId, dossierId);
    }

    @GetMapping("/folder-structure")
    @Operation(summary = "Predefined folder taxonomy with document counts per path")
    public List<FolderStructureResponse> listFolderStructure(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId,
            @PathVariable UUID dossierId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return documentService.listFolderStructure(userId, firmId, dossierId).stream()
                .map(DocumentWebMapper::toFolderStructureResponse)
                .toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a single file into dossier (queued for organization)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "413", description = "File too large"),
            @ApiResponse(responseCode = "429", description = "Too many uploads")
    })
    public ResponseEntity<DocumentResponse> uploadDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId,
            @PathVariable UUID dossierId,
            @RequestPart("file") MultipartFile file
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        DocumentSummary created = documentService.upload(userId, firmId, dossierId, file);
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/firms/{firmId}/dossiers/{dossierId}/documents/{documentId}")
                .buildAndExpand(firmId, dossierId, created.id())
                .toUri();
        return ResponseEntity.created(location).body(DocumentWebMapper.toResponse(created));
    }

    @PostMapping(path = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload multiple files into dossier (async organization)")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Accepted"),
            @ApiResponse(responseCode = "413", description = "Batch too large")
    })
    public ResponseEntity<BatchUploadResponse> uploadDocumentsBatch(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId,
            @PathVariable UUID dossierId,
            @RequestPart("files") MultipartFile[] files
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        BatchUploadResult result = documentService.uploadBatch(userId, firmId, dossierId, files);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(DocumentWebMapper.toBatchResponse(result));
    }

    @GetMapping("/{documentId}/content")
    @Operation(summary = "Download document bytes")
    @ApiResponses({
            @ApiResponse(responseCode = "409", description = "Still organizing or failed")
    })
    public ResponseEntity<Resource> downloadContent(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId,
            @PathVariable UUID dossierId,
            @PathVariable UUID documentId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var download = documentService.openForDownload(userId, firmId, dossierId, documentId);
        MediaType mediaType = MediaType.parseMediaType(download.mimeType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, DocumentService.contentDispositionAttachment(download.originalFilename()))
                .body(download.resource());
    }

    @DeleteMapping("/{documentId}")
    @Operation(summary = "Delete a document")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID firmId,
            @PathVariable UUID dossierId,
            @PathVariable UUID documentId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        documentService.deleteDocument(userId, firmId, dossierId, documentId);
    }
}
