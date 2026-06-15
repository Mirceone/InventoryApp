package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.FirmOwnershipTransferConfirmationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface FirmOwnershipTransferConfirmationRepository extends JpaRepository<FirmOwnershipTransferConfirmationEntity, UUID> {

    void deleteByExpiresAtBefore(Instant instant);

    void deleteAllByFirmIdAndRequesterUserId(UUID firmId, UUID requesterUserId);

    Optional<FirmOwnershipTransferConfirmationEntity>
    findFirstByFirmIdAndRequesterUserIdAndNewOwnerUserIdOrderByCreatedAtDesc(
            UUID firmId,
            UUID requesterUserId,
            UUID newOwnerUserId
    );
}
