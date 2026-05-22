package com.mirceone.inventoryapp.service.firms.access;

import com.mirceone.inventoryapp.model.MemberRole;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class RolePermissions {

    private static final Set<FirmPermission> MEMBER_AND_OWNER = EnumSet.of(
            FirmPermission.FIRM_READ,
            FirmPermission.INVENTORY_WRITE,
            FirmPermission.DOCUMENT_WRITE
    );

    private static final Set<FirmPermission> OWNER_ONLY = EnumSet.of(
            FirmPermission.FIRM_UPDATE,
            FirmPermission.FIRM_DELETE,
            FirmPermission.MEMBER_MANAGE
    );

    private static final Map<MemberRole, Set<FirmPermission>> BY_ROLE = Map.of(
            MemberRole.OWNER, union(MEMBER_AND_OWNER, OWNER_ONLY),
            MemberRole.MEMBER, Set.copyOf(MEMBER_AND_OWNER)
    );

    private RolePermissions() {
    }

    public static boolean allowed(MemberRole role, FirmPermission permission) {
        Set<FirmPermission> permissions = BY_ROLE.get(role);
        return permissions != null && permissions.contains(permission);
    }

    private static Set<FirmPermission> union(Set<FirmPermission> a, Set<FirmPermission> b) {
        EnumSet<FirmPermission> merged = EnumSet.copyOf(a);
        merged.addAll(b);
        return Set.copyOf(merged);
    }
}
