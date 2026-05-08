package com.scheduley.models;

public class TimeBlock {
    private Long id;
    private String title;
    private String blockType = "CUSTOM";
    private Long courseId;
    private Long taskId;
    private Integer dayOfWeek;
    private String blockDate;
    private int startMinute;
    private int endMinute;
    private String locationText;
    private String colorHex;
    private String notes;
    private String createdAt;
    private String updatedAt;

    public TimeBlock() {
    }

    public TimeBlock(String title, String blockType, Long courseId, Long taskId, Integer dayOfWeek,
                     String blockDate, int startMinute, int endMinute, String locationText, String colorHex,
                     String notes) {
        this(null, title, blockType, courseId, taskId, dayOfWeek, blockDate, startMinute, endMinute,
                locationText, colorHex, notes, null, null);
    }

    public TimeBlock(Long id, String title, String blockType, Long courseId, Long taskId, Integer dayOfWeek,
                     String blockDate, int startMinute, int endMinute, String locationText, String colorHex,
                     String notes, String createdAt, String updatedAt) {
        this.id = id;
        this.title = title;
        this.blockType = blockType;
        this.courseId = courseId;
        this.taskId = taskId;
        this.dayOfWeek = dayOfWeek;
        this.blockDate = blockDate;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBlockType() {
        return blockType;
    }

    public void setBlockType(String blockType) {
        this.blockType = blockType;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getBlockDate() {
        return blockDate;
    }

    public void setBlockDate(String blockDate) {
        this.blockDate = blockDate;
    }

    public int getStartMinute() {
        return startMinute;
    }

    public void setStartMinute(int startMinute) {
        this.startMinute = startMinute;
    }

    public int getEndMinute() {
        return endMinute;
    }

    public void setEndMinute(int endMinute) {
        this.endMinute = endMinute;
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
}
