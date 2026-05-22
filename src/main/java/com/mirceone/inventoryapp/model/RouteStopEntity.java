package com.mirceone.inventoryapp.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "route_stops", indexes = @Index(name = "idx_route_stops_firm_id", columnList = "firm_id"))
public class RouteStopEntity {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    private UUID id;

    @Column(name = "firm_id", nullable = false)
    private UUID firmId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(length = 512)
    private String address;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RouteStopEntity() {
    }

    public RouteStopEntity(UUID firmId, String name, double latitude, double longitude, String address, int sortOrder) {
        this.firmId = firmId;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.sortOrder = sortOrder;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
