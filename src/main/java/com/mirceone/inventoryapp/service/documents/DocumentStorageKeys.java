package com.mirceone.inventoryapp.service.documents;

import java.util.UUID;

public final class DocumentStorageKeys {

    public static final String PENDING_SEGMENT = "_pending";
    public static final String DOSSIERS_SEGMENT = "dossiers";

    private DocumentStorageKeys() {
    }

    public static String pendingKey(UUID firmId, UUID dossierId, UUID documentId, String fileSuffix) {
        return firmId + "/" + DOSSIERS_SEGMENT + "/" + dossierId + "/" + PENDING_SEGMENT + "/" + documentId + fileSuffix;
    }

    public static String classifiedKey(UUID firmId, UUID dossierId, String folderPath, UUID documentId, String fileSuffix) {
        String folderSeg = FolderPathSanitizer.storageSegment(folderPath);
        return firmId + "/" + DOSSIERS_SEGMENT + "/" + dossierId + "/" + folderSeg + documentId + fileSuffix;
    }

    public static String fileSuffixFromStorageKey(String storageKey) {
        int slash = storageKey.lastIndexOf('/');
        if (slash < 0 || slash == storageKey.length() - 1) {
            return ".bin";
        }
        String name = storageKey.substring(slash + 1);
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return ".bin";
        }
        return name.substring(dot);
    }
}
