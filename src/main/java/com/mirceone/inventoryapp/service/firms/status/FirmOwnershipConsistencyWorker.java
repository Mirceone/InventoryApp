package com.mirceone.inventoryapp.service.firms.status;

import com.mirceone.inventoryapp.model.FirmEntity;
import com.mirceone.inventoryapp.model.MemberRole;
import com.mirceone.inventoryapp.repository.FirmMemberRepository;
import com.mirceone.inventoryapp.repository.FirmRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class FirmOwnershipConsistencyWorker {

    public static final String INCONSISTENT_OWNER_MESSAGE = "Owner membership is inconsistent with firm owner reference";

    private final FirmRepository firmRepository;
    private final FirmMemberRepository firmMemberRepository;
    private final FirmStatusSystemService firmStatusSystemService;

    public FirmOwnershipConsistencyWorker(
            FirmRepository firmRepository,
            FirmMemberRepository firmMemberRepository,
            FirmStatusSystemService firmStatusSystemService
    ) {
        this.firmRepository = firmRepository;
        this.firmMemberRepository = firmMemberRepository;
        this.firmStatusSystemService = firmStatusSystemService;
    }

    @Scheduled(fixedDelayString = "${app.firms.status-consistency-poll-interval:15m}")
    public void verifyOwnershipConsistency() {
        for (FirmEntity firm : firmRepository.findAll()) {
            boolean consistent = firmMemberRepository.findByFirmIdAndUserId(firm.getId(), firm.getOwnerUserId())
                    .map(member -> member.getRole() == MemberRole.OWNER)
                    .orElse(false);
            if (!consistent) {
                firmStatusSystemService.markCritical(firm.getId(), INCONSISTENT_OWNER_MESSAGE);
            }
        }
    }
}
