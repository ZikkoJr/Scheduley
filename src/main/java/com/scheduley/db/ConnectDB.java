package com.scheduley.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConnectDB {
    private static final String DEFAULT_DB_PATH = "scheduley.db";

    public static Connection getConnection() throws SQLException {
        String dbPath = System.getProperty("scheduley.db.path", DEFAULT_DB_PATH);
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }
}
