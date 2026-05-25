package com.mirceone.inventoryapp.service.firms.members;

import com.mirceone.inventoryapp.model.MemberRole;

import java.time.Instant;
import java.util.UUID;

public final class FirmMemberContracts {

    private FirmMemberContracts() {
    }

    public record FirmMemberSummary(
            UUID userId,
            String email,
            String displayName,
            MemberRole role,
            String roleDisplayLabel,
            Instant joinedAt
    ) {
    }

    public record UpdateMemberRoleSpec(MemberRole role) {
    }

    public record TransferOwnershipSpec(UUID newOwnerUserId) {
    }

    public record ConfirmOwnershipTransferSpec(UUID newOwnerUserId, String confirmationCode) {
    }

    public record TransferOwnershipRequestResult(UUID newOwnerUserId, java.time.Instant expiresAt, String rawConfirmationCode) {
    }
}
