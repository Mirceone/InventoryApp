package com.mirceone.inventoryapp.service.workorders;

import com.mirceone.inventoryapp.config.AppIntegrationProperties;
import com.mirceone.inventoryapp.model.FirmWorkOrderEntity;
import com.mirceone.inventoryapp.model.WorkOrderFolderEntity;
import com.mirceone.inventoryapp.model.WorkOrderStatus;
import com.mirceone.inventoryapp.repository.FirmWorkOrderRepository;
import com.mirceone.inventoryapp.repository.WorkOrderFileRepository;
import com.mirceone.inventoryapp.repository.WorkOrderFolderRepository;
import com.mirceone.inventoryapp.repository.WorkOrderFolderRuleRepository;
import com.mirceone.inventoryapp.repository.WorkOrderInvoiceRepository;
import com.mirceone.inventoryapp.service.firms.access.FirmAccessService;
import com.mirceone.inventoryapp.service.storage.BlobStorage;
import com.mirceone.inventoryapp.service.support.AfterCommitExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @Mock
    private AppIntegrationProperties props;
    @Mock
    private AppIntegrationProperties.Features features;
    @Mock
    private FirmAccessService firmAccessService;
    @Mock
    private FirmWorkOrderRepository firmWorkOrderRepository;
    @Mock
    private WorkOrderFolderRepository folderRepository;
    @Mock
    private WorkOrderFolderRuleRepository ruleRepository;
    @Mock
    private WorkOrderFileRepository fileRepository;
    @Mock
    private WorkOrderInvoiceRepository invoiceRepository;
    @Mock
    private BlobStorage blobStorage;

    private WorkOrderService workOrderService;

    private UUID userId;
    private UUID firmId;
    private UUID workOrderId;

    @BeforeEach
    void setUp() {
        workOrderService = new WorkOrderService(
                props,
                firmAccessService,
                firmWorkOrderRepository,
                folderRepository,
                ruleRepository,
                fileRepository,
                invoiceRepository,
                new DefaultFolderTemplate(),
                blobStorage,
                new AfterCommitExecutor()
        );
        userId = UUID.randomUUID();
        firmId = UUID.randomUUID();
        workOrderId = UUID.randomUUID();
        when(props.getFeatures()).thenReturn(features);
        when(features.isWorkOrderEnabled()).thenReturn(true);
    }

    @Test
    void createWorkOrderPersistsMetadataAndSeedsDefaultFolders() {
        LocalDate estimatedEndDate = LocalDate.now(ZoneOffset.UTC).plusDays(30);
        when(firmWorkOrderRepository.existsByFirmIdAndNameIgnoreCase(firmId, "Project A")).thenReturn(false);
        when(firmWorkOrderRepository.save(any(FirmWorkOrderEntity.class))).thenAnswer(invocation -> {
            FirmWorkOrderEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", workOrderId);
            ReflectionTestUtils.setField(entity, "createdAt", Instant.parse("2026-06-08T10:00:00Z"));
            return entity;
        });

        WorkOrderSummary created = workOrderService.createWorkOrder(
                userId,
                firmId,
                new WorkOrderContracts.CreateWorkOrderSpec(
                        "Project A",
                        "Client SRL",
                        "Bucharest",
                        "  Kitchen fit-out  ",
                        estimatedEndDate
                )
        );

        ArgumentCaptor<FirmWorkOrderEntity> captor = ArgumentCaptor.forClass(FirmWorkOrderEntity.class);
        verify(firmWorkOrderRepository).save(captor.capture());
        FirmWorkOrderEntity saved = captor.getValue();
        assertEquals("Project A", saved.getName());
        assertEquals("Client SRL", saved.getClientName());
        assertEquals("Bucharest", saved.getLocation());
        assertEquals("Kitchen fit-out", saved.getDescription());
        assertEquals(estimatedEndDate, saved.getEstimatedEndDate());
        assertEquals(WorkOrderStatus.PLANNED, saved.getStatus());
        assertEquals("Project A", created.name());
        assertEquals(0, created.fileCount());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<WorkOrderFolderEntity>> foldersCaptor = ArgumentCaptor.forClass(List.class);
        verify(folderRepository).saveAll(foldersCaptor.capture());
        List<WorkOrderFolderEntity> seeded = foldersCaptor.getValue();
        assertEquals(2, seeded.size());
        assertEquals(1, seeded.stream().filter(WorkOrderFolderEntity::isCatchAll).count());
        assertTrue(seeded.stream().allMatch(f -> f.getWorkOrderId().equals(workOrderId)));
    }

    @Test
    void createWorkOrderRejectsEstimatedEndDateBeforeStart() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> workOrderService.createWorkOrder(
                        userId,
                        firmId,
                        new WorkOrderContracts.CreateWorkOrderSpec(
                                "Project A",
                                "Client SRL",
                                "Bucharest",
                                null,
                                LocalDate.now(ZoneOffset.UTC).minusDays(1)
                        )
                )
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateWorkOrderRejectsEmptyBody() {
        FirmWorkOrderEntity workOrder = existingWorkOrder();
        when(firmWorkOrderRepository.findByIdAndFirmId(workOrderId, firmId)).thenReturn(Optional.of(workOrder));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> workOrderService.updateWorkOrder(
                        userId,
                        firmId,
                        workOrderId,
                        new WorkOrderContracts.UpdateWorkOrderSpec(null, null, null, null, null, false)
                )
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateWorkOrderClearsDescriptionWhenExplicitNull() {
        FirmWorkOrderEntity workOrder = existingWorkOrder();
        workOrder.setDescription("Old notes");
        when(firmWorkOrderRepository.findByIdAndFirmId(workOrderId, firmId)).thenReturn(Optional.of(workOrder));
        when(firmWorkOrderRepository.save(workOrder)).thenReturn(workOrder);
        when(fileRepository.countByWorkOrderId(workOrderId)).thenReturn(2L);

        WorkOrderSummary updated = workOrderService.updateWorkOrder(
                userId,
                firmId,
                workOrderId,
                new WorkOrderContracts.UpdateWorkOrderSpec(null, null, null, null, null, true)
        );

        assertNull(updated.description());
        assertNull(workOrder.getDescription());
    }

    @Test
    void updateWorkOrderRejectsDuplicateName() {
        FirmWorkOrderEntity workOrder = existingWorkOrder();
        when(firmWorkOrderRepository.findByIdAndFirmId(workOrderId, firmId)).thenReturn(Optional.of(workOrder));
        when(firmWorkOrderRepository.existsByFirmIdAndNameIgnoreCaseAndIdNot(firmId, "Taken", workOrderId)).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> workOrderService.updateWorkOrder(
                        userId,
                        firmId,
                        workOrderId,
                        new WorkOrderContracts.UpdateWorkOrderSpec("Taken", null, null, null, null, false)
                )
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void updateWorkOrderStatusPersistsStatus() {
        FirmWorkOrderEntity workOrder = existingWorkOrder();
        when(firmWorkOrderRepository.findByIdAndFirmId(workOrderId, firmId)).thenReturn(Optional.of(workOrder));
        when(firmWorkOrderRepository.save(workOrder)).thenReturn(workOrder);
        when(fileRepository.countByWorkOrderId(workOrderId)).thenReturn(1L);

        WorkOrderSummary updated = workOrderService.updateWorkOrderStatus(
                userId,
                firmId,
                workOrderId,
                WorkOrderStatus.COMPLETED
        );

        assertEquals(WorkOrderStatus.COMPLETED, workOrder.getStatus());
        assertEquals(WorkOrderStatus.COMPLETED, updated.status());
    }

    @Test
    void deleteWorkOrderRemovesRowsAndBlobPrefix() throws Exception {
        FirmWorkOrderEntity workOrder = existingWorkOrder();
        when(firmWorkOrderRepository.findByIdAndFirmId(workOrderId, firmId)).thenReturn(Optional.of(workOrder));

        workOrderService.deleteWorkOrder(userId, firmId, workOrderId);

        verify(fileRepository).deleteByWorkOrderId(workOrderId);
        verify(invoiceRepository).deleteByWorkOrderId(workOrderId);
        verify(firmWorkOrderRepository).deleteById(workOrderId);
        verify(blobStorage).deleteByPrefix(firmId + "/" + workOrderId + "/");
    }

    private FirmWorkOrderEntity existingWorkOrder() {
        FirmWorkOrderEntity workOrder = new FirmWorkOrderEntity(
                firmId,
                "Project A",
                "Client SRL",
                "Bucharest",
                "Notes",
                LocalDate.now(ZoneOffset.UTC).plusDays(10),
                userId
        );
        ReflectionTestUtils.setField(workOrder, "id", workOrderId);
        ReflectionTestUtils.setField(workOrder, "createdAt", Instant.parse("2026-06-08T10:00:00Z"));
        return workOrder;
    }
}
