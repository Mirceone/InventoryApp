package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.WorkOrderFolderRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkOrderFolderRuleRepository extends JpaRepository<WorkOrderFolderRuleEntity, UUID> {

    List<WorkOrderFolderRuleEntity> findAllByWorkOrderId(UUID workOrderId);

    List<WorkOrderFolderRuleEntity> findAllByFolderId(UUID folderId);

    Optional<WorkOrderFolderRuleEntity> findByWorkOrderIdAndExtension(UUID workOrderId, String extension);

    void deleteByFolderId(UUID folderId);
}
