package com.scheduley.dao.sqlite;

import com.scheduley.dao.ScheduleProfileDAO;
import com.scheduley.db.ConnectDB;
import com.scheduley.models.ScheduleProfile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteScheduleProfileDAO implements ScheduleProfileDAO {
    private static final String SQL_SELECT = """
            SELECT id, name, description, is_active, created_at, updated_at
            FROM schedule_profile
            """;

    @Override
    public ScheduleProfile create(ScheduleProfile profile) {
        String now = LocalDateTime.now().toString();
        String sql = """
                INSERT INTO schedule_profile(name, description, is_active, created_at, updated_at)
                VALUES (?, ?, 0, ?, ?)
                """;
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, profile.getName());
            ps.setString(2, blankToNull(profile.getDescription()));
            ps.setString(3, now);
            ps.setString(4, now);
            int rows = ps.executeUpdate();
            if (rows != 1) throw new SQLException("Insert failed, rows affected: " + rows);
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    profile.setId(keys.getLong(1));
                    profile.setActive(false);
                    profile.setCreatedAt(now);
                    profile.setUpdatedAt(now);
                    return profile;
                }
            }
            throw new SQLException("Insert succeeded but no generated key returned.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create schedule profile: " + profile.getName(), e);
        }
    }

    @Override
    public Optional<ScheduleProfile> findById(Long id) {
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT + " WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find schedule profile id: " + id, e);
        }
    }

    @Override
    public List<ScheduleProfile> findAll() {
        List<ScheduleProfile> profiles = new ArrayList<>();
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT + " ORDER BY name COLLATE NOCASE, id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                profiles.add(map(rs));
            }
            return profiles;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get schedule profiles", e);
        }
    }

    @Override
    public Optional<ScheduleProfile> findActive() {
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT + " WHERE is_active = 1 ORDER BY id LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return Optional.of(map(rs));
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find active schedule profile", e);
        }
    }

    @Override
    public boolean update(ScheduleProfile profile) {
        if (profile.getId() == null) {
            throw new IllegalArgumentException("Schedule profile id is null. Can't update.");
        }
        String now = LocalDateTime.now().toString();
        String sql = """
                UPDATE schedule_profile
                SET name = ?, description = ?, updated_at = ?
                WHERE id = ?
                """;
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, profile.getName());
            ps.setString(2, blankToNull(profile.getDescription()));
            ps.setString(3, now);
            ps.setLong(4, profile.getId());
            profile.setUpdatedAt(now);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update schedule profile: " + profile.getName(), e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Schedule profile id is null. Can't delete.");
        }

        try (Connection conn = ConnectDB.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int profileCount = countProfiles(conn);
                if (profileCount <= 1) {
                    throw new IllegalStateException("The final schedule profile cannot be deleted.");
                }
                boolean wasActive = isActive(conn, id);
                deleteOwnedData(conn, id);
                int deleted;
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM schedule_profile WHERE id = ?")) {
                    ps.setLong(1, id);
                    deleted = ps.executeUpdate();
                }
                if (wasActive && deleted == 1) {
                    Long nextId = firstProfileId(conn);
                    if (nextId != null) {
                        setActiveProfile(conn, nextId);
                    }
                }
                conn.commit();
                return deleted == 1;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete schedule profile id: " + id, e);
        }
    }

    @Override
    public boolean setActiveProfile(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Schedule profile id is null. Can't activate.");
        }
        try (Connection conn = ConnectDB.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean updated = setActiveProfile(conn, id);
                conn.commit();
                return updated;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to activate schedule profile id: " + id, e);
        }
    }

    @Override
    public ScheduleProfile ensureDefaultProfileExists() {
        Optional<ScheduleProfile> active = findActive();
        if (active.isPresent()) {
            return active.get();
        }

        List<ScheduleProfile> profiles = findAll();
        if (!profiles.isEmpty()) {
            ScheduleProfile first = profiles.getFirst();
            setActiveProfile(first.getId());
            return findById(first.getId()).orElse(first);
        }

        ScheduleProfile created = create(new ScheduleProfile("Default Schedule", "Imported MVP schedule data"));
        setActiveProfile(created.getId());
        assignExistingDataToDefaultProfile(created.getId());
        return findById(created.getId()).orElse(created);
    }

    @Override
    public void assignExistingDataToDefaultProfile(Long defaultProfileId) {
        if (defaultProfileId == null) {
            throw new IllegalArgumentException("Default schedule profile id is null.");
        }
        try (Connection conn = ConnectDB.getConnection()) {
            assignExistingDataToDefaultProfile(conn, defaultProfileId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to assign existing data to default schedule profile", e);
        }
    }

    private static boolean setActiveProfile(Connection conn, Long id) throws SQLException {
        try (PreparedStatement exists = conn.prepareStatement("SELECT id FROM schedule_profile WHERE id = ?")) {
            exists.setLong(1, id);
            try (ResultSet rs = exists.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE schedule_profile
                SET is_active = CASE WHEN id = ? THEN 1 ELSE 0 END,
                    updated_at = CASE WHEN id = ? THEN ? ELSE updated_at END
                """)) {
            String now = LocalDateTime.now().toString();
            ps.setLong(1, id);
            ps.setLong(2, id);
            ps.setString(3, now);
            ps.executeUpdate();
            return true;
        }
    }

    private static void assignExistingDataToDefaultProfile(Connection conn, Long defaultProfileId) throws SQLException {
        updateNullProfileId(conn, "course", defaultProfileId);
        updateNullProfileId(conn, "task", defaultProfileId);
        updateNullProfileId(conn, "time_block", defaultProfileId);
    }

    private static void updateNullProfileId(Connection conn, String table, Long defaultProfileId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE " + table + " SET schedule_profile_id = ? WHERE schedule_profile_id IS NULL")) {
            ps.setLong(1, defaultProfileId);
            ps.executeUpdate();
        }
    }

    private static void deleteOwnedData(Connection conn, Long id) throws SQLException {
        deleteByProfile(conn, "time_block", id);
        deleteByProfile(conn, "task", id);
        deleteByProfile(conn, "course", id);
    }

    private static void deleteByProfile(Connection conn, String table, Long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM " + table + " WHERE schedule_profile_id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private static int countProfiles(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM schedule_profile")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static boolean isActive(Connection conn, Long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT is_active FROM schedule_profile WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("is_active") == 1;
            }
        }
    }

    private static Long firstProfileId(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM schedule_profile ORDER BY id LIMIT 1")) {
            return rs.next() ? rs.getLong("id") : null;
        }
    }

    private static ScheduleProfile map(ResultSet rs) throws SQLException {
        return new ScheduleProfile(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("is_active") == 1,
                rs.getString("created_at"),
                rs.getString("updated_at")
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
