package com.scheduley.service;

import com.scheduley.models.ScheduleProfile;

public record ScheduleImportResult(
        ScheduleProfile scheduleProfile,
        int courseCount,
        int taskCount,
        int timeBlockCount
) {
}
