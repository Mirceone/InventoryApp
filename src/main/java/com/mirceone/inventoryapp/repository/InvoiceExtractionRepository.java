package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.InvoiceExtractionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceExtractionRepository extends JpaRepository<InvoiceExtractionEntity, UUID> {

    Optional<InvoiceExtractionEntity> findByInvoiceId(UUID invoiceId);

    boolean existsByInvoiceId(UUID invoiceId);

    @Query(value = """
            SELECT * FROM invoice_extractions
            WHERE status = 'PENDING'
            ORDER BY created_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<InvoiceExtractionEntity> lockNextPendingBatch(@Param("limit") int limit);

    @Query(value = """
            SELECT * FROM invoice_extractions
            WHERE id = :id AND status = 'PENDING'
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<InvoiceExtractionEntity> lockPendingById(@Param("id") UUID id);
}
