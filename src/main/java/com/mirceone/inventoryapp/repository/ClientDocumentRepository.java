package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.ClientDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientDocumentRepository extends JpaRepository<ClientDocumentEntity, UUID> {

    List<ClientDocumentEntity> findAllByFirmIdAndClientIdOrderByCreatedAtDesc(UUID firmId, UUID clientId);

    Optional<ClientDocumentEntity> findByIdAndFirmIdAndClientId(UUID id, UUID firmId, UUID clientId);
}
