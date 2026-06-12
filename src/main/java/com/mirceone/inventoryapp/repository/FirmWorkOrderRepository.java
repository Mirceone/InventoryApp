package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.FirmWorkOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FirmWorkOrderRepository extends JpaRepository<FirmWorkOrderEntity, UUID> {

    long countByFirmId(UUID firmId);

    List<FirmWorkOrderEntity> findAllByFirmIdOrderByCreatedAtDesc(UUID firmId);

    List<FirmWorkOrderEntity> findTop5ByFirmIdOrderByCreatedAtDesc(UUID firmId);

    Optional<FirmWorkOrderEntity> findByIdAndFirmId(UUID id, UUID firmId);

    boolean existsByFirmIdAndNameIgnoreCase(UUID firmId, String name);

    boolean existsByFirmIdAndNameIgnoreCaseAndIdNot(UUID firmId, String name, UUID id);
}
