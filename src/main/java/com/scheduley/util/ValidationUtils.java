package com.scheduley.util;

import com.scheduley.models.AppSettings;
import com.scheduley.models.Course;
import com.scheduley.models.Task;
import com.scheduley.models.TimeBlock;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class ValidationUtils {
    private static final List<String> BLOCK_TYPES = List.of("COURSE", "WORK", "STUDY", "TASK", "CUSTOM");
    private static final List<String> PRIORITIES = List.of("LOW", "MEDIUM", "HIGH");
    private static final List<String> STATUSES = List.of("NOT_STARTED", "IN_PROGRESS", "COMPLETE");

    private ValidationUtils() {
    }

    public static void validateCourse(Course course) {
        requireText(course.getCode(), "Course code is required.");
        requireText(course.getName(), "Course name is required.");
        validateOptionalHex(course.getColorHex());
    }

    public static void validateTask(Task task) {
        requireText(task.getTitle(), "Task title is required.");
        if (task.getEstimatedMinutes() != null && task.getEstimatedMinutes() <= 0) {
            throw new IllegalArgumentException("Estimated minutes must be positive.");
        }
        if (!PRIORITIES.contains(task.getPriority())) {
            throw new IllegalArgumentException("Priority must be LOW, MEDIUM, or HIGH.");
        }
        if (!STATUSES.contains(task.getStatus())) {
            throw new IllegalArgumentException("Status must be NOT_STARTED, IN_PROGRESS, or COMPLETE.");
        }
        validateOptionalDate(task.getDueDate());
    }

    public static void validateTimeBlock(TimeBlock block) {
        requireText(block.getTitle(), "Time block title is required.");
        if (!BLOCK_TYPES.contains(block.getBlockType())) {
            throw new IllegalArgumentException("Type must be COURSE, WORK, STUDY, TASK, or CUSTOM.");
        }
        if (block.getDayOfWeek() == null || block.getDayOfWeek() < 1 || block.getDayOfWeek() > 7) {
            throw new IllegalArgumentException("Choose a day of the week.");
        }
        if (block.getStartMinute() < 0 || block.getStartMinute() >= 1440) {
            throw new IllegalArgumentException("Start time must be within the day.");
        }
        if (block.getEndMinute() <= 0 || block.getEndMinute() > 1440) {
            throw new IllegalArgumentException("End time must be within the day.");
        }
        if (block.getEndMinute() <= block.getStartMinute()) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        validateOptionalHex(block.getColorHex());
    }

    public static void validateSettings(AppSettings settings) {
        if (settings.getWeekStartDay() < 1 || settings.getWeekStartDay() > 7) {
            throw new IllegalArgumentException("Week start day must be 1-7.");
        }
        if (settings.getCalendarStartHour() < 0 || settings.getCalendarStartHour() > 23) {
            throw new IllegalArgumentException("Calendar start hour must be 0-23.");
        }
        if (settings.getCalendarEndHour() < 1 || settings.getCalendarEndHour() > 24) {
            throw new IllegalArgumentException("Calendar end hour must be 1-24.");
        }
        if (settings.getCalendarEndHour() <= settings.getCalendarStartHour()) {
            throw new IllegalArgumentException("Calendar end hour must be after start hour.");
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void validateOptionalHex(String value) {
        if (value != null && !value.isBlank() && !value.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("Color must be a hex value like #4F46E5.");
        }
    }

    private static void validateOptionalDate(String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Due date must use YYYY-MM-DD.");
        }
    }
}
