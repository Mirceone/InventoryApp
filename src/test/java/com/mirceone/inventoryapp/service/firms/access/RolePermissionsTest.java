package com.mirceone.inventoryapp.service.firms.access;

import com.mirceone.inventoryapp.model.MemberRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RolePermissionsTest {

    @Test
    void ownerHasFirmManagementPermissions() {
        assertTrue(RolePermissions.allowed(MemberRole.OWNER, FirmPermission.FIRM_UPDATE));
        assertTrue(RolePermissions.allowed(MemberRole.OWNER, FirmPermission.FIRM_DELETE));
        assertTrue(RolePermissions.allowed(MemberRole.OWNER, FirmPermission.MEMBER_MANAGE));
    }

    @Test
    void memberCannotManageFirmOrMembers() {
        assertFalse(RolePermissions.allowed(MemberRole.MEMBER, FirmPermission.FIRM_UPDATE));
        assertFalse(RolePermissions.allowed(MemberRole.MEMBER, FirmPermission.FIRM_DELETE));
        assertFalse(RolePermissions.allowed(MemberRole.MEMBER, FirmPermission.MEMBER_MANAGE));
    }

    @Test
    void ownerAndMemberShareEmployeePermissions() {
        for (MemberRole role : new MemberRole[]{MemberRole.OWNER, MemberRole.MEMBER}) {
            assertTrue(RolePermissions.allowed(role, FirmPermission.FIRM_READ));
            assertTrue(RolePermissions.allowed(role, FirmPermission.INVENTORY_WRITE));
            assertTrue(RolePermissions.allowed(role, FirmPermission.DOCUMENT_WRITE));
        }
    }
}
