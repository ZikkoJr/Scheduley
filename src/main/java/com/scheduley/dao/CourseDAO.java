package com.scheduley.dao;

import com.scheduley.models.Course;

import java.util.List;
import java.util.Optional;

public interface CourseDAO {
    Course create(Course course);
    Optional<Course> findById(Long id);
    Optional<Course> findByCode(String code);
    List<Course> findAll();
    boolean update(Course course);
    boolean deleteById(Long id);
}
