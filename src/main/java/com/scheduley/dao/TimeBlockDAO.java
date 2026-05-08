package com.scheduley.dao;

import com.scheduley.models.TimeBlock;

import java.util.List;
import java.util.Optional;

public interface TimeBlockDAO {
    TimeBlock create(TimeBlock timeBlock);
    Optional<TimeBlock> findById(Long id);
    List<TimeBlock> findAll();
    List<TimeBlock> findByDayOfWeek(int dayOfWeek);
    List<TimeBlock> findByCourseId(Long courseId);
    List<TimeBlock> findByTaskId(Long taskId);
    boolean update(TimeBlock timeBlock);
    boolean deleteById(Long id);
    List<TimeBlock> findConflicts(int dayOfWeek, int startMinute, int endMinute, Long excludeId);
}
