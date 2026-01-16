package com.scheduley.dao;

import com.scheduley.models.Course;

import java.util.List;
import java.util.Optional;

public interface CourseDAO {

    Course create(Course course);
    Optional<Course> getById(Long id);
    Optional<Course> getByCode(String code);
    List<Course> getAll();
    boolean update(Course course);
    boolean delete(Long id);

}
