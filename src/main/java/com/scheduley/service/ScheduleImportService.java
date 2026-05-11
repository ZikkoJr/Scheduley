package com.scheduley.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.scheduley.dao.CourseDAO;
import com.scheduley.dao.ScheduleProfileDAO;
import com.scheduley.dao.TaskDAO;
import com.scheduley.dao.TimeBlockDAO;
import com.scheduley.export.ScheduleExportData;
import com.scheduley.models.Course;
import com.scheduley.models.ScheduleProfile;
import com.scheduley.models.Task;
import com.scheduley.models.TimeBlock;
import com.scheduley.util.ValidationUtils;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ScheduleImportService {
    private static final DateTimeFormatter NAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final ScheduleProfileDAO scheduleProfileDAO;
    private final CourseDAO courseDAO;
    private final TaskDAO taskDAO;
    private final TimeBlockDAO timeBlockDAO;
    private final Gson gson;

    public ScheduleImportService(
            ScheduleProfileDAO scheduleProfileDAO,
            CourseDAO courseDAO,
            TaskDAO taskDAO,
            TimeBlockDAO timeBlockDAO
    ) {
        this.scheduleProfileDAO = scheduleProfileDAO;
        this.courseDAO = courseDAO;
        this.taskDAO = taskDAO;
        this.timeBlockDAO = timeBlockDAO;
        this.gson = new GsonBuilder().create();
    }

    public ScheduleImportResult importFromJsonFile(Path source) {
        if (source == null) {
            throw new ScheduleImportException("Choose a Scheduley JSON export file to import.");
        }

        ScheduleExportData exportData = readExportData(source);
        return importExportData(exportData);
    }

    public ScheduleImportResult importExportData(ScheduleExportData exportData) {
        ValidatedExport validated = validate(exportData);
        ScheduleProfile createdProfile = null;
        try {
            createdProfile = scheduleProfileDAO.create(new ScheduleProfile(
                    uniqueImportedProfileName(validated.profileName()),
                    "Imported from Scheduley JSON export"
            ));
            Long newProfileId = createdProfile.getId();

            Map<Long, Long> courseIdMap = importCourses(validated.courses(), newProfileId);
            Map<Long, Long> taskIdMap = importTasks(validated.tasks(), courseIdMap, newProfileId);
            importTimeBlocks(validated.timeBlocks(), courseIdMap, taskIdMap, newProfileId);

            scheduleProfileDAO.setActiveProfile(newProfileId);
            ScheduleProfile activeProfile = scheduleProfileDAO.findById(newProfileId).orElse(createdProfile);
            return new ScheduleImportResult(
                    activeProfile,
                    validated.courses().size(),
                    validated.tasks().size(),
                    validated.timeBlocks().size()
            );
        } catch (RuntimeException e) {
            cleanupCreatedProfile(createdProfile);
            if (e instanceof ScheduleImportException) {
                throw e;
            }
            throw new ScheduleImportException("Could not import the schedule. No existing schedule data was changed.", e);
        }
    }

    private ScheduleExportData readExportData(Path source) {
        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            ScheduleExportData exportData = gson.fromJson(reader, ScheduleExportData.class);
            if (exportData == null) {
                throw new ScheduleImportException("The selected file is empty or is not a Scheduley export.");
            }
            return exportData;
        } catch (JsonParseException e) {
            throw new ScheduleImportException("The selected file is not valid Scheduley JSON.", e);
        } catch (IOException e) {
            throw new ScheduleImportException("Could not read the selected JSON file.", e);
        }
    }

    private ValidatedExport validate(ScheduleExportData exportData) {
        if (exportData == null) {
            throw new ScheduleImportException("The selected file is empty or is not a Scheduley export.");
        }
        if (!ScheduleExportData.FORMAT.equals(exportData.format())) {
            throw new ScheduleImportException("This file is not a Scheduley schedule export.");
        }
        if (exportData.version() != ScheduleExportData.VERSION) {
            throw new ScheduleImportException("This Scheduley export version is not supported: " + exportData.version() + ".");
        }
        if (exportData.scheduleProfile() == null) {
            throw new ScheduleImportException("The export is missing schedule profile details.");
        }

        List<Course> courses = listOrEmpty(exportData.courses());
        List<Task> tasks = listOrEmpty(exportData.tasks());
        List<TimeBlock> timeBlocks = listOrEmpty(exportData.timeBlocks());
        validateCourses(courses);
        validateUniqueIds("course", exportedCourseIds(courses), courses.stream().filter(course -> course.getId() != null).count());
        validateTasks(tasks, exportedCourseIds(courses));
        validateUniqueIds("task", exportedTaskIds(tasks), tasks.stream().filter(task -> task.getId() != null).count());
        validateTimeBlocks(timeBlocks, exportedCourseIds(courses), exportedTaskIds(tasks));

        String profileName = exportData.scheduleProfile().name();
        if (profileName == null || profileName.isBlank()) {
            profileName = "Schedule";
        }
        return new ValidatedExport(profileName.trim(), courses, tasks, timeBlocks);
    }

    private void validateUniqueIds(String recordType, Set<Long> uniqueIds, long nonNullIdCount) {
        if (uniqueIds.size() != nonNullIdCount) {
            throw new ScheduleImportException("The export contains duplicate " + recordType + " IDs and cannot be imported safely.");
        }
    }

    private void validateCourses(List<Course> courses) {
        for (int i = 0; i < courses.size(); i++) {
            Course course = courses.get(i);
            if (course == null) {
                throw new ScheduleImportException("Course #" + (i + 1) + " is missing.");
            }
            try {
                ValidationUtils.validateCourse(course);
            } catch (IllegalArgumentException e) {
                throw new ScheduleImportException("Course #" + (i + 1) + " is invalid: " + e.getMessage(), e);
            }
        }
    }

    private void validateTasks(List<Task> tasks, Set<Long> courseIds) {
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            if (task == null) {
                throw new ScheduleImportException("Task #" + (i + 1) + " is missing.");
            }
            if (task.getCourseId() != null && !courseIds.contains(task.getCourseId())) {
                throw new ScheduleImportException("Task \"" + value(task.getTitle()) + "\" references a missing course.");
            }
            try {
                ValidationUtils.validateTask(task);
            } catch (IllegalArgumentException e) {
                throw new ScheduleImportException("Task #" + (i + 1) + " is invalid: " + e.getMessage(), e);
            }
        }
    }

    private void validateTimeBlocks(List<TimeBlock> timeBlocks, Set<Long> courseIds, Set<Long> taskIds) {
        for (int i = 0; i < timeBlocks.size(); i++) {
            TimeBlock block = timeBlocks.get(i);
            if (block == null) {
                throw new ScheduleImportException("Time block #" + (i + 1) + " is missing.");
            }
            if (block.getCourseId() != null && !courseIds.contains(block.getCourseId())) {
                throw new ScheduleImportException("Time block \"" + value(block.getTitle()) + "\" references a missing course.");
            }
            if (block.getTaskId() != null && !taskIds.contains(block.getTaskId())) {
                throw new ScheduleImportException("Time block \"" + value(block.getTitle()) + "\" references a missing task.");
            }
            try {
                ValidationUtils.validateTimeBlock(block);
            } catch (IllegalArgumentException e) {
                throw new ScheduleImportException("Time block #" + (i + 1) + " is invalid: " + e.getMessage(), e);
            }
        }
    }

    private Map<Long, Long> importCourses(List<Course> courses, Long newProfileId) {
        Map<Long, Long> courseIdMap = new HashMap<>();
        for (Course exported : courses) {
            Course created = courseDAO.create(new Course(
                    exported.getCode(),
                    exported.getName(),
                    exported.getInstructor(),
                    exported.getLocationText(),
                    exported.getColorHex(),
                    exported.getNotes()
            ), newProfileId);
            if (exported.getId() != null) {
                courseIdMap.put(exported.getId(), created.getId());
            }
        }
        return courseIdMap;
    }

    private Map<Long, Long> importTasks(List<Task> tasks, Map<Long, Long> courseIdMap, Long newProfileId) {
        Map<Long, Long> taskIdMap = new HashMap<>();
        for (Task exported : tasks) {
            Task created = taskDAO.create(new Task(
                    exported.getTitle(),
                    remap(exported.getCourseId(), courseIdMap, "task course"),
                    exported.getDueDate(),
                    exported.getEstimatedMinutes(),
                    exported.getPriority(),
                    exported.getStatus(),
                    exported.getNotes()
            ), newProfileId);
            if (exported.getId() != null) {
                taskIdMap.put(exported.getId(), created.getId());
            }
        }
        return taskIdMap;
    }

    private void importTimeBlocks(
            List<TimeBlock> timeBlocks,
            Map<Long, Long> courseIdMap,
            Map<Long, Long> taskIdMap,
            Long newProfileId
    ) {
        for (TimeBlock exported : timeBlocks) {
            timeBlockDAO.create(new TimeBlock(
                    exported.getTitle(),
                    exported.getBlockType(),
                    remap(exported.getCourseId(), courseIdMap, "time block course"),
                    remap(exported.getTaskId(), taskIdMap, "time block task"),
                    exported.getDayOfWeek(),
                    exported.getBlockDate(),
                    exported.getStartMinute(),
                    exported.getEndMinute(),
                    exported.getLocationText(),
                    exported.getColorHex(),
                    exported.getNotes()
            ), newProfileId);
        }
    }

    private String uniqueImportedProfileName(String exportedName) {
        String baseName = "Imported - " + exportedName;
        Set<String> existingNames = new HashSet<>();
        for (ScheduleProfile profile : scheduleProfileDAO.findAll()) {
            if (profile.getName() != null) {
                existingNames.add(profile.getName().toLowerCase(Locale.ROOT));
            }
        }

        if (!existingNames.contains(baseName.toLowerCase(Locale.ROOT))) {
            return baseName;
        }

        String timestamped = baseName + " " + LocalDateTime.now().format(NAME_TIMESTAMP);
        if (!existingNames.contains(timestamped.toLowerCase(Locale.ROOT))) {
            return timestamped;
        }

        int suffix = 2;
        while (existingNames.contains((baseName + " (" + suffix + ")").toLowerCase(Locale.ROOT))) {
            suffix++;
        }
        return baseName + " (" + suffix + ")";
    }

    private void cleanupCreatedProfile(ScheduleProfile createdProfile) {
        if (createdProfile == null || createdProfile.getId() == null) {
            return;
        }
        try {
            scheduleProfileDAO.deleteById(createdProfile.getId());
        } catch (RuntimeException ignored) {
            // Preserve the original import error; the UI presents that to the user.
        }
    }

    private static Long remap(Long exportedId, Map<Long, Long> idMap, String relationshipName) {
        if (exportedId == null) {
            return null;
        }
        Long remappedId = idMap.get(exportedId);
        if (remappedId == null) {
            throw new ScheduleImportException("Could not remap exported " + relationshipName + " id " + exportedId + ".");
        }
        return remappedId;
    }

    private static Set<Long> exportedCourseIds(List<Course> courses) {
        Set<Long> ids = new HashSet<>();
        for (Course course : courses) {
            if (course != null && course.getId() != null) {
                ids.add(course.getId());
            }
        }
        return ids;
    }

    private static Set<Long> exportedTaskIds(List<Task> tasks) {
        Set<Long> ids = new HashSet<>();
        for (Task task : tasks) {
            if (task != null && task.getId() != null) {
                ids.add(task.getId());
            }
        }
        return ids;
    }

    private static <T> List<T> listOrEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static String value(String value) {
        return value == null || value.isBlank() ? "Untitled" : value;
    }

    private record ValidatedExport(
            String profileName,
            List<Course> courses,
            List<Task> tasks,
            List<TimeBlock> timeBlocks
    ) {
    }
}
