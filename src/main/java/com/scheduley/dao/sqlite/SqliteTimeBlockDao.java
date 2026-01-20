package com.scheduley.dao.sqlite;

import com.scheduley.dao.TimeBlockDAO;
import com.scheduley.db.ConnectDB;
import com.scheduley.models.BlockCategory;
import com.scheduley.models.Course;
import com.scheduley.models.TimeBlock;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteTimeBlockDao implements TimeBlockDAO {

    // Field: DB connection helper
    private final ConnectDB db;

    // Fields: SQL strings
    String SQL_Create = "INSERT INTO time_block(title, category, course_id, day_of_week," +
            "            start_min, end_min, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";
    String SQL_GetById = "SELECT id, title, category, course_id, day_of_week, start_min, end_min, notes FROM time_block WHERE id = ?";
    String SQL_GetAll = "SELECT id, title, category, course_id, day_of_week, start_min, end_min,"+
            "            notes FROM time_block ORDER BY day_of_week, start_min";
    String SQL_GetByDay = "SELECT id, title, category, course_id, day_of_week, start_min, end_min," +
            "              notes FROM time_block WHERE day_of_week = ? ORDER BY start_min";
    String SQL_GetByCourseId = "SELECT id, title, category, course_id, day_of_week, start_min, end_min," +
            "              notes FROM time_block WHERE course_id = ? ORDER BY day_of_week, start_min";
    String SQL_GetByCategory = "SELECT id, title, category, course_id, day_of_week, start_min, end_min," +
            "              notes FROM time_block WHERE category = ? ORDER BY day_of_week, start_min";
    String SQL_Update = "UPDATE time_block SET title = ?, category = ?, course_id = ?, day_of_week = ?,"
        +"               start_min = ?, end_min = ?, notes = ? WHERE id = ?";
    String SQL_Delete = "DELETE FROM time_block WHERE id = ?";

    //Constructor:
    public SqliteTimeBlockDao(ConnectDB db) {
        this.db = db;
    }

   @Override
    public TimeBlock create(TimeBlock block){

        String sql =  SQL_Create;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, block.getTitle());
            ps.setString(2, block.getCategory().name());

            if (block.getCourseId() == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            }else{
                ps.setLong(3, block.getCourseId());
            }

            ps.setInt(4, block.getDayOfWeek());
            ps.setInt(5, block.getStartMin());
            ps.setInt(6, block.getEndMin());
            ps.setString(7, block.getNotes());

            int rows = ps.executeUpdate();
            if (rows != 1) throw new SQLException("Insert failed, rows affected: " + rows);

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()){
                    long id = keys.getLong(1);
                    block.setId(id); // update the object in memory
                    return block;
                }
            }

            throw new SQLException("Insert succeeded but no generated key returned.");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create time block: " + block, e);
        }



    }

   @Override
    public Optional<TimeBlock> getById(Long id){
       if (id == null)
           throw new IllegalArgumentException("Time block id is null. Can't get time block with null id.");

       String sql = SQL_GetById;

       try(Connection conn = db.getConnection();
           PreparedStatement ps = conn.prepareStatement(sql)){

           ps.setLong(1, id);

           try (ResultSet rs = ps.executeQuery()) {
               if (rs.next()) return Optional.of(map(rs));
               return Optional.empty();
           }
       }catch (SQLException e) {
           throw new RuntimeException("Failed to get time block by id: " + id, e);
       }
   }

    @Override
    public List<TimeBlock> getAll(){
        String sql = SQL_GetAll;

        List<TimeBlock> block = new ArrayList<>();

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                block.add(map(rs));
            }
            return block;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to get all time blocks", e);
        }
    }

    @Override
    public List<TimeBlock> getByDay(int dayOfWeek) {
        String sql = SQL_GetByDay;
        List<TimeBlock> blocks = new ArrayList<>();

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, dayOfWeek);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    blocks.add(map(rs));
                }
            }
            return blocks;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to get list of time blocks by day: " + dayOfWeek, e);
        }
    }

    @Override
    public List<TimeBlock> getByCourseId(long CourseId) {
        String sql = SQL_GetByCourseId;
        List<TimeBlock> blocks = new ArrayList<>();

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, CourseId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    blocks.add(map(rs));
                }
            }
            return blocks;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to get list of time blocks by Course ID: " + CourseId, e);
        }
    }

    @Override
    public List<TimeBlock> getByCategory(BlockCategory category) {
        String sql = SQL_GetByCategory;
        List<TimeBlock> blocks = new ArrayList<>();

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, category.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    blocks.add(map(rs));
                }
            }
            return blocks;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to get list of time blocks by Course ID: " + category.name(), e);
        }
    }

    @Override
    public boolean update(TimeBlock block){
        if (block.getId() == null)
            throw new IllegalArgumentException("Time block id is null. Can't update.");

        String sql = SQL_Update;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)){

            ps.setString(1, block.getTitle());
            ps.setString(2, block.getCategory().name());

            if (block.getCourseId() == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            }else{
                ps.setLong(3, block.getCourseId());
            }

            ps.setInt(4, block.getDayOfWeek());
            ps.setInt(5, block.getStartMin());
            ps.setInt(6, block.getEndMin());
            ps.setString(7, block.getNotes());
            ps.setLong(8, block.getId());

            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update time block: " + block, e);
        }
    }

    @Override
    public boolean delete(Long id){
        if (id == null)
            throw new IllegalArgumentException("Time block id is null. Can't delete.");

        String sql = SQL_Delete;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)){

            ps.setLong(1, id);

            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete Time block, id: " + id, e);
        }
    }


    private TimeBlock map(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String title = rs.getString("title");

        BlockCategory category = BlockCategory.valueOf(rs.getString("category"));

        Long courseId = (Long) rs.getObject("course_id"); // null-safe

        int dayOfWeek = rs.getInt("day_of_week");
        int startMin = rs.getInt("start_min");
        int endMin = rs.getInt("end_min");

        String notes = rs.getString("notes"); // rs.getString returns null if NULL

        return new TimeBlock(id, title, category, courseId, dayOfWeek, startMin, endMin, notes);
    }



}
