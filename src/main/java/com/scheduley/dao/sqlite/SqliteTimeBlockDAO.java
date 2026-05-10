package com.scheduley.dao.sqlite;

import com.scheduley.dao.TimeBlockDAO;
import com.scheduley.db.ConnectDB;
import com.scheduley.models.TimeBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteTimeBlockDAO implements TimeBlockDAO {
    private static final String SQL_CREATE = """
            INSERT INTO time_block(schedule_profile_id, title, block_type, course_id, task_id, day_of_week, block_date, start_minute,
                                   end_minute, location_text, color_hex, notes, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String SQL_SELECT = """
            SELECT id, title, block_type, course_id, task_id, day_of_week, block_date, start_minute, end_minute,
                   location_text, color_hex, notes, created_at, updated_at
            FROM time_block
            """;
    private static final String SQL_UPDATE = """
            UPDATE time_block
            SET title = ?, block_type = ?, course_id = ?, task_id = ?, day_of_week = ?, block_date = ?,
                start_minute = ?, end_minute = ?, location_text = ?, color_hex = ?, notes = ?, updated_at = ?
            WHERE id = ? AND schedule_profile_id = ?
            """;

    @Override
    public TimeBlock create(TimeBlock timeBlock, Long scheduleProfileId) {
        requireProfileId(scheduleProfileId);
        String now = LocalDateTime.now().toString();
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_CREATE, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, scheduleProfileId);
            bindEditable(ps, timeBlock, 2);
            ps.setString(13, now);
            ps.setString(14, now);
            int rows = ps.executeUpdate();
            if (rows != 1) throw new SQLException("Insert failed, rows affected: " + rows);
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    timeBlock.setId(keys.getLong(1));
                    timeBlock.setCreatedAt(now);
                    timeBlock.setUpdatedAt(now);
                    return timeBlock;
                }
            }
            throw new SQLException("Insert succeeded but no generated key returned.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create time block: " + timeBlock.getTitle(), e);
        }
    }

    @Override
    public Optional<TimeBlock> findById(Long id, Long scheduleProfileId) {
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
            throw new RuntimeException("Failed to find time block id: " + id, e);
        }
    }

    @Override
    public List<TimeBlock> findAll(Long scheduleProfileId) {
        return query(SQL_SELECT + " WHERE schedule_profile_id = ? ORDER BY day_of_week, start_minute, title COLLATE NOCASE", scheduleProfileId);
    }

    @Override
    public List<TimeBlock> findByDayOfWeek(int dayOfWeek, Long scheduleProfileId) {
        return query(SQL_SELECT + " WHERE day_of_week = ? AND schedule_profile_id = ? ORDER BY start_minute", dayOfWeek, scheduleProfileId);
    }

    @Override
    public List<TimeBlock> findByCourseId(Long courseId, Long scheduleProfileId) {
        return query(SQL_SELECT + " WHERE course_id = ? AND schedule_profile_id = ? ORDER BY day_of_week, start_minute", courseId, scheduleProfileId);
    }

    @Override
    public List<TimeBlock> findByTaskId(Long taskId, Long scheduleProfileId) {
        return query(SQL_SELECT + " WHERE task_id = ? AND schedule_profile_id = ? ORDER BY day_of_week, start_minute", taskId, scheduleProfileId);
    }

    @Override
    public boolean update(TimeBlock timeBlock, Long scheduleProfileId) {
        requireProfileId(scheduleProfileId);
        if (timeBlock.getId() == null) {
            throw new IllegalArgumentException("Time block id is null. Can't update.");
        }
        String now = LocalDateTime.now().toString();
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            bindEditable(ps, timeBlock, 1);
            ps.setString(12, now);
            ps.setLong(13, timeBlock.getId());
            ps.setLong(14, scheduleProfileId);
            timeBlock.setUpdatedAt(now);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update time block: " + timeBlock.getTitle(), e);
        }
    }

    @Override
    public boolean deleteById(Long id, Long scheduleProfileId) {
        requireProfileId(scheduleProfileId);
        if (id == null) {
            throw new IllegalArgumentException("Time block id is null. Can't delete.");
        }
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM time_block WHERE id = ? AND schedule_profile_id = ?")) {
            ps.setLong(1, id);
            ps.setLong(2, scheduleProfileId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete time block id: " + id, e);
        }
    }

    @Override
    public List<TimeBlock> findConflicts(int dayOfWeek, int startMinute, int endMinute, Long excludeId, Long scheduleProfileId) {
        requireProfileId(scheduleProfileId);
        String sql = SQL_SELECT + """
                WHERE day_of_week = ?
                  AND schedule_profile_id = ?
                  AND start_minute < ?
                  AND end_minute > ?
                  AND (? IS NULL OR id != ?)
                ORDER BY start_minute
                """;
        List<TimeBlock> conflicts = new ArrayList<>();
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dayOfWeek);
            ps.setLong(2, scheduleProfileId);
            ps.setInt(3, endMinute);
            ps.setInt(4, startMinute);
            if (excludeId == null) {
                ps.setNull(5, java.sql.Types.INTEGER);
                ps.setNull(6, java.sql.Types.INTEGER);
            } else {
                ps.setLong(5, excludeId);
                ps.setLong(6, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    conflicts.add(map(rs));
                }
                return conflicts;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find time block conflicts", e);
        }
    }

    private List<TimeBlock> query(String sql, Object... params) {
        List<TimeBlock> blocks = new ArrayList<>();
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                Object param = params[i];
                if (param instanceof Integer value) {
                    ps.setInt(i + 1, value);
                } else if (param instanceof Long value) {
                    ps.setLong(i + 1, value);
                } else {
                    throw new IllegalArgumentException("Unsupported SQL parameter: " + param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    blocks.add(map(rs));
                }
                return blocks;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query time blocks", e);
        }
    }

    private void bindEditable(PreparedStatement ps, TimeBlock block, int startIndex) throws SQLException {
        ps.setString(startIndex, block.getTitle());
        ps.setString(startIndex + 1, block.getBlockType());
        setNullableLong(ps, startIndex + 2, block.getCourseId());
        setNullableLong(ps, startIndex + 3, block.getTaskId());
        setNullableInt(ps, startIndex + 4, block.getDayOfWeek());
        ps.setString(startIndex + 5, blankToNull(block.getBlockDate()));
        ps.setInt(startIndex + 6, block.getStartMinute());
        ps.setInt(startIndex + 7, block.getEndMinute());
        ps.setString(startIndex + 8, block.getLocationText());
        ps.setString(startIndex + 9, block.getColorHex());
        ps.setString(startIndex + 10, block.getNotes());
    }

    private TimeBlock map(ResultSet rs) throws SQLException {
        return new TimeBlock(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("block_type"),
                nullableLong(rs, "course_id"),
                nullableLong(rs, "task_id"),
                nullableInt(rs, "day_of_week"),
                rs.getString("block_date"),
                rs.getInt("start_minute"),
                rs.getInt("end_minute"),
                rs.getString("location_text"),
                rs.getString("color_hex"),
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
