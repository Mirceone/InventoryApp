package com.mirceone.inventoryapp.service.firms.status;

import com.mirceone.inventoryapp.model.FirmEntity;
import com.mirceone.inventoryapp.model.FirmMemberEntity;
import com.mirceone.inventoryapp.model.FirmStatus;
import com.mirceone.inventoryapp.model.MemberRole;
import com.mirceone.inventoryapp.repository.FirmMemberRepository;
import com.mirceone.inventoryapp.repository.FirmRepository;
import com.mirceone.inventoryapp.service.firms.FirmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirmOwnershipConsistencyWorkerTest {

    @Mock
    private FirmRepository firmRepository;
    @Mock
    private FirmMemberRepository firmMemberRepository;
    @Mock
    private FirmService firmService;

    private FirmOwnershipConsistencyWorker worker;

    @BeforeEach
    void setUp() {
        worker = new FirmOwnershipConsistencyWorker(firmRepository, firmMemberRepository, firmService);
    }

    @Test
    void inconsistentOwnerMembershipMarksFirmCritical() {
        UUID ownerUserId = UUID.randomUUID();
        FirmEntity firm = new FirmEntity(ownerUserId, "Demo");
        UUID firmId = UUID.randomUUID();
        ReflectionTestUtils.setField(firm, "id", firmId);

        when(firmRepository.findAll()).thenReturn(List.of(firm));
        when(firmMemberRepository.findByFirmIdAndUserId(firmId, ownerUserId)).thenReturn(Optional.empty());
        when(firmMemberRepository.countByFirmIdAndRole(firmId, MemberRole.OWNER)).thenReturn(0L);

        worker.verifyOwnershipConsistency();

        verify(firmService).setFirmStatusSystem(firmId, FirmStatus.CRITICAL, FirmOwnershipConsistencyWorker.INCONSISTENT_OWNER_MESSAGE);
    }

    @Test
    void consistentOwnerMembershipDoesNothing() {
        UUID ownerUserId = UUID.randomUUID();
        FirmEntity firm = new FirmEntity(ownerUserId, "Demo");
        UUID firmId = UUID.randomUUID();
        ReflectionTestUtils.setField(firm, "id", firmId);
        FirmMemberEntity ownerMember = new FirmMemberEntity(firmId, ownerUserId, MemberRole.OWNER);

        when(firmRepository.findAll()).thenReturn(List.of(firm));
        when(firmMemberRepository.findByFirmIdAndUserId(firmId, ownerUserId)).thenReturn(Optional.of(ownerMember));
        when(firmMemberRepository.countByFirmIdAndRole(firmId, MemberRole.OWNER)).thenReturn(1L);

        worker.verifyOwnershipConsistency();

        verifyNoInteractions(firmService);
    }

    @Test
    void recoveredOwnershipConsistencyReactivatesFirm() {
        UUID ownerUserId = UUID.randomUUID();
        FirmEntity firm = new FirmEntity(ownerUserId, "Demo");
        UUID firmId = UUID.randomUUID();
        ReflectionTestUtils.setField(firm, "id", firmId);
        ReflectionTestUtils.setField(firm, "status", FirmStatus.CRITICAL);
        ReflectionTestUtils.setField(
                firm,
                "statusMessage",
                FirmOwnershipConsistencyWorker.INCONSISTENT_OWNER_MESSAGE
        );
        FirmMemberEntity ownerMember = new FirmMemberEntity(firmId, ownerUserId, MemberRole.OWNER);

        when(firmRepository.findAll()).thenReturn(List.of(firm));
        when(firmMemberRepository.findByFirmIdAndUserId(firmId, ownerUserId)).thenReturn(Optional.of(ownerMember));
        when(firmMemberRepository.countByFirmIdAndRole(firmId, MemberRole.OWNER)).thenReturn(1L);

        worker.verifyOwnershipConsistency();

        verify(firmService).setFirmStatusSystem(firmId, FirmStatus.ACTIVE, null);
    }
}
