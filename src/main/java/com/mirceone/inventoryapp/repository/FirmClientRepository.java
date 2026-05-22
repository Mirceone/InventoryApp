package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.FirmClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FirmClientRepository extends JpaRepository<FirmClientEntity, UUID> {

    List<FirmClientEntity> findAllByFirmIdOrderByDisplayNameAsc(UUID firmId);

    Optional<FirmClientEntity> findByIdAndFirmId(UUID id, UUID firmId);
}
