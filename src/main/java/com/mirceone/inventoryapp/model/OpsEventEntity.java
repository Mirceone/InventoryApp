package com.mirceone.inventoryapp.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ops_events")
public class OpsEventEntity {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    private UUID id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "prompt_excerpt", columnDefinition = "text")
    private String promptExcerpt;

    @Column(name = "response_excerpt", columnDefinition = "text")
    private String responseExcerpt;

    @Column(length = 255)
    private String model;

    protected OpsEventEntity() {
    }

    public OpsEventEntity(String promptExcerpt, String responseExcerpt, String model) {
        this.promptExcerpt = promptExcerpt;
        this.responseExcerpt = responseExcerpt;
        this.model = model;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }
}
