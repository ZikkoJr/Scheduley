package com.scheduley.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLOutput;
import java.sql.Statement;

/**
 * Runs one-time (or idempotent) schema creation for your database.
 * Call Migrations.init() ONCE at app startup (before you use any DAOs).
 */
public class Migrations {


    // TABLE CREATIONS:

    private static final String CREATE_TABLE_COURSE = """
            CREATE TABLE IF NOT EXISTS course (
              id         INTEGER PRIMARY KEY AUTOINCREMENT,
              code       TEXT    NOT NULL UNIQUE,
              name       TEXT    NOT NULL,
              credits    INTEGER NOT NULL,
              colour_hex TEXT    DEFAULT '#2196F3'
            );
            """;

    private static final String CREATE_TABLE_TIMEBLOCK = """
            CREATE TABLE IF NOT EXISTS time_block (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              title TEXT NOT NULL,
              category TEXT NOT NULL,
              course_id INTEGER,
              day_of_week INTEGER NOT NULL CHECK(day_of_week BETWEEN 1 AND 7),
              start_min INTEGER NOT NULL CHECK(start_min BETWEEN 0 AND 1439),
              end_min INTEGER NOT NULL CHECK(end_min BETWEEN 1 AND 1440),
              notes TEXT,
              FOREIGN KEY(course_id) REFERENCES course(id) ON DELETE CASCADE,
              CHECK(end_min > start_min)
            );
            
    """;

    // SEEDS

    private static final String Course_Seed_1 = """
            INSERT or IGNORE INTO course(code, name,credits,colour_hex)
            VALUES ('SC1000', 'Example Course 1', 3, '#00A86B');
    """;

    private static final String TimeBlock_Seed_1 = """
            INSERT or IGNORE INTO time_block(title, category, day_of_week,
            start_min, end_min, notes) VALUES ('Hobby 1', 'Hobby', 1, 780, 840, 
            'Doing hobby 1 from 1:00 pm to 2:00 pm');
            """;




    //This method will be called at startup
    public static void init() {

        // IMPORTANT: try-with-resources auto closes everything
        // Open a connection, enable foreign keys, run the statements, close.
        try (Connection conn = ConnectDB.getConnection();
             Statement st = conn.createStatement()) {

            // Good practice for SQLite to enforce FK constraints.
            st.execute("PRAGMA foreign_keys = ON");

            // Create Table(s)
            st.executeUpdate(CREATE_TABLE_COURSE);
            st.executeUpdate(CREATE_TABLE_TIMEBLOCK);


            // Optional: Seed rows (safe to run multiple times)
            st.executeUpdate(Course_Seed_1);
            st.executeUpdate(TimeBlock_Seed_1);


            System.out.println("Schema initialized");
        } catch (SQLException e) {
            throw new RuntimeException("Database migration failed", e);
        }
    }
}

