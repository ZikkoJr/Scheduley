package com.scheduley.models;

public class ScheduleProfile {
    private Long id;
    private String name;
    private String description;
    private boolean active;
    private String createdAt;
    private String updatedAt;

    public ScheduleProfile() {
    }

    public ScheduleProfile(String name, String description) {
        this(null, name, description, false, null, null);
    }

    public ScheduleProfile(Long id, String name, String description, boolean active, String createdAt, String updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return name == null ? "" : name;
    }
}
