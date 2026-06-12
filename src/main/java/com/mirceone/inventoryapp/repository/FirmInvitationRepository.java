package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.FirmInvitationEntity;
import com.mirceone.inventoryapp.model.FirmInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FirmInvitationRepository extends JpaRepository<FirmInvitationEntity, UUID> {

    Optional<FirmInvitationEntity> findByTokenHash(String tokenHash);

    Optional<FirmInvitationEntity> findByFirmIdAndEmailAndStatus(UUID firmId, String email, FirmInvitationStatus status);

    List<FirmInvitationEntity> findAllByFirmIdAndStatusOrderByCreatedAtDesc(UUID firmId, FirmInvitationStatus status);

    List<FirmInvitationEntity> findAllByFirmIdAndStatusAndExpiresAtGreaterThanEqualOrderByCreatedAtDesc(
            UUID firmId,
            FirmInvitationStatus status,
            Instant expiresAt
    );

    long countByFirmIdAndStatusAndExpiresAtGreaterThanEqual(
            UUID firmId,
            FirmInvitationStatus status,
            Instant expiresAt
    );

    List<FirmInvitationEntity> findAllByStatusAndExpiresAtBefore(
            FirmInvitationStatus status,
            Instant expiresAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update FirmInvitationEntity invitation
            set invitation.status = :expiredStatus
            where invitation.status = :pendingStatus
              and invitation.expiresAt < :now
            """)
    int expirePendingInvitations(
            @Param("pendingStatus") FirmInvitationStatus pendingStatus,
            @Param("expiredStatus") FirmInvitationStatus expiredStatus,
            @Param("now") Instant now
    );
}
