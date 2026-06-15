package com.mirceone.inventoryapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_recipient_created_at", columnList = "recipient_user_id, created_at DESC"),
                @Index(name = "idx_notifications_recipient_read_at", columnList = "recipient_user_id, read_at, created_at DESC")
        }
)
public class NotificationEntity {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    private UUID id;

    @Column(name = "firm_id", nullable = false)
    private UUID firmId;

    @Column(name = "recipient_user_id", nullable = false)
    private UUID recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 32)
    private NotificationLevel level;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "body", nullable = false, length = 1024)
    private String body;

    @Column(name = "metadata_json", columnDefinition = "text")
    private String metadataJson;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NotificationEntity() {
    }

    public NotificationEntity(
            UUID firmId,
            UUID recipientUserId,
            NotificationType type,
            NotificationLevel level,
            String title,
            String body,
            String metadataJson
    ) {
        this.firmId = firmId;
        this.recipientUserId = recipientUserId;
        this.type = type;
        this.level = level;
        this.title = title;
        this.body = body;
        this.metadataJson = metadataJson;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getFirmId() {
        return firmId;
    }

    public UUID getRecipientUserId() {
        return recipientUserId;
    }

    public NotificationType getType() {
        return type;
    }

    public NotificationLevel getLevel() {
        return level;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }
}
