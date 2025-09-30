package com.scheduley.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Runs one-time (or idempotent) schema creation for your database.
 * Call Migrations.init() ONCE at app startup (before you use any DAOs).
 */
public class Migrations {

    public static void init() {
        // DDL for your first table: COURSES
        final String createCourses = """
            CREATE TABLE IF NOT EXISTS courses (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                code        TEXT NOT NULL,
                name        TEXT NOT NULL,
                location    TEXT,
                day_of_week INTEGER NOT NULL,
                start_time  TEXT NOT NULL,
                end_time    TEXT NOT NULL,
                colour_hex   TEXT DEFAULT '#2196F3',
                created_at  TEXT DEFAULT (datetime('now')),
                updated_at  TEXT DEFAULT (datetime('now'))
              );
        """;

        // If you later add more tables, put more CREATE TABLE statements here.
        // final String createWorkShifts = "...";
        // final String createTasks = "...";

        // Open a connection, enable foreign keys, run the statements, close.
        try (Connection conn = ConnectDB.getConnection();
             Statement st = conn.createStatement()) {

            // Good practice for SQLite to enforce FK constraints.
            st.execute("PRAGMA foreign_keys = ON");

            st.execute(createCourses);
            // st.execute(createWorkShifts);
            // st.execute(createTasks);

        } catch (SQLException e) {
            throw new RuntimeException("Database migration failed", e);
        }
    }
}

