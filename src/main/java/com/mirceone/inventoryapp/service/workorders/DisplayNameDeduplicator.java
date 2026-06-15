package com.mirceone.inventoryapp.service.workorders;

import com.mirceone.inventoryapp.repository.WorkOrderFileRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;

/**
 * Display names are unique (case-insensitive) within a folder; collisions get a
 * "name (n).ext" suffix like desktop file managers.
 */
@Component
public class DisplayNameDeduplicator {

    private static final int MAX_ATTEMPTS = 200;
    private static final int MAX_NAME_LENGTH = 255;

    private final WorkOrderFileRepository fileRepository;

    public DisplayNameDeduplicator(WorkOrderFileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public String uniqueName(UUID folderId, String desired) {
        if (!fileRepository.existsByFolderIdAndDisplayNameIgnoreCase(folderId, desired)) {
            return desired;
        }
        int dot = desired.lastIndexOf('.');
        String base = dot > 0 ? desired.substring(0, dot) : desired;
        String ext = dot > 0 ? desired.substring(dot) : "";
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            String suffix = " (" + i + ")";
            String trimmedBase = trimBase(base, suffix.length() + ext.length());
            String candidate = trimmedBase + suffix + ext;
            if (!fileRepository.existsByFolderIdAndDisplayNameIgnoreCase(folderId, candidate)) {
                return candidate;
            }
        }
        throw new ResponseStatusException(CONFLICT, "Too many files with the same name in this folder");
    }

    private static String trimBase(String base, int reserved) {
        int maxBase = MAX_NAME_LENGTH - reserved;
        return base.length() > maxBase ? base.substring(0, maxBase) : base;
    }
}
