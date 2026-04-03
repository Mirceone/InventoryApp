package com.mirceone.inventoryapp.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "firm_members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_firm_members", columnNames = {"firm_id", "user_id"})
        }
)
public class FirmMemberEntity {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    private UUID id;

    @Column(name = "firm_id", nullable = false)
    private UUID firmId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private MemberRole role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FirmMemberEntity() {
    }

    public FirmMemberEntity(UUID firmId, UUID userId, MemberRole role) {
        this.firmId = firmId;
        this.userId = userId;
        this.role = role;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getFirmId() {
        return firmId;
    }

    public UUID getUserId() {
        return userId;
    }

    public MemberRole getRole() {
        return role;
    }
}
