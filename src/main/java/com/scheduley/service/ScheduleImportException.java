package com.scheduley.service;

public class ScheduleImportException extends RuntimeException {
    public ScheduleImportException(String message) {
        super(message);
    }

    public ScheduleImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
