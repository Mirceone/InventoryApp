package com.mirceone.inventoryapp.service.firms;

import com.mirceone.inventoryapp.model.FirmDocumentEntity;
import com.mirceone.inventoryapp.model.FirmEntity;
import com.mirceone.inventoryapp.model.FirmMemberEntity;
import com.mirceone.inventoryapp.model.FirmStatus;
import com.mirceone.inventoryapp.model.MemberRole;
import com.mirceone.inventoryapp.repository.FirmDocumentRepository;
import com.mirceone.inventoryapp.repository.FirmMemberRepository;
import com.mirceone.inventoryapp.repository.FirmRepository;
import com.mirceone.inventoryapp.service.documents.storage.DocumentStorage;
import com.mirceone.inventoryapp.service.firms.access.FirmAccessService;
import com.mirceone.inventoryapp.service.inventory.CategoryService;
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
    private FirmDocumentRepository firmDocumentRepository;
    @Mock
    private DocumentStorage documentStorage;
    @Mock
    private CategoryService categoryService;

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
                firmDocumentRepository,
                documentStorage,
                categoryService,
                firmAccessService
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
        assertEquals("În pauză", result.statusDisplayLabel());
    }

    @Test
    void deleteFirmAsOwnerDeletesFirmAndCollectsStorageKeys() {
        when(firmMemberRepository.findByFirmIdAndUserId(firmId, userId))
                .thenReturn(Optional.of(new FirmMemberEntity(firmId, userId, MemberRole.OWNER)));
        when(firmRepository.findById(firmId)).thenReturn(Optional.of(firm));
        FirmDocumentEntity doc = mock(FirmDocumentEntity.class);
        when(doc.getStorageKey()).thenReturn(firmId + "/dossiers/x/f.pdf");
        when(firmDocumentRepository.findAllByFirmId(firmId)).thenReturn(List.of(doc));

        firmService.deleteFirm(userId, firmId);

        verify(firmRepository).deleteById(firmId);
    }
}
