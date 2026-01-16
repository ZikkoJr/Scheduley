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



    private static final String CREATE_TABLE_COURSE = """
            CREATE TABLE IF NOT EXISTS course (
              id         INTEGER PRIMARY KEY AUTOINCREMENT,
              code       TEXT    NOT NULL UNIQUE,
              name       TEXT    NOT NULL,
              credits    INTEGER NOT NULL,
              colour_hex TEXT    DEFAULT '#2196F3'
            );
            """;
    private static final String Course_Seed_1 = """
            INSERT or IGNORE INTO course(code, name,credits,colour_hex)
            VALUES ('SC1000', 'Example Course 1', 3, '#00A86B');
    """;

  /*  private static final String CREATE_TABLE_WORKSHIFT = """
            CREATE TABLE IF NOT EXISTS workshift (
            id INTEGER PRIMARY KEY AUTOINCREMENT,

            """ */



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


            // Optional: Seed rows (safe to run multiple times)
            st.executeUpdate(Course_Seed_1);


            System.out.println("Schema initialized");
        } catch (SQLException e) {
            throw new RuntimeException("Database migration failed", e);
        }
    }
}

