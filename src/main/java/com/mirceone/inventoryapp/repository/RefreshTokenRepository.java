package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    long countByUserIdAndRevokedAtIsNullAndExpiresAtAfter(UUID userId, Instant now);

    List<RefreshTokenEntity> findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtAsc(UUID userId, Instant now);

    List<RefreshTokenEntity> findAllByUserIdAndRevokedAtIsNull(UUID userId);

    void deleteByExpiresAtBefore(Instant now);
}
