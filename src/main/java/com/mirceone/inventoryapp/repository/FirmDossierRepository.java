package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.FirmDossierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FirmDossierRepository extends JpaRepository<FirmDossierEntity, UUID> {

    List<FirmDossierEntity> findAllByFirmIdOrderByCreatedAtDesc(UUID firmId);

    Optional<FirmDossierEntity> findByIdAndFirmId(UUID id, UUID firmId);

    boolean existsByFirmIdAndNameIgnoreCase(UUID firmId, String name);

    boolean existsByFirmIdAndNameIgnoreCaseAndIdNot(UUID firmId, String name, UUID id);
}
