package com.mirceone.inventoryapp.service.firms.access;

import com.mirceone.inventoryapp.model.MemberRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberRoleCatalogTest {

    @Test
    void ownerDisplayLabelIsAdmin() {
        assertEquals("Admin", MemberRoleCatalog.displayLabel(MemberRole.OWNER));
    }

    @Test
    void memberDisplayLabelIsAngajat() {
        assertEquals("Angajat", MemberRoleCatalog.displayLabel(MemberRole.MEMBER));
    }

    @Test
    void ownerMatchesAdminAlias() {
        assertTrue(MemberRoleCatalog.matchesAlias("admin", MemberRole.OWNER));
        assertTrue(MemberRoleCatalog.matchesAlias("ADMINISTRATOR", MemberRole.OWNER));
        assertFalse(MemberRoleCatalog.matchesAlias("admin", MemberRole.MEMBER));
    }

    @Test
    void memberMatchesEmployeeAlias() {
        assertTrue(MemberRoleCatalog.matchesAlias("angajat", MemberRole.MEMBER));
        assertTrue(MemberRoleCatalog.matchesAlias("EMPLOYEE", MemberRole.MEMBER));
    }
}
