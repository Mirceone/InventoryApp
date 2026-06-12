package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.WorkOrderFileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkOrderFileRepository extends JpaRepository<WorkOrderFileEntity, UUID> {

    Page<WorkOrderFileEntity> findByWorkOrderIdOrderByCreatedAtDesc(UUID workOrderId, Pageable pageable);

    Page<WorkOrderFileEntity> findByFolderIdOrderByCreatedAtDesc(UUID folderId, Pageable pageable);

    Optional<WorkOrderFileEntity> findByIdAndFirmIdAndWorkOrderId(UUID id, UUID firmId, UUID workOrderId);

    List<WorkOrderFileEntity> findAllByFolderId(UUID folderId);

    List<WorkOrderFileEntity> findAllByFolderIdIn(List<UUID> folderIds);

    boolean existsByFolderIdAndDisplayNameIgnoreCase(UUID folderId, String displayName);

    boolean existsByFolderIdIn(List<UUID> folderIds);

    long countByWorkOrderId(UUID workOrderId);

    long countByFolderId(UUID folderId);

    long countByFirmId(UUID firmId);

    @Query("""
            SELECT f.folderId, count(f)
            FROM WorkOrderFileEntity f
            WHERE f.workOrderId = :workOrderId
            GROUP BY f.folderId
            """)
    List<Object[]> countByFolderForWorkOrder(@Param("workOrderId") UUID workOrderId);

    @Modifying
    @Query("DELETE FROM WorkOrderFileEntity f WHERE f.workOrderId = :workOrderId")
    void deleteByWorkOrderId(@Param("workOrderId") UUID workOrderId);

    @Modifying
    @Query("DELETE FROM WorkOrderFileEntity f WHERE f.firmId = :firmId")
    void deleteByFirmId(@Param("firmId") UUID firmId);
}
