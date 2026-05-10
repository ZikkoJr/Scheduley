package com.scheduley.dao;

import com.scheduley.models.Task;

import java.util.List;
import java.util.Optional;

public interface TaskDAO {
    Task create(Task task, Long scheduleProfileId);
    Optional<Task> findById(Long id, Long scheduleProfileId);
    List<Task> findAll(Long scheduleProfileId);
    List<Task> findByStatus(String status, Long scheduleProfileId);
    List<Task> findByCourseId(Long courseId, Long scheduleProfileId);
    boolean update(Task task, Long scheduleProfileId);
    boolean deleteById(Long id, Long scheduleProfileId);
}
