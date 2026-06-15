package com.mirceone.inventoryapp.service.workorders;

import java.util.List;
import java.util.UUID;

/**
 * Read model for one node of the work order folder tree.
 */
public record FolderTreeNode(
        UUID id,
        String name,
        String path,
        boolean catchAll,
        long fileCount,
        List<String> extensions,
        List<FolderTreeNode> children
) {
}
