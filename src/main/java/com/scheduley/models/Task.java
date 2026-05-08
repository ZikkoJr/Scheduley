package com.scheduley.models;

public class Task {
    private Long id;
    private String title;
    private Long courseId;
    private String dueDate;
    private Integer estimatedMinutes;
    private String priority = "MEDIUM";
    private String status = "NOT_STARTED";
    private String notes;
    private String createdAt;
    private String updatedAt;

    public Task() {
    }

    public Task(String title, Long courseId, String dueDate, Integer estimatedMinutes, String priority,
                String status, String notes) {
        this(null, title, courseId, dueDate, estimatedMinutes, priority, status, notes, null, null);
    }

    public Task(Long id, String title, Long courseId, String dueDate, Integer estimatedMinutes, String priority,
                String status, String notes, String createdAt, String updatedAt) {
        this.id = id;
        this.title = title;
        this.courseId = courseId;
        this.dueDate = dueDate;
        this.estimatedMinutes = estimatedMinutes;
        this.priority = priority;
        this.status = status;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(Integer estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    @Override
    public String toString() {
        return title == null ? "" : title;
    }
}
