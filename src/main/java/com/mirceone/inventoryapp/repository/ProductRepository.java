package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    List<ProductEntity> findAllByFirmId(UUID firmId);

    Optional<ProductEntity> findByIdAndFirmId(UUID productId, UUID firmId);

    @Query("""
            SELECT p FROM ProductEntity p
            WHERE p.firmId = :firmId
            AND p.reorderEnabled = true
            AND p.currentQuantity < COALESCE(p.reorderThreshold, :defaultThreshold)
            """)
    List<ProductEntity> findNeedingRestock(@Param("firmId") UUID firmId, @Param("defaultThreshold") int defaultThreshold);
}
