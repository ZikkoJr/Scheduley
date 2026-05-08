package com.scheduley.models;

public class Course {
    private Long id;
    private String code;
    private String name;
    private String instructor;
    private String locationText;
    private String colorHex;
    private String notes;
    private String createdAt;
    private String updatedAt;

    public Course() {
    }

    public Course(String code, String name, String instructor, String locationText, String colorHex, String notes) {
        this(null, code, name, instructor, locationText, colorHex, notes, null, null);
    }

    public Course(Long id, String code, String name, String instructor, String locationText, String colorHex,
                  String notes, String createdAt, String updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.instructor = instructor;
        this.locationText = locationText;
        this.colorHex = colorHex;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }

    public String getLocationText() {
        return locationText;
    }

    public void setLocationText(String locationText) {
        this.locationText = locationText;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public String displayName() {
        if (code == null || code.isBlank()) {
            return name == null ? "" : name;
        }
        if (name == null || name.isBlank()) {
            return code;
        }
        return code + " - " + name;
    }

    @Override
    public String toString() {
        return displayName();
    }
}
