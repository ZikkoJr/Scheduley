package com.scheduley.dao.sqlite;

import com.scheduley.dao.SettingsDAO;
import com.scheduley.db.ConnectDB;
import com.scheduley.models.AppSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SqliteSettingsDAO implements SettingsDAO {
    @Override
    public AppSettings getSettings() {
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     SELECT id, week_start_day, calendar_start_hour, calendar_end_hour, theme
                     FROM app_settings
                     WHERE id = 1
                     """);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new AppSettings(
                        rs.getInt("id"),
                        rs.getInt("week_start_day"),
                        rs.getInt("calendar_start_hour"),
                        rs.getInt("calendar_end_hour"),
                        rs.getString("theme")
                );
            }
            return new AppSettings();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load settings", e);
        }
    }

    @Override
    public boolean updateSettings(AppSettings settings) {
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     INSERT INTO app_settings(id, week_start_day, calendar_start_hour, calendar_end_hour, theme)
                     VALUES (1, ?, ?, ?, ?)
                     ON CONFLICT(id) DO UPDATE SET
                         week_start_day = excluded.week_start_day,
                         calendar_start_hour = excluded.calendar_start_hour,
                         calendar_end_hour = excluded.calendar_end_hour,
                         theme = excluded.theme
                     """)) {
            ps.setInt(1, settings.getWeekStartDay());
            ps.setInt(2, settings.getCalendarStartHour());
            ps.setInt(3, settings.getCalendarEndHour());
            ps.setString(4, settings.getTheme());
            return ps.executeUpdate() >= 1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save settings", e);
        }
    }
}
