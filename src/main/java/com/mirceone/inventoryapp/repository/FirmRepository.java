package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.FirmEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FirmRepository extends JpaRepository<FirmEntity, UUID> {
    List<FirmEntity> findAllByIdIn(Collection<UUID> ids);
}
