package com.scheduley.dao;

import com.scheduley.models.Task;

import java.util.List;
import java.util.Optional;

public interface TaskDAO {
    Task create(Task task);
    Optional<Task> findById(Long id);
    List<Task> findAll();
    List<Task> findByStatus(String status);
    List<Task> findByCourseId(Long courseId);
    boolean update(Task task);
    boolean deleteById(Long id);
}
