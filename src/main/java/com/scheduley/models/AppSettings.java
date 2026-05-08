package com.scheduley.models;

public class AppSettings {
    private int id = 1;
    private int weekStartDay = 1;
    private int calendarStartHour = 7;
    private int calendarEndHour = 23;
    private String theme = "LIGHT";

    public AppSettings() {
    }

    public AppSettings(int id, int weekStartDay, int calendarStartHour, int calendarEndHour, String theme) {
        this.id = id;
        this.weekStartDay = weekStartDay;
        this.calendarStartHour = calendarStartHour;
        this.calendarEndHour = calendarEndHour;
        this.theme = theme;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getWeekStartDay() {
        return weekStartDay;
    }

    public void setWeekStartDay(int weekStartDay) {
        this.weekStartDay = weekStartDay;
    }

    public int getCalendarStartHour() {
        return calendarStartHour;
    }

    public void setCalendarStartHour(int calendarStartHour) {
        this.calendarStartHour = calendarStartHour;
    }

    public int getCalendarEndHour() {
        return calendarEndHour;
    }

    public void setCalendarEndHour(int calendarEndHour) {
        this.calendarEndHour = calendarEndHour;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }
}
