package com.mirceone.inventoryapp.service.firms.access;

import com.mirceone.inventoryapp.model.MemberRole;

import java.util.UUID;

public record FirmMembership(UUID firmId, UUID userId, MemberRole role) {
}
