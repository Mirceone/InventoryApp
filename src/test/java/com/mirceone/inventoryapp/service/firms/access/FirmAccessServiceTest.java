package com.mirceone.inventoryapp.service.firms.access;

import com.mirceone.inventoryapp.model.FirmEntity;
import com.mirceone.inventoryapp.model.FirmMemberEntity;
import com.mirceone.inventoryapp.model.FirmStatus;
import com.mirceone.inventoryapp.model.MemberRole;
import com.mirceone.inventoryapp.repository.FirmMemberRepository;
import com.mirceone.inventoryapp.repository.FirmRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirmAccessServiceTest {

    @Mock
    private FirmMemberRepository firmMemberRepository;
    @Mock
    private FirmRepository firmRepository;

    private FirmAccessService firmAccessService;

    private UUID firmId;
    private UUID userId;
    private FirmEntity firm;

    @BeforeEach
    void setUp() {
        firmAccessService = new FirmAccessService(firmMemberRepository, firmRepository);
        firmId = UUID.randomUUID();
        userId = UUID.randomUUID();
        firm = new FirmEntity(userId, "Test Firm");
        org.springframework.test.util.ReflectionTestUtils.setField(firm, "id", firmId);
    }

    @Test
    void resolveMembershipReturnsRole() {
        when(firmMemberRepository.findByFirmIdAndUserId(firmId, userId))
                .thenReturn(Optional.of(new FirmMemberEntity(firmId, userId, MemberRole.MEMBER)));

        FirmMembership membership = firmAccessService.resolveMembership(firmId, userId);

        assertEquals(MemberRole.MEMBER, membership.role());
    }

    @Test
    void requirePermissionRejectsMemberForFirmUpdate() {
        when(firmMemberRepository.findByFirmIdAndUserId(firmId, userId))
                .thenReturn(Optional.of(new FirmMemberEntity(firmId, userId, MemberRole.MEMBER)));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> firmAccessService.requirePermission(firmId, userId, FirmPermission.FIRM_UPDATE)
        );
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void requireFirmOperationalAllowsActiveFirm() {
        when(firmRepository.findById(firmId)).thenReturn(Optional.of(firm));

        firmAccessService.requireFirmOperational(firmId);
    }

    @Test
    void requireFirmOperationalRejectsPausedFirm() {
        firm.setStatus(FirmStatus.PAUSED);
        when(firmRepository.findById(firmId)).thenReturn(Optional.of(firm));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> firmAccessService.requireFirmOperational(firmId)
        );
        assertEquals(403, ex.getStatusCode().value());
        assertEquals("Firm operations are paused", ex.getReason());
    }

    @Test
    void requireFirmOperationalRejectsCriticalFirmWithMessage() {
        firm.setStatus(FirmStatus.CRITICAL);
        firm.setStatusMessage("Database integrity check failed");
        when(firmRepository.findById(firmId)).thenReturn(Optional.of(firm));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> firmAccessService.requireFirmOperational(firmId)
        );
        assertEquals(403, ex.getStatusCode().value());
        assertEquals("Firm is in critical state: Database integrity check failed", ex.getReason());
    }
}
