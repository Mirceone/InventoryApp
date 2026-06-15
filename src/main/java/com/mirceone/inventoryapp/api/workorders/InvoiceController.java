package com.mirceone.inventoryapp.api.workorders;

import com.mirceone.inventoryapp.api.support.CurrentUserId;
import com.mirceone.inventoryapp.service.workorders.InvoiceSummary;
import com.mirceone.inventoryapp.service.workorders.WorkOrderFileService;
import com.mirceone.inventoryapp.service.workorders.WorkOrderInvoiceService;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/firms/{firmId}/work-orders/{workOrderId}/invoices")
@Tag(name = "Work order invoices", description = "Invoice uploads with async MarkItDown extraction to markdown")
@SecurityRequirement(name = "bearerAuth")
public class InvoiceController {

    private final WorkOrderInvoiceService invoiceService;

    public InvoiceController(WorkOrderInvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    @Operation(summary = "List invoices in work order (newest first)")
    public Page<InvoiceResponse> listInvoices(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<InvoiceSummary> result = invoiceService.listInvoices(
                userId, firmId, workOrderId, PageRequest.of(page, size));
        return InvoiceWebMapper.toResponsePage(result);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a single invoice (processed asynchronously)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created with PENDING status"),
            @ApiResponse(responseCode = "413", description = "File too large"),
            @ApiResponse(responseCode = "429", description = "Too many uploads")
    })
    public ResponseEntity<InvoiceResponse> uploadInvoice(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId,
            @RequestPart("file") MultipartFile file
    ) {
        InvoiceSummary created = invoiceService.upload(userId, firmId, workOrderId, file);
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/firms/{firmId}/work-orders/{workOrderId}/invoices/{invoiceId}")
                .buildAndExpand(firmId, workOrderId, created.id())
                .toUri();
        return ResponseEntity.created(location).body(InvoiceWebMapper.toResponse(created));
    }

    @PostMapping(path = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload multiple invoices")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Per-file results"),
            @ApiResponse(responseCode = "413", description = "Batch too large")
    })
    public BatchInvoiceUploadResponse uploadInvoicesBatch(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId,
            @RequestPart("files") MultipartFile[] files
    ) {
        return InvoiceWebMapper.toBatchResponse(invoiceService.uploadBatch(userId, firmId, workOrderId, files));
    }

    @GetMapping("/{invoiceId}")
    @Operation(summary = "Get invoice details including markdown when READY")
    public InvoiceResponse getInvoice(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId,
            @PathVariable UUID invoiceId
    ) {
        return InvoiceWebMapper.toResponse(invoiceService.getInvoice(userId, firmId, workOrderId, invoiceId));
    }

    @GetMapping("/{invoiceId}/extraction")
    @Operation(summary = "Get the structured line items extracted from an invoice")
    public InvoiceExtractionResponse getExtraction(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId,
            @PathVariable UUID invoiceId
    ) {
        return InvoiceWebMapper.toExtractionResponse(
                invoiceService.getExtraction(userId, firmId, workOrderId, invoiceId));
    }

    @GetMapping("/{invoiceId}/content")
    @Operation(summary = "Download original invoice file")
    public ResponseEntity<Resource> downloadContent(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId,
            @PathVariable UUID invoiceId
    ) {
        var download = invoiceService.openForDownload(userId, firmId, workOrderId, invoiceId);
        MediaType mediaType = MediaType.parseMediaType(download.mimeType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        WorkOrderFileService.contentDispositionAttachment(download.displayName()))
                .body(download.resource());
    }

    @PostMapping("/{invoiceId}/retry")
    @Operation(summary = "Retry MarkItDown processing for a failed invoice")
    public InvoiceResponse retryProcessing(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId,
            @PathVariable UUID invoiceId
    ) {
        return InvoiceWebMapper.toResponse(invoiceService.retryProcessing(userId, firmId, workOrderId, invoiceId));
    }

    @DeleteMapping("/{invoiceId}")
    @Operation(summary = "Delete an invoice")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInvoice(
            @CurrentUserId UUID userId,
            @PathVariable UUID firmId,
            @PathVariable UUID workOrderId,
            @PathVariable UUID invoiceId
    ) {
        invoiceService.deleteInvoice(userId, firmId, workOrderId, invoiceId);
    }
}
