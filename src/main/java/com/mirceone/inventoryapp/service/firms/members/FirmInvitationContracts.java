package com.mirceone.inventoryapp.service.firms.members;

import com.mirceone.inventoryapp.model.FirmInvitationStatus;
import com.mirceone.inventoryapp.model.MemberRole;

import java.time.Instant;
import java.util.UUID;

public final class FirmInvitationContracts {

    private FirmInvitationContracts() {
    }

    public record CreateInvitationSpec(String email, MemberRole role) {
    }

    public record CreateInvitationResult(InvitationSummary summary, String rawToken) {
    }

    public record InvitationSummary(
            UUID id,
            String email,
            MemberRole role,
            String roleDisplayLabel,
            FirmInvitationStatus status,
            Instant expiresAt,
            Instant createdAt
    ) {
    }

    public record InvitationPreview(
            String firmName,
            String email,
            String maskedEmail,
            MemberRole role,
            String roleDisplayLabel,
            Instant expiresAt,
            boolean accountExists
    ) {
    }

    public record AcceptInvitationSpec(String token, String displayName, String password) {
    }
}
