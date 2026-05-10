package com.scheduley.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Migrations {
    private static final String CREATE_TABLE_COURSE = """
            CREATE TABLE IF NOT EXISTS course (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                schedule_profile_id INTEGER,
                code TEXT NOT NULL,
                name TEXT NOT NULL,
                instructor TEXT,
                location_text TEXT,
                color_hex TEXT,
                notes TEXT,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                FOREIGN KEY (schedule_profile_id) REFERENCES schedule_profile(id) ON DELETE CASCADE
            );
            """;

    private static final String CREATE_TABLE_TASK = """
            CREATE TABLE IF NOT EXISTS task (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                schedule_profile_id INTEGER,
                title TEXT NOT NULL,
                course_id INTEGER,
                due_date TEXT,
                estimated_minutes INTEGER,
                priority TEXT NOT NULL DEFAULT 'MEDIUM',
                status TEXT NOT NULL DEFAULT 'NOT_STARTED',
                notes TEXT,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                FOREIGN KEY (schedule_profile_id) REFERENCES schedule_profile(id) ON DELETE CASCADE,
                FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE SET NULL
            );
            """;

    private static final String CREATE_TABLE_TIME_BLOCK = """
            CREATE TABLE IF NOT EXISTS time_block (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                schedule_profile_id INTEGER,
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
                updated_at TEXT NOT NULL,
                FOREIGN KEY (schedule_profile_id) REFERENCES schedule_profile(id) ON DELETE CASCADE,
                FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
                FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE SET NULL,
                CHECK (start_minute >= 0 AND start_minute < 1440),
                CHECK (end_minute > 0 AND end_minute <= 1440),
                CHECK (end_minute > start_minute),
                CHECK (day_of_week IS NULL OR day_of_week BETWEEN 1 AND 7)
            );
            """;

    private static final String CREATE_TABLE_SCHEDULE_PROFILE = """
            CREATE TABLE IF NOT EXISTS schedule_profile (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT,
                is_active INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            );
            """;

    private static final String CREATE_TABLE_SETTINGS = """
            CREATE TABLE IF NOT EXISTS app_settings (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                week_start_day INTEGER NOT NULL DEFAULT 1,
                calendar_start_hour INTEGER NOT NULL DEFAULT 7,
                calendar_end_hour INTEGER NOT NULL DEFAULT 23,
                theme TEXT DEFAULT 'LIGHT'
            );
            """;

    public static void init() {
        try (Connection conn = ConnectDB.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.executeUpdate(CREATE_TABLE_SCHEDULE_PROFILE);
            st.executeUpdate(CREATE_TABLE_COURSE);
            migrateLegacyCourseTable(conn);
            st.executeUpdate(CREATE_TABLE_TASK);
            st.executeUpdate(CREATE_TABLE_TIME_BLOCK);
            migrateScheduleProfiles(conn);
            st.executeUpdate(CREATE_TABLE_SETTINGS);
            st.executeUpdate("""
                    INSERT OR IGNORE INTO app_settings
                    (id, week_start_day, calendar_start_hour, calendar_end_hour, theme)
                    VALUES (1, 1, 7, 23, 'LIGHT')
                    """);
        } catch (SQLException e) {
            throw new RuntimeException("Database migration failed", e);
        }
    }

    private static void migrateScheduleProfiles(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "course", "schedule_profile_id", "INTEGER");
        addColumnIfMissing(conn, "task", "schedule_profile_id", "INTEGER");
        addColumnIfMissing(conn, "time_block", "schedule_profile_id", "INTEGER");

        long defaultProfileId = ensureDefaultProfile(conn);
        assignExistingData(conn, defaultProfileId);
        ensureSingleActiveProfile(conn);
    }

    private static long ensureDefaultProfile(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM schedule_profile ORDER BY id LIMIT 1")) {
            if (rs.next()) {
                return rs.getLong("id");
            }
        }

        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                    INSERT INTO schedule_profile(name, description, is_active, created_at, updated_at)
                    VALUES ('Default Schedule', 'Imported MVP schedule data', 1, datetime('now'), datetime('now'))
                    """, Statement.RETURN_GENERATED_KEYS);
            try (ResultSet keys = st.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Default schedule profile insert succeeded but no generated key returned.");
    }

    private static void assignExistingData(Connection conn, long defaultProfileId) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("UPDATE course SET schedule_profile_id = " + defaultProfileId + " WHERE schedule_profile_id IS NULL");
            st.executeUpdate("UPDATE task SET schedule_profile_id = " + defaultProfileId + " WHERE schedule_profile_id IS NULL");
            st.executeUpdate("UPDATE time_block SET schedule_profile_id = " + defaultProfileId + " WHERE schedule_profile_id IS NULL");
        }
    }

    private static void ensureSingleActiveProfile(Connection conn) throws SQLException {
        long activeId = -1;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM schedule_profile WHERE is_active = 1 ORDER BY id LIMIT 1")) {
            if (rs.next()) {
                activeId = rs.getLong("id");
            }
        }

        if (activeId == -1) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id FROM schedule_profile ORDER BY id LIMIT 1")) {
                if (rs.next()) {
                    activeId = rs.getLong("id");
                }
            }
        }

        if (activeId != -1) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("UPDATE schedule_profile SET is_active = CASE WHEN id = " + activeId + " THEN 1 ELSE 0 END");
            }
        }
    }

    private static void migrateLegacyCourseTable(Connection conn) throws SQLException {
        if (hasColumn(conn, "course", "credits")) {
            rebuildLegacyCourseTable(conn);
            return;
        }

        addColumnIfMissing(conn, "course", "instructor", "TEXT");
        addColumnIfMissing(conn, "course", "location_text", "TEXT");
        addColumnIfMissing(conn, "course", "color_hex", "TEXT");
        addColumnIfMissing(conn, "course", "notes", "TEXT");
        addColumnIfMissing(conn, "course", "created_at", "TEXT NOT NULL DEFAULT '1970-01-01T00:00:00'");
        addColumnIfMissing(conn, "course", "updated_at", "TEXT NOT NULL DEFAULT '1970-01-01T00:00:00'");

        if (hasColumn(conn, "course", "colour_hex")) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("UPDATE course SET color_hex = COALESCE(color_hex, colour_hex, '#4F46E5')");
            }
        }
    }

    private static void rebuildLegacyCourseTable(Connection conn) throws SQLException {
        boolean hasColorHex = hasColumn(conn, "course", "color_hex");
        boolean hasColourHex = hasColumn(conn, "course", "colour_hex");
        String colorExpression;
        if (hasColorHex && hasColourHex) {
            colorExpression = "COALESCE(color_hex, colour_hex, '#4F46E5')";
        } else if (hasColorHex) {
            colorExpression = "COALESCE(color_hex, '#4F46E5')";
        } else if (hasColourHex) {
            colorExpression = "COALESCE(colour_hex, '#4F46E5')";
        } else {
            colorExpression = "'#4F46E5'";
        }

        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = OFF");
            st.executeUpdate("DROP TABLE IF EXISTS course_mvp");
            st.executeUpdate("""
                    CREATE TABLE course_mvp (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        schedule_profile_id INTEGER,
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
                    INSERT INTO course_mvp(id, schedule_profile_id, code, name, instructor, location_text, color_hex, notes, created_at, updated_at)
                    SELECT id, NULL, code, name, NULL, NULL, %s, NULL, datetime('now'), datetime('now')
                    FROM course
                    """.formatted(colorExpression));
            st.executeUpdate("DROP TABLE course");
            st.executeUpdate("ALTER TABLE course_mvp RENAME TO course");
            st.execute("PRAGMA foreign_keys = ON");
        }
    }

    private static void addColumnIfMissing(Connection conn, String table, String column, String definition)
            throws SQLException {
        if (!hasColumn(conn, table, column)) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            }
        }
    }

    private static boolean hasColumn(Connection conn, String table, String column) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }
}

