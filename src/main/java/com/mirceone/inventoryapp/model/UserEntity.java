package com.mirceone.inventoryapp.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = {"email"}),
                @UniqueConstraint(name = "uk_users_provider_sub", columnNames = {"provider", "provider_sub"})
        }
)
public class UserEntity {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    private UUID id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    private ProviderType provider;

    @Column(name = "provider_sub", nullable = false, length = 255)
    private String providerSub;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserEntity() {
    }

    public UserEntity(String email,
                       String passwordHash,
                       ProviderType provider,
                       String providerSub,
                       String displayName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.provider = provider;
        this.providerSub = providerSub;
        this.displayName = displayName;
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

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public ProviderType getProvider() {
        return provider;
    }

    public String getProviderSub() {
        return providerSub;
    }

    public String getDisplayName() {
        return displayName;
    }
}
