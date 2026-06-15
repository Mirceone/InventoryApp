package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.WorkOrderActivityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WorkOrderActivityRepository extends JpaRepository<WorkOrderActivityEntity, UUID> {

    List<WorkOrderActivityEntity> findByWorkOrderIdOrderByCreatedAtDesc(UUID workOrderId);

    @Modifying
    @Query("DELETE FROM WorkOrderActivityEntity a WHERE a.firmId = :firmId")
    void deleteByFirmId(@Param("firmId") UUID firmId);
}
