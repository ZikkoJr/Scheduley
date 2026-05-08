package com.scheduley.util;

import java.time.LocalTime;

public final class TimeUtils {
    private TimeUtils() {
    }

    public static int parseTimeToMinute(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Time is required.");
        }
        String value = text.trim().toUpperCase();
        boolean pm = value.endsWith("PM");
        boolean am = value.endsWith("AM");
        value = value.replace("AM", "").replace("PM", "").trim();

        String[] parts = value.split(":");
        int hour = Integer.parseInt(parts[0].trim());
        int minute = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;

        if (am || pm) {
            if (hour < 1 || hour > 12) {
                throw new IllegalArgumentException("12-hour times must use hours 1-12.");
            }
            if (hour == 12) {
                hour = 0;
            }
            if (pm) {
                hour += 12;
            }
        }

        int result = hour * 60 + minute;
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59 || result < 0 || result > 1439) {
            throw new IllegalArgumentException("Time must be between 00:00 and 23:59.");
        }
        return result;
    }

    public static String formatMinute(int minuteOfDay) {
        int hour = minuteOfDay / 60;
        int minute = minuteOfDay % 60;
        String suffix = hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0) {
            displayHour = 12;
        }
        return String.format("%d:%02d %s", displayHour, minute, suffix);
    }

    public static String dayName(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "Monday";
            case 2 -> "Tuesday";
            case 3 -> "Wednesday";
            case 4 -> "Thursday";
            case 5 -> "Friday";
            case 6 -> "Saturday";
            case 7 -> "Sunday";
            default -> "";
        };
    }

    public static int minuteOfDay(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }

    public static int dayNumber(String dayName) {
        return switch (dayName) {
            case "Monday" -> 1;
            case "Tuesday" -> 2;
            case "Wednesday" -> 3;
            case "Thursday" -> 4;
            case "Friday" -> 5;
            case "Saturday" -> 6;
            case "Sunday" -> 7;
            default -> throw new IllegalArgumentException("Choose a day of the week.");
        };
    }
}
