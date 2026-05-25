package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.FirmInvitationEntity;
import com.mirceone.inventoryapp.model.FirmInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FirmInvitationRepository extends JpaRepository<FirmInvitationEntity, UUID> {

    Optional<FirmInvitationEntity> findByTokenHash(String tokenHash);

    Optional<FirmInvitationEntity> findByFirmIdAndEmailAndStatus(UUID firmId, String email, FirmInvitationStatus status);

    List<FirmInvitationEntity> findAllByFirmIdAndStatusOrderByCreatedAtDesc(UUID firmId, FirmInvitationStatus status);

    List<FirmInvitationEntity> findAllByStatusAndExpiresAtBefore(
            FirmInvitationStatus status,
            Instant expiresAt
    );
}
