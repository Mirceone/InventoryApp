package com.mirceone.inventoryapp.repository;

import com.mirceone.inventoryapp.model.DocumentProcessingStatus;
import com.mirceone.inventoryapp.model.FirmDocumentEntity;
import com.mirceone.inventoryapp.service.documents.DocumentSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FirmDocumentRepository extends JpaRepository<FirmDocumentEntity, UUID> {

    @Query(value = """
            SELECT new com.mirceone.inventoryapp.service.documents.DocumentSummary(
                d.id, d.firmId, d.dossierId, d.originalFilename, d.mimeType, d.sizeBytes, d.createdAt,
                d.uploadedByUserId, u.email, d.folderPath, d.processingStatus, d.organizationSource)
            FROM FirmDocumentEntity d
            JOIN UserEntity u ON u.id = d.uploadedByUserId
            WHERE d.dossierId = :dossierId
            AND (:folder IS NULL OR :folder = '' OR d.folderPath = :folder)
            ORDER BY d.createdAt DESC
            """,
            countQuery = """
            SELECT count(d) FROM FirmDocumentEntity d
            WHERE d.dossierId = :dossierId
            AND (:folder IS NULL OR :folder = '' OR d.folderPath = :folder)
            """)
    Page<DocumentSummary> findSummariesByDossierId(
            @Param("dossierId") UUID dossierId,
            @Param("folder") String folder,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT COALESCE(d.folderPath, '')
            FROM FirmDocumentEntity d
            WHERE d.dossierId = :dossierId
            AND d.processingStatus = com.mirceone.inventoryapp.model.DocumentProcessingStatus.CLASSIFIED
            AND d.folderPath IS NOT NULL AND d.folderPath <> ''
            ORDER BY COALESCE(d.folderPath, '')
            """)
    List<String> findDistinctFolderPathsByDossierId(@Param("dossierId") UUID dossierId);

    List<FirmDocumentEntity> findByProcessingStatusOrderByCreatedAtAsc(
            DocumentProcessingStatus status,
            Pageable pageable
    );

    List<FirmDocumentEntity> findAllByDossierId(UUID dossierId);

    List<FirmDocumentEntity> findAllByFirmId(UUID firmId);

    long countByDossierId(UUID dossierId);

    Optional<FirmDocumentEntity> findByIdAndDossierId(UUID id, UUID dossierId);

    Optional<FirmDocumentEntity> findByIdAndFirmIdAndDossierId(UUID id, UUID firmId, UUID dossierId);

    @Query("""
            SELECT d.folderPath, count(d)
            FROM FirmDocumentEntity d
            WHERE d.dossierId = :dossierId
            AND d.processingStatus = com.mirceone.inventoryapp.model.DocumentProcessingStatus.CLASSIFIED
            AND d.folderPath IS NOT NULL AND d.folderPath <> ''
            GROUP BY d.folderPath
            """)
    List<Object[]> countClassifiedDocumentsByFolderPath(@Param("dossierId") UUID dossierId);
}
