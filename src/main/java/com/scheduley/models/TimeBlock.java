package com.scheduley.models;

public class TimeBlock {
    private Long id, courseId;
    private String title;
    private BlockCategory category; //enum
    private Long getCourseId;
    private int dayOfWeek;
    private int startMin;
    private int endMin;
    private String notes;

    // Constructor for initial creation (no id)
    public TimeBlock(String title, BlockCategory category, Long getCourseId,
                     int dayOfWeek, int startMin, int endMin, String notes) {
        this.title = title;
        this.category = category;
        this.getCourseId = getCourseId;
        this.dayOfWeek = dayOfWeek;
        this.startMin = startMin;
        this.endMin = endMin;
        this.notes = notes;
    }

    // Constructor for existing Timeblock entity
    public TimeBlock(Long id,String title, BlockCategory category, Long getCourseId,
                     int dayOfWeek, int startMin, int endMin, String notes){
        this(title,category,getCourseId,dayOfWeek,startMin,endMin,notes);
        this.id = id;
    }

    // Getters and Setters
    public  Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Long getCourseId() {
        return courseId;
    }
    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public BlockCategory getCategory() {
        return category;
    }
    public void setCategory(BlockCategory category) {
        this.category = category;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }
    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public int getStartMin() {
        return startMin;
    }
    public void setStartMin(int startMin) {
        this.startMin = startMin;
    }

    public int getEndMin() {
        return endMin;
    }
    public void setEndMin(int endMin) {
        this.endMin = endMin;
    }

    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }

}
