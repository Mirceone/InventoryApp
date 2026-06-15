package com.mirceone.inventoryapp.service.workorders;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Folder structure seeded into every new work order. The application never references
 * these names afterwards; the catch-all flag (not a name) drives classification fallback.
 * A future firm-level template table can replace this bean as the seed source.
 */
@Component
public class DefaultFolderTemplate {

    public record TemplateFolder(String name, boolean catchAll, List<String> extensions) {
    }

    public List<TemplateFolder> folders() {
        return List.of(
                new TemplateFolder("Documents", false, List.of()),
                new TemplateFolder("Misc", true, List.of())
        );
    }
}
