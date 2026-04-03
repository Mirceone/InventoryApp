package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.FirmMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FirmMemberRepository extends JpaRepository<FirmMemberEntity, UUID> {

    boolean existsByFirmIdAndUserId(UUID firmId, UUID userId);

    List<FirmMemberEntity> findAllByUserId(UUID userId);
}
