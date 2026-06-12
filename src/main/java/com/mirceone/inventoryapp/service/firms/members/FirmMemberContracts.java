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

    /**
     * Result of requesting an ownership transfer.
     *
     * <p><strong>Security:</strong> {@code rawConfirmationCode} is the possession factor that is
     * delivered to the current owner by email. It is exposed here only as an internal/test seam and
     * MUST NOT be serialized into any HTTP response — doing so would let the requester confirm the
     * transfer without proving access to the owner's mailbox. The controller returns {@code void}.
     */
    public record TransferOwnershipRequestResult(UUID newOwnerUserId, java.time.Instant expiresAt, String rawConfirmationCode) {
    }
}
