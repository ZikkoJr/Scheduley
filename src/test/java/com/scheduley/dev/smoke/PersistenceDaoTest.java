package com.scheduley.dev.smoke;

import com.scheduley.dao.CourseDAO;
import com.scheduley.dao.TaskDAO;
import com.scheduley.dao.TimeBlockDAO;
import com.scheduley.dao.sqlite.SqliteCourseDao;
import com.scheduley.dao.sqlite.SqliteTaskDAO;
import com.scheduley.dao.sqlite.SqliteTimeBlockDAO;
import com.scheduley.db.ConnectDB;
import com.scheduley.db.Migrations;
import com.scheduley.models.Course;
import com.scheduley.models.Task;
import com.scheduley.models.TimeBlock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistenceDaoTest {
    private Path dbPath;
    private CourseDAO courseDAO;
    private TaskDAO taskDAO;
    private TimeBlockDAO timeBlockDAO;

    @BeforeEach
    void setUp() throws Exception {
        Path dbDir = Path.of("target", "test-dbs");
        Files.createDirectories(dbDir);
        dbPath = dbDir.resolve("scheduley-" + System.nanoTime() + ".db");
        System.setProperty("scheduley.db.path", dbPath.toString());
        Migrations.init();

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
        assertTrue(tableExists("app_settings"));
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
        ));

        assertNotNull(created.getId());
        assertTrue(courseDAO.findById(created.getId()).isPresent());
        assertEquals("Database Systems", courseDAO.findByCode("ITEC3220").orElseThrow().getName());
        assertEquals(1, courseDAO.findAll().size());

        created.setName("Database Systems II");
        created.setInstructor("Dr. Chen");
        assertTrue(courseDAO.update(created));
        Course updated = courseDAO.findById(created.getId()).orElseThrow();
        assertEquals("Database Systems II", updated.getName());
        assertEquals("Dr. Chen", updated.getInstructor());

        assertTrue(courseDAO.deleteById(created.getId()));
        assertTrue(courseDAO.findById(created.getId()).isEmpty());
    }

    @Test
    void taskDaoCreatesFindsUpdatesAndDeletesTasks() {
        Course course = courseDAO.create(new Course("MATH101", "Calculus I", "Dr. Noether", "Hall 2", "#2563EB", null));
        Task created = taskDAO.create(new Task(
                "Problem set",
                course.getId(),
                "2026-05-12",
                90,
                "HIGH",
                "NOT_STARTED",
                "Chapter 4"
        ));

        assertNotNull(created.getId());
        assertEquals("Problem set", taskDAO.findById(created.getId()).orElseThrow().getTitle());
        assertEquals(1, taskDAO.findByCourseId(course.getId()).size());
        assertEquals(1, taskDAO.findByStatus("NOT_STARTED").size());

        created.setStatus("COMPLETE");
        created.setEstimatedMinutes(120);
        assertTrue(taskDAO.update(created));
        Task updated = taskDAO.findById(created.getId()).orElseThrow();
        assertEquals("COMPLETE", updated.getStatus());
        assertEquals(120, updated.getEstimatedMinutes());

        assertTrue(taskDAO.deleteById(created.getId()));
        assertTrue(taskDAO.findById(created.getId()).isEmpty());
    }

    @Test
    void timeBlockConflictDetectionFindsOverlapsAndIgnoresExcludedBlock() {
        Course course = courseDAO.create(new Course("CS201", "Data Structures", "Dr. Hopper", "Lab 1", "#7C3AED", null));
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
        ));
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
        ));
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
        ));

        List<TimeBlock> conflicts = timeBlockDAO.findConflicts(2, lecture.getStartMinute(), lecture.getEndMinute(), lecture.getId());
        assertEquals(1, conflicts.size());
        assertEquals(work.getId(), conflicts.getFirst().getId());

        assertTrue(timeBlockDAO.findConflicts(2, 780, 840, null).isEmpty());
        assertFalse(timeBlockDAO.findConflicts(2, 660, 780, work.getId()).stream()
                .anyMatch(block -> block.getId().equals(work.getId())));
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
}
