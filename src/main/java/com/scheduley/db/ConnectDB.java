package com.scheduley.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConnectDB {
    private static final String URL = "jdbc:sqlite:scheduley.db";

    // Returns a NEW connection each call (callers close it via try-with-resources)
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        // Enforce foreign keys for SQLite every time we open a connection
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }
}
