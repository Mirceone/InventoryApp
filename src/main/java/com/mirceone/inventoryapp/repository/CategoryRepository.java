package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    Optional<CategoryEntity> findByFirmIdAndName(UUID firmId, String name);

    Optional<CategoryEntity> findByIdAndFirmId(UUID id, UUID firmId);

    List<CategoryEntity> findAllByFirmIdOrderByNameAsc(UUID firmId);

    void deleteByIdAndFirmId(UUID id, UUID firmId);
}
