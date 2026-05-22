package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.RouteStopEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RouteStopRepository extends JpaRepository<RouteStopEntity, UUID> {

    List<RouteStopEntity> findAllByFirmIdOrderBySortOrderAscNameAsc(UUID firmId);

    Optional<RouteStopEntity> findByIdAndFirmId(UUID id, UUID firmId);

    List<RouteStopEntity> findAllByFirmIdAndIdIn(UUID firmId, Collection<UUID> ids);
}
