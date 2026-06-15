package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.WorkOrderInvoiceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkOrderInvoiceRepository extends JpaRepository<WorkOrderInvoiceEntity, UUID> {

    Page<WorkOrderInvoiceEntity> findByWorkOrderIdOrderByCreatedAtDesc(UUID workOrderId, Pageable pageable);

    Optional<WorkOrderInvoiceEntity> findByIdAndFirmIdAndWorkOrderId(UUID id, UUID firmId, UUID workOrderId);

    long countByWorkOrderId(UUID workOrderId);

    long countByFirmId(UUID firmId);

    @Query(value = """
            SELECT * FROM work_order_invoices
            WHERE processing_status = 'PENDING'
            ORDER BY created_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<WorkOrderInvoiceEntity> lockNextPendingBatch(@Param("limit") int limit);

    @Query(value = """
            SELECT * FROM work_order_invoices
            WHERE id = :id AND processing_status = 'PENDING'
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<WorkOrderInvoiceEntity> lockPendingById(@Param("id") UUID id);

    @Modifying
    @Query("DELETE FROM WorkOrderInvoiceEntity i WHERE i.workOrderId = :workOrderId")
    void deleteByWorkOrderId(@Param("workOrderId") UUID workOrderId);

    @Modifying
    @Query("DELETE FROM WorkOrderInvoiceEntity i WHERE i.firmId = :firmId")
    void deleteByFirmId(@Param("firmId") UUID firmId);
}
