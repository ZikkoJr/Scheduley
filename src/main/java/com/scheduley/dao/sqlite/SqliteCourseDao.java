package com.scheduley.dao.sqlite;

import com.scheduley.dao.CourseDAO;
import com.scheduley.db.ConnectDB;
import com.scheduley.models.Course;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteCourseDao implements CourseDAO {
    private static final String SQL_CREATE = """
            INSERT INTO course(schedule_profile_id, code, name, instructor, location_text, color_hex, notes, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String SQL_SELECT = """
            SELECT id, code, name, instructor, location_text, color_hex, notes, created_at, updated_at
            FROM course
            """;
    private static final String SQL_UPDATE = """
            UPDATE course
            SET code = ?, name = ?, instructor = ?, location_text = ?, color_hex = ?, notes = ?, updated_at = ?
            WHERE id = ? AND schedule_profile_id = ?
            """;
    private static final String SQL_DELETE = "DELETE FROM course WHERE id = ? AND schedule_profile_id = ?";

    @Override
    public Course create(Course course, Long scheduleProfileId) {
        requireProfileId(scheduleProfileId);
        String now = LocalDateTime.now().toString();
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_CREATE, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, scheduleProfileId);
            ps.setString(2, course.getCode());
            ps.setString(3, course.getName());
            ps.setString(4, course.getInstructor());
            ps.setString(5, course.getLocationText());
            ps.setString(6, course.getColorHex());
            ps.setString(7, course.getNotes());
            ps.setString(8, now);
            ps.setString(9, now);
            int rows = ps.executeUpdate();
            if (rows != 1) throw new SQLException("Insert failed, rows affected: " + rows);

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    course.setId(keys.getLong(1));
                    course.setCreatedAt(now);
                    course.setUpdatedAt(now);
                    return course;
                }
            }
            throw new SQLException("Insert succeeded but no generated key returned.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create course: " + course, e);
        }
    }

    @Override
    public Optional<Course> findById(Long id, Long scheduleProfileId) {
        requireProfileId(scheduleProfileId);
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT + " WHERE id = ? AND schedule_profile_id = ?")) {
            ps.setLong(1, id);
            ps.setLong(2, scheduleProfileId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get course by id: " + id, e);
        }
    }

    @Override
    public Optional<Course> findByCode(String code, Long scheduleProfileId) {
        requireProfileId(scheduleProfileId);
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT + " WHERE code = ? AND schedule_profile_id = ?")) {
            ps.setString(1, code);
            ps.setLong(2, scheduleProfileId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get course by code: " + code, e);
        }
    }

    @Override
    public List<Course> findAll(Long scheduleProfileId) {
        requireProfileId(scheduleProfileId);
        List<Course> courses = new ArrayList<>();
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT + " WHERE schedule_profile_id = ? ORDER BY code COLLATE NOCASE, name COLLATE NOCASE")) {
            ps.setLong(1, scheduleProfileId);
            try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                courses.add(map(rs));
            }
            return courses;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get all courses", e);
        }
    }

    @Override
    public boolean update(Course course, Long scheduleProfileId) {
        requireProfileId(scheduleProfileId);
        if (course.getId() == null)
            throw new IllegalArgumentException("Course id is null. Can't update.");

        String now = LocalDateTime.now().toString();
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, course.getCode());
            ps.setString(2, course.getName());
            ps.setString(3, course.getInstructor());
            ps.setString(4, course.getLocationText());
            ps.setString(5, course.getColorHex());
            ps.setString(6, course.getNotes());
            ps.setString(7, now);
            ps.setLong(8, course.getId());
            ps.setLong(9, scheduleProfileId);
            course.setUpdatedAt(now);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update course: " + course, e);
        }
    }

    @Override
    public boolean deleteById(Long id, Long scheduleProfileId) {
        requireProfileId(scheduleProfileId);
        if (id == null)
            throw new IllegalArgumentException("Course id is null. Can't delete.");

        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setLong(1, id);
            ps.setLong(2, scheduleProfileId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete course, id: " + id, e);
        }
    }

    private Course map(ResultSet rs) throws SQLException {
        return new Course(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("instructor"),
                rs.getString("location_text"),
                rs.getString("color_hex"),
                rs.getString("notes"),
                rs.getString("created_at"),
                rs.getString("updated_at")
        );
    }

    private static void requireProfileId(Long scheduleProfileId) {
        if (scheduleProfileId == null) {
            throw new IllegalStateException("No active schedule profile is selected.");
        }
    }
}
