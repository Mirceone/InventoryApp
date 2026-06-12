package com.mirceone.inventoryapp.service.workorders;

import com.mirceone.inventoryapp.model.WorkOrderFolderEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Materializes "Parent/Child" display paths from folder ancestry. Paths are derived,
 * never stored, so folder renames stay pure DB updates.
 */
public final class FolderPaths {

    private FolderPaths() {
    }

    public static Map<UUID, String> buildPathMap(List<WorkOrderFolderEntity> folders) {
        Map<UUID, WorkOrderFolderEntity> byId = new HashMap<>();
        for (WorkOrderFolderEntity folder : folders) {
            byId.put(folder.getId(), folder);
        }
        Map<UUID, String> paths = new HashMap<>();
        for (WorkOrderFolderEntity folder : folders) {
            paths.put(folder.getId(), pathOf(folder, byId, paths));
        }
        return paths;
    }

    private static String pathOf(
            WorkOrderFolderEntity folder,
            Map<UUID, WorkOrderFolderEntity> byId,
            Map<UUID, String> memo
    ) {
        String cached = memo.get(folder.getId());
        if (cached != null) {
            return cached;
        }
        if (folder.getParentId() == null) {
            return folder.getName();
        }
        WorkOrderFolderEntity parent = byId.get(folder.getParentId());
        if (parent == null) {
            return folder.getName();
        }
        return pathOf(parent, byId, memo) + "/" + folder.getName();
    }
}
