package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    Page<NotificationEntity> findAllByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId, Pageable pageable);

    Page<NotificationEntity> findAllByRecipientUserIdAndFirmIdOrderByCreatedAtDesc(
            UUID recipientUserId,
            UUID firmId,
            Pageable pageable
    );

    Page<NotificationEntity> findAllByRecipientUserIdAndReadAtIsNullOrderByCreatedAtDesc(UUID recipientUserId, Pageable pageable);

    long countByRecipientUserIdAndFirmIdAndReadAtIsNull(UUID recipientUserId, UUID firmId);

    long countByRecipientUserIdAndReadAtIsNull(UUID recipientUserId);

    Optional<NotificationEntity> findByIdAndRecipientUserId(UUID id, UUID recipientUserId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE NotificationEntity n
            SET n.readAt = :readAt
            WHERE n.recipientUserId = :recipientUserId AND n.readAt IS NULL
            """)
    int markAllAsRead(@Param("recipientUserId") UUID recipientUserId, @Param("readAt") Instant readAt);
}
