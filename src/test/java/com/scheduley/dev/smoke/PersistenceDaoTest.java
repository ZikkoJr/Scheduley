package com.scheduley.dev.smoke;

import com.scheduley.dao.CourseDAO;
import com.scheduley.dao.ScheduleProfileDAO;
import com.scheduley.dao.TaskDAO;
import com.scheduley.dao.TimeBlockDAO;
import com.scheduley.dao.sqlite.SqliteCourseDao;
import com.scheduley.dao.sqlite.SqliteScheduleProfileDAO;
import com.scheduley.dao.sqlite.SqliteTaskDAO;
import com.scheduley.dao.sqlite.SqliteTimeBlockDAO;
import com.scheduley.db.ConnectDB;
import com.scheduley.db.Migrations;
import com.scheduley.export.ScheduleExportData;
import com.scheduley.models.Course;
import com.scheduley.models.ScheduleProfile;
import com.scheduley.models.Task;
import com.scheduley.models.TimeBlock;
import com.scheduley.service.ScheduleExportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistenceDaoTest {
    private Path dbPath;
    private ScheduleProfileDAO scheduleProfileDAO;
    private CourseDAO courseDAO;
    private TaskDAO taskDAO;
    private TimeBlockDAO timeBlockDAO;
    private Long activeProfileId;

    @BeforeEach
    void setUp() throws Exception {
        Path dbDir = Path.of("target", "test-dbs");
        Files.createDirectories(dbDir);
        dbPath = dbDir.resolve("scheduley-" + System.nanoTime() + ".db");
        System.setProperty("scheduley.db.path", dbPath.toString());
        Migrations.init();

        scheduleProfileDAO = new SqliteScheduleProfileDAO();
        activeProfileId = scheduleProfileDAO.findActive().orElseThrow().getId();
        courseDAO = new SqliteCourseDao();
        taskDAO = new SqliteTaskDAO();
        timeBlockDAO = new SqliteTimeBlockDAO();
    }

    @AfterEach
    void tearDown() throws Exception {
        System.clearProperty("scheduley.db.path");
        if (dbPath != null) {
            Files.deleteIfExists(dbPath);
        }
    }

    @Test
    void migrationsCreateRequiredTablesAndDefaultSettings() throws Exception {
        assertTrue(Files.exists(dbPath));
        assertTrue(tableExists("course"));
        assertTrue(tableExists("task"));
        assertTrue(tableExists("time_block"));
        assertTrue(tableExists("schedule_profile"));
        assertTrue(tableExists("app_settings"));
        assertEquals(1, countRows("schedule_profile"));
        assertEquals(1, countRows("app_settings"));
    }

    @Test
    void courseDaoCreatesFindsUpdatesAndDeletesCourses() {
        Course created = courseDAO.create(new Course(
                "ITEC3220",
                "Database Systems",
                "Dr. Rivera",
                "Room 204",
                "#00A86B",
                "Relational design"
        ), activeProfileId);

        assertNotNull(created.getId());
        assertTrue(courseDAO.findById(created.getId(), activeProfileId).isPresent());
        assertEquals("Database Systems", courseDAO.findByCode("ITEC3220", activeProfileId).orElseThrow().getName());
        assertEquals(1, courseDAO.findAll(activeProfileId).size());

        created.setName("Database Systems II");
        created.setInstructor("Dr. Chen");
        assertTrue(courseDAO.update(created, activeProfileId));
        Course updated = courseDAO.findById(created.getId(), activeProfileId).orElseThrow();
        assertEquals("Database Systems II", updated.getName());
        assertEquals("Dr. Chen", updated.getInstructor());

        assertTrue(courseDAO.deleteById(created.getId(), activeProfileId));
        assertTrue(courseDAO.findById(created.getId(), activeProfileId).isEmpty());
    }

    @Test
    void taskDaoCreatesFindsUpdatesAndDeletesTasks() {
        Course course = courseDAO.create(new Course("MATH101", "Calculus I", "Dr. Noether", "Hall 2", "#2563EB", null), activeProfileId);
        Task created = taskDAO.create(new Task(
                "Problem set",
                course.getId(),
                "2026-05-12",
                90,
                "HIGH",
                "NOT_STARTED",
                "Chapter 4"
        ), activeProfileId);

        assertNotNull(created.getId());
        assertEquals("Problem set", taskDAO.findById(created.getId(), activeProfileId).orElseThrow().getTitle());
        assertEquals(1, taskDAO.findByCourseId(course.getId(), activeProfileId).size());
        assertEquals(1, taskDAO.findByStatus("NOT_STARTED", activeProfileId).size());

        created.setStatus("COMPLETE");
        created.setEstimatedMinutes(120);
        assertTrue(taskDAO.update(created, activeProfileId));
        Task updated = taskDAO.findById(created.getId(), activeProfileId).orElseThrow();
        assertEquals("COMPLETE", updated.getStatus());
        assertEquals(120, updated.getEstimatedMinutes());

        assertTrue(taskDAO.deleteById(created.getId(), activeProfileId));
        assertTrue(taskDAO.findById(created.getId(), activeProfileId).isEmpty());
    }

    @Test
    void timeBlockConflictDetectionFindsOverlapsAndIgnoresExcludedBlock() {
        Course course = courseDAO.create(new Course("CS201", "Data Structures", "Dr. Hopper", "Lab 1", "#7C3AED", null), activeProfileId);
        TimeBlock lecture = timeBlockDAO.create(new TimeBlock(
                "Data Structures",
                "COURSE",
                course.getId(),
                null,
                2,
                null,
                600,
                690,
                "Lab 1",
                "#7C3AED",
                null
        ), activeProfileId);
        TimeBlock work = timeBlockDAO.create(new TimeBlock(
                "Work shift",
                "WORK",
                null,
                null,
                2,
                null,
                660,
                780,
                "Campus Cafe",
                "#0F766E",
                null
        ), activeProfileId);
        timeBlockDAO.create(new TimeBlock(
                "Evening study",
                "STUDY",
                null,
                null,
                2,
                null,
                900,
                960,
                "Library",
                "#F59E0B",
                null
        ), activeProfileId);

        List<TimeBlock> conflicts = timeBlockDAO.findConflicts(2, lecture.getStartMinute(), lecture.getEndMinute(), lecture.getId(), activeProfileId);
        assertEquals(1, conflicts.size());
        assertEquals(work.getId(), conflicts.getFirst().getId());

        assertTrue(timeBlockDAO.findConflicts(2, 780, 840, null, activeProfileId).isEmpty());
        assertFalse(timeBlockDAO.findConflicts(2, 660, 780, work.getId(), activeProfileId).stream()
                .anyMatch(block -> block.getId().equals(work.getId())));
    }

    @Test
    void scheduleProfilesScopeDaoReadsAndWrites() {
        Course defaultCourse = courseDAO.create(
                new Course("HIST100", "History", "Dr. Morgan", "A101", "#2563EB", null),
                activeProfileId
        );

        ScheduleProfile fall = scheduleProfileDAO.create(new ScheduleProfile("Fall 2026", null));
        scheduleProfileDAO.setActiveProfile(fall.getId());
        Course fallCourse = courseDAO.create(
                new Course("CS301", "Algorithms", "Dr. Knuth", "B202", "#7C3AED", null),
                fall.getId()
        );

        assertEquals(1, courseDAO.findAll(activeProfileId).size());
        assertEquals(defaultCourse.getId(), courseDAO.findAll(activeProfileId).getFirst().getId());
        assertEquals(1, courseDAO.findAll(fall.getId()).size());
        assertEquals(fallCourse.getId(), courseDAO.findAll(fall.getId()).getFirst().getId());
        assertTrue(courseDAO.findById(fallCourse.getId(), activeProfileId).isEmpty());
    }

    @Test
    void scheduleExportIncludesOnlyActiveScheduleData() {
        courseDAO.create(
                new Course("HIST100", "History", "Dr. Morgan", "A101", "#2563EB", null),
                activeProfileId
        );

        ScheduleProfile fall = scheduleProfileDAO.create(new ScheduleProfile("Fall 2026", "Test export profile"));
        scheduleProfileDAO.setActiveProfile(fall.getId());
        Course fallCourse = courseDAO.create(
                new Course("CS301", "Algorithms", "Dr. Knuth", "B202", "#7C3AED", null),
                fall.getId()
        );
        Task fallTask = taskDAO.create(
                new Task("Project", fallCourse.getId(), "2026-05-20", 120, "HIGH", "NOT_STARTED", null),
                fall.getId()
        );
        TimeBlock fallBlock = timeBlockDAO.create(
                new TimeBlock("Algorithms", "COURSE", fallCourse.getId(), null, 1, null, 540, 600, "B202", "#7C3AED", null),
                fall.getId()
        );

        ScheduleExportService exportService = new ScheduleExportService(courseDAO, taskDAO, timeBlockDAO);
        ScheduleExportData exportData = exportService.buildExportData(scheduleProfileDAO.findActive().orElseThrow());

        assertEquals("scheduley-schedule-export", exportData.format());
        assertEquals(1, exportData.version());
        assertEquals(fall.getId(), exportData.scheduleProfile().id());
        assertEquals(1, exportData.courses().size());
        assertEquals(fallCourse.getId(), exportData.courses().getFirst().getId());
        assertEquals(1, exportData.tasks().size());
        assertEquals(fallTask.getId(), exportData.tasks().getFirst().getId());
        assertEquals(1, exportData.timeBlocks().size());
        assertEquals(fallBlock.getId(), exportData.timeBlocks().getFirst().getId());
        assertTrue(exportService.toJson(exportData).contains("\"format\": \"scheduley-schedule-export\""));
    }

    @Test
    void scheduleProfileDeleteRemovesOwnedDataAndKeepsOneActiveProfile() {
        ScheduleProfile temp = scheduleProfileDAO.create(new ScheduleProfile("Temporary", null));
        courseDAO.create(new Course("TMP101", "Temporary", null, null, "#2563EB", null), temp.getId());

        assertTrue(scheduleProfileDAO.deleteById(temp.getId()));
        assertTrue(courseDAO.findAll(temp.getId()).isEmpty());
        assertTrue(scheduleProfileDAO.findActive().isPresent());
        assertThrows(IllegalStateException.class, () -> scheduleProfileDAO.deleteById(activeProfileId));
    }

    @Test
    void migrationsAssignLegacyMvpDataToDefaultScheduleProfile() throws Exception {
        Path legacyPath = dbPath.getParent().resolve("legacy-" + System.nanoTime() + ".db");
        try {
            createLegacyMvpDatabase(legacyPath);
            System.setProperty("scheduley.db.path", legacyPath.toString());

            Migrations.init();
            ScheduleProfileDAO profiles = new SqliteScheduleProfileDAO();
            Long defaultProfileId = profiles.findActive().orElseThrow().getId();

            assertEquals(1, profiles.findAll().size());
            assertEquals("Default Schedule", profiles.findActive().orElseThrow().getName());
            assertEquals(1, new SqliteCourseDao().findAll(defaultProfileId).size());
            assertEquals(1, new SqliteTaskDAO().findAll(defaultProfileId).size());
            assertEquals(1, new SqliteTimeBlockDAO().findAll(defaultProfileId).size());
        } finally {
            System.setProperty("scheduley.db.path", dbPath.toString());
            Files.deleteIfExists(legacyPath);
        }
    }

    private boolean tableExists(String tableName) throws Exception {
        try (Connection conn = ConnectDB.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM sqlite_master WHERE type = 'table' AND name = '" + tableName + "'")) {
            return rs.next();
        }
    }

    private int countRows(String tableName) throws Exception {
        try (Connection conn = ConnectDB.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void createLegacyMvpDatabase(Path path) throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + path);
             Statement st = conn.createStatement()) {
            st.executeUpdate("""
                    CREATE TABLE course (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        code TEXT NOT NULL,
                        name TEXT NOT NULL,
                        instructor TEXT,
                        location_text TEXT,
                        color_hex TEXT,
                        notes TEXT,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                    """);
            st.executeUpdate("""
                    CREATE TABLE task (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        course_id INTEGER,
                        due_date TEXT,
                        estimated_minutes INTEGER,
                        priority TEXT NOT NULL DEFAULT 'MEDIUM',
                        status TEXT NOT NULL DEFAULT 'NOT_STARTED',
                        notes TEXT,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                    """);
            st.executeUpdate("""
                    CREATE TABLE time_block (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        block_type TEXT NOT NULL,
                        course_id INTEGER,
                        task_id INTEGER,
                        day_of_week INTEGER,
                        block_date TEXT,
                        start_minute INTEGER NOT NULL,
                        end_minute INTEGER NOT NULL,
                        location_text TEXT,
                        color_hex TEXT,
                        notes TEXT,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                    """);
            st.executeUpdate("""
                    INSERT INTO course(code, name, instructor, location_text, color_hex, notes, created_at, updated_at)
                    VALUES ('BIO101', 'Biology', 'Dr. Curie', 'Lab', '#2563EB', NULL, '2026-01-01T00:00:00', '2026-01-01T00:00:00')
                    """);
            st.executeUpdate("""
                    INSERT INTO task(title, course_id, due_date, estimated_minutes, priority, status, notes, created_at, updated_at)
                    VALUES ('Read chapter', 1, '2026-05-12', 60, 'MEDIUM', 'NOT_STARTED', NULL, '2026-01-01T00:00:00', '2026-01-01T00:00:00')
                    """);
            st.executeUpdate("""
                    INSERT INTO time_block(title, block_type, course_id, task_id, day_of_week, block_date, start_minute, end_minute,
                                           location_text, color_hex, notes, created_at, updated_at)
                    VALUES ('Biology', 'COURSE', 1, NULL, 1, NULL, 540, 600, 'Lab', '#2563EB', NULL,
                            '2026-01-01T00:00:00', '2026-01-01T00:00:00')
                    """);
        }
    }
}
