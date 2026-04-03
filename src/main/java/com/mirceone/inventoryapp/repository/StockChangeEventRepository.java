package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.StockChangeEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StockChangeEventRepository extends JpaRepository<StockChangeEventEntity, UUID> {
}
