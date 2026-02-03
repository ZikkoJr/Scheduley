package com.scheduley.dao.sqlite;

import com.scheduley.dao.CourseDAO;
import com.scheduley.models.Course;
import com.scheduley.db.ConnectDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteCourseDao implements CourseDAO {

    // Field: DB connection helper
    private final ConnectDB db;

    // Fields: SQL strings
    String SQL_Create = "INSERT INTO course(code, name, credits, colour_hex) VALUES (?, ?, ?, ?)";
    String SQL_GetById = "SELECT id, code, name, credits, colour_hex FROM course WHERE id = ?";
    String SQL_GetByCode = "SELECT id, code, name, credits, colour_hex FROM course WHERE code = ?";
    String SQL_GetAll = "SELECT id, code, name, credits, colour_hex FROM course ORDER BY id";
    String SQL_Update = "UPDATE course SET code = ?, name = ?, credits = ?, colour_hex = ? WHERE id = ?";
    String SQL_Delete = "DELETE FROM course WHERE id = ?";

    //constructor
    public SqliteCourseDao(ConnectDB db) {
        this.db = db;
    }

    @Override
    public Course create(Course course) {

        String sql = SQL_Create;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, course.getCode());
            ps.setString(2, course.getName());
            ps.setInt(3, course.getCredits());
            ps.setString(4, course.getColourHex());

            int rows = ps.executeUpdate();
            if (rows != 1) throw new SQLException("Insert failed, rows affected: " + rows);

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()){
                    long id = keys.getLong(1);
                    course.setId(id); // update the object in memory
                    return course;
                }
            }

            throw new SQLException("Insert succeeded but no generated key returned.");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create course: " + course, e);
        }


    }

    @Override
    public Optional<Course> getById(Long id){
        String sql = SQL_GetById;

        try(Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to get course by id: " + id, e);
        }
    }

    @Override
    public Optional<Course> getByCode(String code){
        String sql = SQL_GetByCode;

        try (Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, code);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        } catch (SQLException e){
            throw new RuntimeException("Failed to get course by code: " + code, e);
        }
    }

    @Override
    public List<Course> getAll(){
        String sql = SQL_GetAll;

        List<Course> courses = new ArrayList<>();

        try (Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                courses.add(map(rs));
            }
            return courses;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to get all courses", e);
        }
    }

    @Override
    public boolean update(Course course){
        if (course.getId() == null)
            throw new IllegalArgumentException("Course id is null. Can't update.");

        String sql = SQL_Update;

        try (Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)){

            ps.setString(1, course.getCode());
            ps.setString(2, course.getName());
            ps.setInt(3, course.getCredits());
            ps.setString(4, course.getColourHex());
            ps.setLong(5, course.getId());

            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update course: " + course, e);
        }
    }

    @Override
    public boolean delete(Long id){
        if (id == null)
            throw new IllegalArgumentException("Course id is null. Can't delete.");

        String sql = SQL_Delete;

        try (Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)){

            ps.setLong(1, id);

            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete course, id: " + id, e);
        }
    }

    private Course map(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String code = rs.getString("code");
        String name = rs.getString("name");
        int credits = rs.getInt("credits");
        String colourHex = rs.getString("colour_hex");
        return new Course(id, code, name, credits, colourHex);
    }


}
