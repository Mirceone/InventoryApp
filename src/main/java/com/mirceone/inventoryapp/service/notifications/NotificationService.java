package com.mirceone.inventoryapp.service.notifications;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirceone.inventoryapp.model.FirmEntity;
import com.mirceone.inventoryapp.model.FirmMemberEntity;
import com.mirceone.inventoryapp.model.FirmStatus;
import com.mirceone.inventoryapp.model.FirmStatusChangeSource;
import com.mirceone.inventoryapp.model.NotificationEntity;
import com.mirceone.inventoryapp.model.NotificationLevel;
import com.mirceone.inventoryapp.model.NotificationType;
import com.mirceone.inventoryapp.model.UserEntity;
import com.mirceone.inventoryapp.repository.FirmMemberRepository;
import com.mirceone.inventoryapp.repository.FirmRepository;
import com.mirceone.inventoryapp.repository.NotificationRepository;
import com.mirceone.inventoryapp.repository.UserRepository;
import com.mirceone.inventoryapp.service.email.EmailService;
import com.mirceone.inventoryapp.service.firms.access.FirmStatusCatalog;
import com.mirceone.inventoryapp.service.support.AfterCommitExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_TEXT_LIMIT = 255;
    private static final int MAX_BODY_LIMIT = 1024;
    private static final TypeReference<Map<String, String>> METADATA_MAP_TYPE = new TypeReference<>() {
    };

    private final NotificationRepository notificationRepository;
    private final FirmMemberRepository firmMemberRepository;
    private final FirmRepository firmRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final AfterCommitExecutor afterCommitExecutor;
    private final TransactionTemplate requiresNewTransactionTemplate;

    public NotificationService(
            NotificationRepository notificationRepository,
            FirmMemberRepository firmMemberRepository,
            FirmRepository firmRepository,
            UserRepository userRepository,
            EmailService emailService,
            ObjectMapper objectMapper,
            AfterCommitExecutor afterCommitExecutor,
            PlatformTransactionManager transactionManager
    ) {
        this.notificationRepository = notificationRepository;
        this.firmMemberRepository = firmMemberRepository;
        this.firmRepository = firmRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
        this.afterCommitExecutor = afterCommitExecutor;
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional(readOnly = true)
    public NotificationContracts.NotificationInbox listNotifications(UUID userId, boolean unreadOnly, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        var pageable = PageRequest.of(0, safeLimit);
        List<NotificationEntity> items = unreadOnly
                ? notificationRepository.findAllByRecipientUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, pageable).getContent()
                : notificationRepository.findAllByRecipientUserIdOrderByCreatedAtDesc(userId, pageable).getContent();
        long unreadCount = notificationRepository.countByRecipientUserIdAndReadAtIsNull(userId);
        return new NotificationContracts.NotificationInbox(
                unreadCount,
                items.stream().map(this::toSummary).toList()
        );
    }

    @Transactional(readOnly = true)
    public NotificationContracts.NotificationInbox listNotificationsForFirm(UUID userId, UUID firmId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        var pageable = PageRequest.of(0, safeLimit);
        List<NotificationEntity> items = notificationRepository
                .findAllByRecipientUserIdAndFirmIdOrderByCreatedAtDesc(userId, firmId, pageable)
                .getContent();
        long unreadCount = notificationRepository.countByRecipientUserIdAndFirmIdAndReadAtIsNull(userId, firmId);
        return new NotificationContracts.NotificationInbox(
                unreadCount,
                items.stream().map(this::toSummary).toList()
        );
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        NotificationEntity entity = notificationRepository.findByIdAndRecipientUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Notification not found"));
        if (entity.getReadAt() == null) {
            entity.setReadAt(Instant.now());
            notificationRepository.save(entity);
        }
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllAsRead(userId, Instant.now());
    }

    public void notifyProductCreatedAfterCommit(
            UUID firmId,
            UUID productId,
            String productName,
            String sku,
            int initialQuantity
    ) {
        runAfterCommit("product-created", () -> publishProductCreated(firmId, productId, productName, sku, initialQuantity));
    }

    public void notifyProductLowStockAfterCommit(
            UUID firmId,
            UUID productId,
            String productName,
            String sku,
            int currentQuantity,
            int threshold
    ) {
        runAfterCommit("product-low-stock", () -> publishProductLowStock(
                firmId,
                productId,
                productName,
                sku,
                currentQuantity,
                threshold
        ));
    }

    public void notifyFirmStatusChangedAfterCommit(
            UUID firmId,
            FirmStatus previousStatus,
            FirmStatus newStatus,
            String message,
            FirmStatusChangeSource source
    ) {
        runAfterCommit("firm-status-changed", () -> publishFirmStatusChanged(firmId, previousStatus, newStatus, message, source));
    }

    private void publishProductCreated(
            UUID firmId,
            UUID productId,
            String productName,
            String sku,
            int initialQuantity
    ) {
        runInNewTransaction(() -> {
            FirmEntity firm = requireFirm(firmId);
            Map<String, String> metadata = metadata(
                    "event", "product_created",
                    "productId", productId.toString(),
                    "productName", productName,
                    "sku", sku,
                    "initialQuantity", Integer.toString(initialQuantity)
            );
            publishToFirmMembers(
                    firm,
                    new NotificationMessage(
                            NotificationType.PRODUCT_CREATED,
                            NotificationLevel.INFO,
                            "Produs adaugat",
                            "Produsul \"%s\" a fost adaugat in inventarul firmei %s cu stoc initial %d."
                                    .formatted(productName, firm.getName(), initialQuantity),
                            metadata
                    )
            );
        });
    }

    private void publishProductLowStock(
            UUID firmId,
            UUID productId,
            String productName,
            String sku,
            int currentQuantity,
            int threshold
    ) {
        runInNewTransaction(() -> {
            FirmEntity firm = requireFirm(firmId);
            Map<String, String> metadata = metadata(
                    "event", "product_low_stock",
                    "productId", productId.toString(),
                    "productName", productName,
                    "sku", sku,
                    "currentQuantity", Integer.toString(currentQuantity),
                    "effectiveMinThreshold", Integer.toString(threshold),
                    "buyListVisible", "true"
            );
            publishToFirmMembers(
                    firm,
                    new NotificationMessage(
                            NotificationType.PRODUCT_LOW_STOCK,
                            NotificationLevel.WARNING,
                            "Produs sub prag minim",
                            "Produsul \"%s\" a scazut sub pragul minim (%d/%d) si este vizibil in buy list pentru firma %s."
                                    .formatted(productName, currentQuantity, threshold, firm.getName()),
                            metadata
                    )
            );
        });
    }

    private void publishFirmStatusChanged(
            UUID firmId,
            FirmStatus previousStatus,
            FirmStatus newStatus,
            String message,
            FirmStatusChangeSource source
    ) {
        String newLabel = FirmStatusCatalog.displayLabel(newStatus);
        FirmEntity firm = requireFirm(firmId);
        runInNewTransaction(() -> {
            String previousLabel = previousStatus == null ? "Unknown" : FirmStatusCatalog.displayLabel(previousStatus);
            String sourceLabel = source == FirmStatusChangeSource.SYSTEM ? "automat" : "manual";
            String body = "Statusul firmei %s a fost actualizat %s din %s in %s."
                    .formatted(firm.getName(), sourceLabel, previousLabel, newLabel);
            if (message != null && !message.isBlank()) {
                body += " Detalii: " + message.strip();
            }

            Map<String, String> metadata = metadata(
                    "event", "firm_status_changed",
                    "previousStatus", previousStatus == null ? null : previousStatus.name(),
                    "newStatus", newStatus.name(),
                    "source", source.name(),
                    "message", message
            );

            publishToFirmMembers(
                    firm,
                    new NotificationMessage(
                            NotificationType.FIRM_STATUS_CHANGED,
                            levelForStatus(newStatus),
                            newStatus == FirmStatus.CRITICAL ? "Firma marcata critic" : "Status firma actualizat",
                            body,
                            metadata
                    )
            );
        });
        if (newStatus == FirmStatus.CRITICAL) {
            sendCriticalStatusEmail(firm, newLabel, message);
        }
    }

    private void publishToFirmMembers(FirmEntity firm, NotificationMessage message) {
        List<FirmMemberEntity> members = firmMemberRepository.findAllByFirmIdOrderByCreatedAtAsc(firm.getId());
        if (members.isEmpty()) {
            log.warn("Skipped notification publish because firm has no members firmId={} type={}", firm.getId(), message.type());
            return;
        }

        String metadataJson = serializeMetadata(message.metadata());
        List<NotificationEntity> rows = new ArrayList<>(members.size());
        String title = normalizeRequired(message.title(), MAX_TEXT_LIMIT);
        String body = normalizeRequired(message.body(), MAX_BODY_LIMIT);
        for (FirmMemberEntity member : members) {
            rows.add(new NotificationEntity(
                    firm.getId(),
                    member.getUserId(),
                    message.type(),
                    message.level(),
                    title,
                    body,
                    metadataJson
            ));
        }
        notificationRepository.saveAll(rows);
    }

    private void sendCriticalStatusEmail(FirmEntity firm, String statusDisplayLabel, String message) {
        Optional<UserEntity> owner = userRepository.findById(firm.getOwnerUserId());
        if (owner.isEmpty()) {
            log.warn("Skipped critical status email because owner was not found firmId={} ownerUserId={}",
                    firm.getId(), firm.getOwnerUserId());
            return;
        }
        emailService.sendCriticalFirmStatusEmail(
                owner.get().getEmail(),
                firm.getName(),
                statusDisplayLabel,
                message
        );
    }

    private void runAfterCommit(String actionName, Runnable action) {
        afterCommitExecutor.executeQuietly("notification-" + actionName, log, action);
    }

    private void runInNewTransaction(Runnable action) {
        requiresNewTransactionTemplate.executeWithoutResult(status -> action.run());
    }

    private NotificationContracts.NotificationSummary toSummary(NotificationEntity entity) {
        return new NotificationContracts.NotificationSummary(
                entity.getId(),
                entity.getFirmId(),
                entity.getType(),
                entity.getLevel(),
                entity.getTitle(),
                entity.getBody(),
                deserializeMetadata(entity.getMetadataJson()),
                entity.getReadAt() != null,
                entity.getReadAt(),
                entity.getCreatedAt()
        );
    }

    private Map<String, String> metadata(String... keyValues) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            String key = keyValues[i];
            String value = keyValues[i + 1];
            if (key == null || key.isBlank() || value == null || value.isBlank()) {
                continue;
            }
            result.put(key.strip(), value.strip());
        }
        return Map.copyOf(result);
    }

    private String serializeMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Unable to serialize notification metadata");
        }
    }

    private Map<String, String> deserializeMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadataJson, METADATA_MAP_TYPE);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize notification metadata: {}", e.getMessage());
            return Map.of();
        }
    }

    private FirmEntity requireFirm(UUID firmId) {
        return firmRepository.findById(firmId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Firm not found"));
    }

    private static NotificationLevel levelForStatus(FirmStatus status) {
        return switch (status) {
            case ACTIVE -> NotificationLevel.INFO;
            case PAUSED -> NotificationLevel.WARNING;
            case CRITICAL -> NotificationLevel.CRITICAL;
        };
    }

    private static String normalizeRequired(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Notification text cannot be blank");
        }
        String trimmed = value.strip();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }

    private record NotificationMessage(
            NotificationType type,
            NotificationLevel level,
            String title,
            String body,
            Map<String, String> metadata
    ) {
    }
}
