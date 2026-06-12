package com.mirceone.inventoryapp.service.firms.status;

import com.mirceone.inventoryapp.model.FirmEntity;
import com.mirceone.inventoryapp.model.FirmStatus;
import com.mirceone.inventoryapp.model.MemberRole;
import com.mirceone.inventoryapp.repository.FirmMemberRepository;
import com.mirceone.inventoryapp.repository.FirmRepository;
import com.mirceone.inventoryapp.service.firms.FirmService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class FirmOwnershipConsistencyWorker {

    public static final String INCONSISTENT_OWNER_MESSAGE = "Owner membership is inconsistent with firm owner reference";

    private final FirmRepository firmRepository;
    private final FirmMemberRepository firmMemberRepository;
    private final FirmService firmService;

    public FirmOwnershipConsistencyWorker(
            FirmRepository firmRepository,
            FirmMemberRepository firmMemberRepository,
            FirmService firmService
    ) {
        this.firmRepository = firmRepository;
        this.firmMemberRepository = firmMemberRepository;
        this.firmService = firmService;
    }

    @Scheduled(fixedDelayString = "${app.firms.status-consistency-poll-interval:15m}")
    public void verifyOwnershipConsistency() {
        for (FirmEntity firm : firmRepository.findAll()) {
            boolean ownerReferenceMatches = firmMemberRepository.findByFirmIdAndUserId(firm.getId(), firm.getOwnerUserId())
                    .map(member -> member.getRole() == MemberRole.OWNER)
                    .orElse(false);
            boolean hasSingleOwnerMembership =
                    firmMemberRepository.countByFirmIdAndRole(firm.getId(), MemberRole.OWNER) == 1;
            boolean consistent = ownerReferenceMatches && hasSingleOwnerMembership;
            if (!consistent) {
                firmService.setFirmStatusSystem(firm.getId(), FirmStatus.CRITICAL, INCONSISTENT_OWNER_MESSAGE);
            } else if (isOwnershipConsistencyCriticalStatus(firm)) {
                firmService.setFirmStatusSystem(firm.getId(), FirmStatus.ACTIVE, null);
            }
        }
    }

    private static boolean isOwnershipConsistencyCriticalStatus(FirmEntity firm) {
        return firm.getStatus() == FirmStatus.CRITICAL
                && INCONSISTENT_OWNER_MESSAGE.equals(firm.getStatusMessage());
    }
}
