package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.ProductEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    long countByFirmId(UUID firmId);

    @Query("""
            SELECT DISTINCT p FROM ProductEntity p JOIN FETCH p.category
            WHERE p.firmId = :firmId
            """)
    List<ProductEntity> findAllByFirmId(@Param("firmId") UUID firmId);

    @Query("""
            SELECT p FROM ProductEntity p JOIN FETCH p.category
            WHERE p.id = :productId AND p.firmId = :firmId
            """)
    Optional<ProductEntity> findByIdAndFirmId(@Param("productId") UUID productId, @Param("firmId") UUID firmId);

    /**
     * Locks the product row (SELECT ... FOR UPDATE) for safe read-modify-write of stock quantity,
     * preventing lost updates under concurrent stock changes. Must be called inside a transaction.
     * No JOIN FETCH so only the product row is locked; the lazy category loads separately.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductEntity p WHERE p.id = :productId AND p.firmId = :firmId")
    Optional<ProductEntity> findByIdAndFirmIdForUpdate(@Param("productId") UUID productId, @Param("firmId") UUID firmId);

    @Query("""
            SELECT DISTINCT p FROM ProductEntity p JOIN FETCH p.category
            WHERE p.firmId = :firmId
            AND p.reorderEnabled = true
            AND p.currentQuantity < COALESCE(p.reorderThreshold, :defaultThreshold)
            """)
    List<ProductEntity> findNeedingRestock(@Param("firmId") UUID firmId, @Param("defaultThreshold") int defaultThreshold);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ProductEntity p
            SET p.category = :targetCategory
            WHERE p.firmId = :firmId AND p.category = :sourceCategory
            """)
    int moveProductsToCategory(
            @Param("firmId") UUID firmId,
            @Param("sourceCategory") com.mirceone.inventoryapp.model.CategoryEntity sourceCategory,
            @Param("targetCategory") com.mirceone.inventoryapp.model.CategoryEntity targetCategory
    );
}
