package com.mirceone.inventoryapp.service.firms.access;

import com.mirceone.inventoryapp.model.MemberRole;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MemberRoleCatalog {

    private static final Map<MemberRole, Set<String>> ALIASES = Map.of(
            MemberRole.OWNER, Set.of("ADMIN", "ADMINISTRATOR"),
            MemberRole.MEMBER, Set.of("EMPLOYEE", "ANGAJAT")
    );

    private static final Map<MemberRole, String> DISPLAY_LABELS = Map.of(
            MemberRole.OWNER, "Admin",
            MemberRole.MEMBER, "Angajat"
    );

    private MemberRoleCatalog() {
    }

    public static String displayLabel(MemberRole role) {
        return DISPLAY_LABELS.getOrDefault(role, role.name());
    }

    public static Set<String> aliases(MemberRole role) {
        return ALIASES.getOrDefault(role, Set.of());
    }

    public static boolean matchesAlias(String alias, MemberRole role) {
        if (alias == null || alias.isBlank()) {
            return false;
        }
        String normalized = alias.strip().toUpperCase(Locale.ROOT);
        if (role.name().equalsIgnoreCase(normalized)) {
            return true;
        }
        return aliases(role).contains(normalized);
    }
}
