package com.mirceone.inventoryapp.service.firms;

import com.mirceone.inventoryapp.model.FirmEntity;
import com.mirceone.inventoryapp.model.FirmMemberEntity;
import com.mirceone.inventoryapp.model.FirmStatus;
import com.mirceone.inventoryapp.model.MemberRole;
import com.mirceone.inventoryapp.repository.FirmMemberRepository;
import com.mirceone.inventoryapp.repository.FirmRepository;
import com.mirceone.inventoryapp.repository.FirmStatusHistoryRepository;
import com.mirceone.inventoryapp.repository.WorkOrderActivityRepository;
import com.mirceone.inventoryapp.repository.WorkOrderFileRepository;
import com.mirceone.inventoryapp.repository.WorkOrderInvoiceRepository;
import com.mirceone.inventoryapp.service.firms.access.FirmAccessService;
import com.mirceone.inventoryapp.service.storage.BlobStorage;
import com.mirceone.inventoryapp.service.inventory.CategoryService;
import com.mirceone.inventoryapp.service.notifications.NotificationService;
import com.mirceone.inventoryapp.service.support.AfterCommitExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirmServiceTest {

    @Mock
    private FirmRepository firmRepository;
    @Mock
    private FirmMemberRepository firmMemberRepository;
    @Mock
    private WorkOrderFileRepository workOrderFileRepository;
    @Mock
    private WorkOrderInvoiceRepository workOrderInvoiceRepository;
    @Mock
    private WorkOrderActivityRepository workOrderActivityRepository;
    @Mock
    private FirmStatusHistoryRepository firmStatusHistoryRepository;
    @Mock
    private BlobStorage blobStorage;
    @Mock
    private CategoryService categoryService;
    @Mock
    private NotificationService notificationService;

    private FirmService firmService;

    private UUID userId;
    private UUID firmId;
    private FirmEntity firm;

    @BeforeEach
    void setUp() {
        FirmAccessService firmAccessService = new FirmAccessService(firmMemberRepository, firmRepository);
        firmService = new FirmService(
                firmRepository,
                firmMemberRepository,
                workOrderFileRepository,
                workOrderInvoiceRepository,
                workOrderActivityRepository,
                firmStatusHistoryRepository,
                blobStorage,
                categoryService,
                firmAccessService,
                notificationService,
                new AfterCommitExecutor()
        );
        userId = UUID.randomUUID();
        firmId = UUID.randomUUID();
        firm = new FirmEntity(userId, "Old Name");
        org.springframework.test.util.ReflectionTestUtils.setField(firm, "id", firmId);
    }

    @Test
    void renameFirmAsOwnerUpdatesName() {
        when(firmMemberRepository.findByFirmIdAndUserId(firmId, userId))
                .thenReturn(Optional.of(new FirmMemberEntity(firmId, userId, MemberRole.OWNER)));
        when(firmRepository.findById(firmId)).thenReturn(Optional.of(firm));
        when(firmRepository.save(any(FirmEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        FirmContracts.FirmSummary result = firmService.renameFirm(
                userId, firmId, new FirmContracts.UpdateFirmSpec("  New Name  ")
        );

        assertEquals("New Name", result.name());
        assertEquals(FirmStatus.ACTIVE, result.status());
    }

    @Test
    void renameFirmWhenPausedReturnsForbidden() {
        firm.setStatus(FirmStatus.PAUSED);
        when(firmMemberRepository.findByFirmIdAndUserId(firmId, userId))
                .thenReturn(Optional.of(new FirmMemberEntity(firmId, userId, MemberRole.OWNER)));
        when(firmRepository.findById(firmId)).thenReturn(Optional.of(firm));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> firmService.renameFirm(userId, firmId, new FirmContracts.UpdateFirmSpec("X"))
        );
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void updateFirmStatusAsOwnerSetsPaused() {
        when(firmMemberRepository.findByFirmIdAndUserId(firmId, userId))
                .thenReturn(Optional.of(new FirmMemberEntity(firmId, userId, MemberRole.OWNER)));
        when(firmRepository.findById(firmId)).thenReturn(Optional.of(firm));
        when(firmRepository.save(any(FirmEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        FirmContracts.FirmSummary result = firmService.updateFirmStatus(
                userId, firmId, new FirmContracts.UpdateFirmStatusSpec(FirmStatus.PAUSED, null)
        );

        assertEquals(FirmStatus.PAUSED, result.status());
        assertEquals("Paused", result.statusDisplayLabel());
        verify(notificationService).notifyFirmStatusChangedAfterCommit(
                firmId, FirmStatus.ACTIVE, FirmStatus.PAUSED, null, com.mirceone.inventoryapp.model.FirmStatusChangeSource.MANUAL
        );
    }

    @Test
    void deleteFirmAsOwnerDeletesFilesFirmAndBlobPrefix() throws Exception {
        when(firmMemberRepository.findByFirmIdAndUserId(firmId, userId))
                .thenReturn(Optional.of(new FirmMemberEntity(firmId, userId, MemberRole.OWNER)));
        when(firmRepository.findById(firmId)).thenReturn(Optional.of(firm));

        firmService.deleteFirm(userId, firmId);

        verify(workOrderFileRepository).deleteByFirmId(firmId);
        verify(workOrderInvoiceRepository).deleteByFirmId(firmId);
        verify(workOrderActivityRepository).deleteByFirmId(firmId);
        verify(firmRepository).deleteById(firmId);
        verify(blobStorage).deleteByPrefix(firmId + "/");
    }
}
