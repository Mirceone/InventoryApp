package com.mirceone.inventoryapp.api.workorders;

import com.mirceone.inventoryapp.service.workorders.FolderTreeNode;
import com.mirceone.inventoryapp.service.workorders.WorkOrderContracts;

import java.util.List;

public final class FolderWebMapper {

    private FolderWebMapper() {
    }

    public static WorkOrderContracts.CreateFolderSpec toCreateFolderSpec(CreateFolderRequest request) {
        return new WorkOrderContracts.CreateFolderSpec(
                request.parentId(),
                request.name(),
                request.extensions() != null ? request.extensions() : List.of()
        );
    }

    public static WorkOrderContracts.UpdateFolderSpec toUpdateFolderSpec(UpdateFolderRequest request) {
        return new WorkOrderContracts.UpdateFolderSpec(
                request.getName(),
                request.getParentId(),
                request.isParentPresent()
        );
    }

    public static FolderNodeResponse toResponse(FolderTreeNode node) {
        return new FolderNodeResponse(
                node.id(),
                node.name(),
                node.path(),
                node.catchAll(),
                node.fileCount(),
                node.extensions(),
                node.children().stream().map(FolderWebMapper::toResponse).toList()
        );
    }

    public static List<FolderNodeResponse> toResponseList(List<FolderTreeNode> nodes) {
        return nodes.stream().map(FolderWebMapper::toResponse).toList();
    }
}
