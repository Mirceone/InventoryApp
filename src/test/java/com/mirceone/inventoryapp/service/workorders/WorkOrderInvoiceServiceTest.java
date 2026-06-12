package com.mirceone.inventoryapp.service.workorders;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import com.mirceone.inventoryapp.model.FirmWorkOrderEntity;
import com.mirceone.inventoryapp.model.InvoiceProcessingStatus;
import com.mirceone.inventoryapp.model.WorkOrderInvoiceEntity;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.repository.WorkOrderInvoiceRepository;
import com.mirceone.inventoryapp.service.firms.access.FirmAccessService;
import com.mirceone.inventoryapp.service.storage.BlobStorage;
import com.mirceone.inventoryapp.service.support.AfterCommitExecutor;
import com.mirceone.inventoryapp.service.workorders.invoices.InvoiceProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkOrderInvoiceServiceTest {

    @Mock
    private AppIntegrationProperties props;
    @Mock
    private AppIntegrationProperties.Features features;
    @Mock
    private AppIntegrationProperties.Invoices invoices;
    @Mock
    private AppIntegrationProperties.Storage storage;
    @Mock
    private FirmAccessService firmAccessService;
    @Mock
    private WorkOrderService workOrderService;
    @Mock
    private WorkOrderInvoiceRepository invoiceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BlobStorage blobStorage;
    @Mock
    private InvoiceProcessingService processingService;

    private WorkOrderInvoiceService invoiceService;

    private final UUID userId = UUID.randomUUID();
    private final UUID firmId = UUID.randomUUID();
    private final UUID workOrderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        invoiceService = new WorkOrderInvoiceService(
                props,
                firmAccessService,
                workOrderService,
                invoiceRepository,
                userRepository,
                blobStorage,
                new AfterCommitExecutor(),
                processingService
        );
        when(props.getFeatures()).thenReturn(features);
        when(features.isWorkOrderEnabled()).thenReturn(true);
        when(props.getStorage()).thenReturn(storage);
        when(storage.getMaxFileSizeBytes()).thenReturn(1024L * 1024);
        when(props.getInvoices()).thenReturn(invoices);
        when(invoices.getAllowedMimePrefixes()).thenReturn(List.of("application/pdf", "image/"));
        when(invoices.getBatchMaxFiles()).thenReturn(10);
        when(invoices.getBatchMaxTotalBytes()).thenReturn(10_000_000L);
        when(workOrderService.requireWorkOrder(firmId, workOrderId)).thenReturn(existingWorkOrder());
    }

    @Test
    void uploadRejectsDisallowedMimeType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "invoice.txt", "text/plain", "hello".getBytes());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> invoiceService.upload(userId, firmId, workOrderId, file)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void uploadStoresBlobAndCreatesPendingInvoice() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "invoice.pdf", "application/pdf", new byte[]{1, 2, 3});
        when(blobStorage.store(anyString(), any(), eq(3L))).thenReturn(3L);
        when(invoiceRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAllById(any())).thenReturn(List.of());

        InvoiceSummary summary = invoiceService.upload(userId, firmId, workOrderId, file);

        assertEquals("invoice.pdf", summary.displayName());
        assertEquals(InvoiceProcessingStatus.PENDING, summary.processingStatus());
        verify(blobStorage).store(contains("/invoices/"), any(), eq(3L));
        verify(processingService).processAsync(any());
    }

    @Test
    void retryProcessingOnlyAllowedForFailed() {
        UUID invoiceId = UUID.randomUUID();
        WorkOrderInvoiceEntity ready = sampleInvoice(invoiceId, InvoiceProcessingStatus.READY);
        when(invoiceRepository.findByIdAndFirmIdAndWorkOrderId(invoiceId, firmId, workOrderId))
                .thenReturn(Optional.of(ready));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> invoiceService.retryProcessing(userId, firmId, workOrderId, invoiceId)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void retryProcessingResetsFailedInvoiceToPending() {
        UUID invoiceId = UUID.randomUUID();
        WorkOrderInvoiceEntity failed = sampleInvoice(invoiceId, InvoiceProcessingStatus.FAILED);
        failed.setProcessingError("boom");
        when(invoiceRepository.findByIdAndFirmIdAndWorkOrderId(invoiceId, firmId, workOrderId))
                .thenReturn(Optional.of(failed));
        when(invoiceRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAllById(any())).thenReturn(List.of());

        InvoiceSummary summary = invoiceService.retryProcessing(userId, firmId, workOrderId, invoiceId);

        assertEquals(InvoiceProcessingStatus.PENDING, summary.processingStatus());
        ArgumentCaptor<WorkOrderInvoiceEntity> captor = ArgumentCaptor.forClass(WorkOrderInvoiceEntity.class);
        verify(invoiceRepository).saveAndFlush(captor.capture());
        assertNull(captor.getValue().getProcessingError());
        verify(processingService).processAsync(invoiceId);
    }

    private FirmWorkOrderEntity existingWorkOrder() {
        FirmWorkOrderEntity workOrder = new FirmWorkOrderEntity(
                firmId,
                "Project",
                "Client",
                "City",
                null,
                LocalDate.now(ZoneOffset.UTC).plusDays(7),
                userId
        );
        org.springframework.test.util.ReflectionTestUtils.setField(workOrder, "id", workOrderId);
        return workOrder;
    }

    private WorkOrderInvoiceEntity sampleInvoice(UUID invoiceId, InvoiceProcessingStatus status) {
        WorkOrderInvoiceEntity invoice = new WorkOrderInvoiceEntity(
                invoiceId,
                firmId,
                workOrderId,
                userId,
                "invoice.pdf",
                "pdf",
                "application/pdf",
                10,
                "checksum",
                firmId + "/" + workOrderId + "/invoices/" + invoiceId + ".pdf"
        );
        invoice.setProcessingStatus(status);
        return invoice;
    }
}
