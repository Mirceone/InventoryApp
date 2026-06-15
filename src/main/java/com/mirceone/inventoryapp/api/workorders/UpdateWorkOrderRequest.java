package com.mirceone.inventoryapp.api.workorders;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Partial update: only non-null fields are applied, except {@code description} which clears when explicitly set to null.
 */
public class UpdateWorkOrderRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String clientName;

    @Size(max = 255)
    private String location;

    @Size(max = 2000)
    private String description;

    private LocalDate estimatedEndDate;

    private boolean descriptionPresent;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
        this.descriptionPresent = true;
    }

    public LocalDate getEstimatedEndDate() {
        return estimatedEndDate;
    }

    public void setEstimatedEndDate(LocalDate estimatedEndDate) {
        this.estimatedEndDate = estimatedEndDate;
    }

    @JsonIgnore
    public boolean isDescriptionPresent() {
        return descriptionPresent;
    }
}
