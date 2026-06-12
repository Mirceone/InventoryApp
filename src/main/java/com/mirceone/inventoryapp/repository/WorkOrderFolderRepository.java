package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.WorkOrderFolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkOrderFolderRepository extends JpaRepository<WorkOrderFolderEntity, UUID> {

    List<WorkOrderFolderEntity> findAllByWorkOrderIdOrderBySortOrderAscNameAsc(UUID workOrderId);

    Optional<WorkOrderFolderEntity> findByIdAndWorkOrderId(UUID id, UUID workOrderId);

    Optional<WorkOrderFolderEntity> findByWorkOrderIdAndCatchAllTrue(UUID workOrderId);

    boolean existsByWorkOrderIdAndParentIdIsNullAndNameIgnoreCase(UUID workOrderId, String name);

    boolean existsByWorkOrderIdAndParentIdAndNameIgnoreCase(UUID workOrderId, UUID parentId, String name);
}
