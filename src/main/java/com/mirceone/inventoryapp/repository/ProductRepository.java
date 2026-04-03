package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    List<ProductEntity> findAllByFirmId(UUID firmId);

    Optional<ProductEntity> findByIdAndFirmId(UUID productId, UUID firmId);
}
