package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.OpsEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OpsEventRepository extends JpaRepository<OpsEventEntity, UUID> {
}
