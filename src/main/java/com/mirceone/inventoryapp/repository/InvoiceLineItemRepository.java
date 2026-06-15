package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.InvoiceLineItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItemEntity, UUID> {

    List<InvoiceLineItemEntity> findByExtractionIdOrderByLineNoAsc(UUID extractionId);

    @Modifying
    @Query("DELETE FROM InvoiceLineItemEntity li WHERE li.extractionId = :extractionId")
    void deleteByExtractionId(@Param("extractionId") UUID extractionId);
}
