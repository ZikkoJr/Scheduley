package com.scheduley.export;

import com.scheduley.models.Course;
import com.scheduley.models.ScheduleProfile;
import com.scheduley.models.Task;
import com.scheduley.models.TimeBlock;

import java.util.List;

public record ScheduleExportData(
        String format,
        int version,
        String exportedAt,
        ScheduleProfileExport scheduleProfile,
        List<Course> courses,
        List<Task> tasks,
        List<TimeBlock> timeBlocks
) {
    public static final String FORMAT = "scheduley-schedule-export";
    public static final int VERSION = 1;

    public static ScheduleExportData of(
            String exportedAt,
            ScheduleProfile scheduleProfile,
            List<Course> courses,
            List<Task> tasks,
            List<TimeBlock> timeBlocks
    ) {
        return new ScheduleExportData(
                FORMAT,
                VERSION,
                exportedAt,
                ScheduleProfileExport.from(scheduleProfile),
                List.copyOf(courses),
                List.copyOf(tasks),
                List.copyOf(timeBlocks)
        );
    }
}
