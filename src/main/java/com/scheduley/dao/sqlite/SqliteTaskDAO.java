package com.scheduley.dao.sqlite;

import com.scheduley.dao.TaskDAO;
import com.scheduley.db.ConnectDB;
import com.scheduley.models.Task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteTaskDAO implements TaskDAO {
    private static final String SQL_CREATE = """
            INSERT INTO task(schedule_profile_id, title, course_id, due_date, estimated_minutes, priority, status, notes, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String SQL_SELECT = """
            SELECT id, title, course_id, due_date, estimated_minutes, priority, status, notes, created_at, updated_at
            FROM task
            """;
    private static final String SQL_UPDATE = """
            UPDATE task
            SET title = ?, course_id = ?, due_date = ?, estimated_minutes = ?, priority = ?, status = ?,
                notes = ?, updated_at = ?
            WHERE id = ? AND schedule_profile_id = ?
            """;

    @Override
    public Task create(Task task, Long scheduleProfileId) {
        requireProfileId(scheduleProfileId);
        String now = LocalDateTime.now().toString();
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_CREATE, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, scheduleProfileId);
            bindEditable(ps, task, 2);
            ps.setString(9, now);
            ps.setString(10, now);
            int rows = ps.executeUpdate();
            if (rows != 1) throw new SQLException("Insert failed, rows affected: " + rows);
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    task.setId(keys.getLong(1));
                    task.setCreatedAt(now);
                    task.setUpdatedAt(now);
                    return task;
                }
            }
            throw new SQLException("Insert succeeded but no generated key returned.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create task: " + task.getTitle(), e);
        }
    }

    @Override
    public Optional<Task> findById(Long id, Long scheduleProfileId) {
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
            throw new RuntimeException("Failed to find task by id: " + id, e);
        }
    }

    @Override
    public List<Task> findAll(Long scheduleProfileId) {
        return query(SQL_SELECT + """
                WHERE schedule_profile_id = ?
                ORDER BY
                  CASE WHEN due_date IS NULL OR due_date = '' THEN 1 ELSE 0 END,
                  due_date,
                  id DESC
                """, scheduleProfileId);
    }

    @Override
    public List<Task> findByStatus(String status, Long scheduleProfileId) {
        return query(SQL_SELECT + " WHERE status = ? AND schedule_profile_id = ? ORDER BY due_date, id DESC", status, scheduleProfileId);
    }

    @Override
    public List<Task> findByCourseId(Long courseId, Long scheduleProfileId) {
        return query(SQL_SELECT + " WHERE course_id = ? AND schedule_profile_id = ? ORDER BY due_date, id DESC", courseId, scheduleProfileId);
    }

    @Override
    public boolean update(Task task, Long scheduleProfileId) {
        requireProfileId(scheduleProfileId);
        if (task.getId() == null) {
            throw new IllegalArgumentException("Task id is null. Can't update.");
        }
        String now = LocalDateTime.now().toString();
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            bindEditable(ps, task, 1);
            ps.setString(8, now);
            ps.setLong(9, task.getId());
            ps.setLong(10, scheduleProfileId);
            task.setUpdatedAt(now);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update task: " + task.getTitle(), e);
        }
    }

    @Override
    public boolean deleteById(Long id, Long scheduleProfileId) {
        requireProfileId(scheduleProfileId);
        if (id == null) {
            throw new IllegalArgumentException("Task id is null. Can't delete.");
        }
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM task WHERE id = ? AND schedule_profile_id = ?")) {
            ps.setLong(1, id);
            ps.setLong(2, scheduleProfileId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete task id: " + id, e);
        }
    }

    private List<Task> query(String sql, Object... params) {
        List<Task> tasks = new ArrayList<>();
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                Object param = params[i];
                if (param instanceof String value) {
                    ps.setString(i + 1, value);
                } else if (param instanceof Long value) {
                    ps.setLong(i + 1, value);
                } else {
                    throw new IllegalArgumentException("Unsupported SQL parameter: " + param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tasks.add(map(rs));
                }
                return tasks;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query tasks", e);
        }
    }

    private void bindEditable(PreparedStatement ps, Task task, int startIndex) throws SQLException {
        ps.setString(startIndex, task.getTitle());
        setNullableLong(ps, startIndex + 1, task.getCourseId());
        ps.setString(startIndex + 2, blankToNull(task.getDueDate()));
        setNullableInt(ps, startIndex + 3, task.getEstimatedMinutes());
        ps.setString(startIndex + 4, task.getPriority());
        ps.setString(startIndex + 5, task.getStatus());
        ps.setString(startIndex + 6, task.getNotes());
    }

    private Task map(ResultSet rs) throws SQLException {
        return new Task(
                rs.getLong("id"),
                rs.getString("title"),
                nullableLong(rs, "course_id"),
                rs.getString("due_date"),
                nullableInt(rs, "estimated_minutes"),
                rs.getString("priority"),
                rs.getString("status"),
                rs.getString("notes"),
                rs.getString("created_at"),
                rs.getString("updated_at")
        );
    }

    private static void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) ps.setNull(index, java.sql.Types.INTEGER);
        else ps.setLong(index, value);
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) ps.setNull(index, java.sql.Types.INTEGER);
        else ps.setInt(index, value);
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static void requireProfileId(Long scheduleProfileId) {
        if (scheduleProfileId == null) {
            throw new IllegalStateException("No active schedule profile is selected.");
        }
    }
}
