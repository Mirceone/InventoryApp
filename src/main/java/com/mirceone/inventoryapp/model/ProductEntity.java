package com.mirceone.inventoryapp.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_products_firm_sku", columnNames = {"firm_id", "sku"})
        },
        indexes = {
                @Index(name = "idx_products_firm_id", columnList = "firm_id")
        }
)
public class ProductEntity {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    private UUID id;

    @Column(name = "firm_id", nullable = false)
    private UUID firmId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "sku", length = 128)
    private String sku;

    @Column(name = "current_quantity", nullable = false)
    private int currentQuantity;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProductEntity() {
    }

    public ProductEntity(UUID firmId, String name, String sku, int currentQuantity) {
        this.firmId = firmId;
        this.name = name;
        this.sku = sku;
        this.currentQuantity = currentQuantity;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getFirmId() {
        return firmId;
    }

    public String getName() {
        return name;
    }

    public String getSku() {
        return sku;
    }

    public int getCurrentQuantity() {
        return currentQuantity;
    }

    public void setCurrentQuantity(int currentQuantity) {
        this.currentQuantity = currentQuantity;
    }
}
