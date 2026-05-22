package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {
    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    List<PasswordResetTokenEntity> findAllByUserIdAndUsedAtIsNull(UUID userId);

    void deleteByExpiresAtBefore(Instant instant);
}
