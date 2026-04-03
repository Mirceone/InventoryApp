package com.mirceone.inventoryapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_change_events")
public class StockChangeEventEntity {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    private UUID id;

    @Column(name = "firm_id", nullable = false)
    private UUID firmId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 16)
    private StockChangeType changeType;

    @Column(name = "previous_quantity", nullable = false)
    private int previousQuantity;

    @Column(name = "new_quantity", nullable = false)
    private int newQuantity;

    @Column(name = "delta", nullable = false)
    private int delta;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StockChangeEventEntity() {
    }

    public StockChangeEventEntity(
            UUID firmId,
            UUID productId,
            UUID actorUserId,
            StockChangeType changeType,
            int previousQuantity,
            int newQuantity,
            int delta
    ) {
        this.firmId = firmId;
        this.productId = productId;
        this.actorUserId = actorUserId;
        this.changeType = changeType;
        this.previousQuantity = previousQuantity;
        this.newQuantity = newQuantity;
        this.delta = delta;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
