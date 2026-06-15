package com.mirceone.inventoryapp.api.firms.members;

import com.mirceone.inventoryapp.model.MemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteMemberRequest(
        @NotBlank @Email String email,
        @NotNull MemberRole role
) {
}
