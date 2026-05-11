package com.scheduley.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.scheduley.dao.CourseDAO;
import com.scheduley.dao.TaskDAO;
import com.scheduley.dao.TimeBlockDAO;
import com.scheduley.export.ScheduleExportData;
import com.scheduley.models.ScheduleProfile;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public class ScheduleExportService {
    private final CourseDAO courseDAO;
    private final TaskDAO taskDAO;
    private final TimeBlockDAO timeBlockDAO;
    private final Gson gson;

    public ScheduleExportService(CourseDAO courseDAO, TaskDAO taskDAO, TimeBlockDAO timeBlockDAO) {
        this.courseDAO = courseDAO;
        this.taskDAO = taskDAO;
        this.timeBlockDAO = timeBlockDAO;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }

    public ScheduleExportData buildExportData(ScheduleProfile activeProfile) {
        if (activeProfile == null || activeProfile.getId() == null) {
            throw new IllegalStateException("No active schedule profile is selected.");
        }

        Long scheduleProfileId = activeProfile.getId();
        return ScheduleExportData.of(
                Instant.now().toString(),
                activeProfile,
                courseDAO.findAll(scheduleProfileId),
                taskDAO.findAll(scheduleProfileId),
                timeBlockDAO.findAll(scheduleProfileId)
        );
    }

    public void exportToJsonFile(ScheduleProfile activeProfile, Path destination) {
        if (destination == null) {
            throw new IllegalArgumentException("Export destination is required.");
        }

        ScheduleExportData exportData = buildExportData(activeProfile);
        try {
            Path parent = destination.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(destination, StandardCharsets.UTF_8)) {
                gson.toJson(exportData, writer);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write schedule export to " + destination, e);
        }
    }

    public String toJson(ScheduleExportData exportData) {
        return gson.toJson(exportData);
    }
}
