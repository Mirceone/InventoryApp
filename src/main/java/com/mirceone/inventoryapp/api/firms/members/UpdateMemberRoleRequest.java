package com.mirceone.inventoryapp.api.firms.members;

import com.mirceone.inventoryapp.model.MemberRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull MemberRole role
) {
}
