package com.scheduley.dao;

import com.scheduley.models.Course;

import java.util.List;
import java.util.Optional;

public interface CourseDAO {
    Course create(Course course, Long scheduleProfileId);
    Optional<Course> findById(Long id, Long scheduleProfileId);
    Optional<Course> findByCode(String code, Long scheduleProfileId);
    List<Course> findAll(Long scheduleProfileId);
    boolean update(Course course, Long scheduleProfileId);
    boolean deleteById(Long id, Long scheduleProfileId);
}
