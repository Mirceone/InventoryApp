package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.FirmStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FirmStatusHistoryRepository extends JpaRepository<FirmStatusHistoryEntity, UUID> {

    List<FirmStatusHistoryEntity> findAllByFirmIdOrderByCreatedAtDesc(UUID firmId);
}
