package com.scheduley.dao;

import com.scheduley.models.TimeBlock;

import java.util.List;
import java.util.Optional;

public interface TimeBlockDAO {
    TimeBlock create(TimeBlock timeBlock, Long scheduleProfileId);
    Optional<TimeBlock> findById(Long id, Long scheduleProfileId);
    List<TimeBlock> findAll(Long scheduleProfileId);
    List<TimeBlock> findByDayOfWeek(int dayOfWeek, Long scheduleProfileId);
    List<TimeBlock> findByCourseId(Long courseId, Long scheduleProfileId);
    List<TimeBlock> findByTaskId(Long taskId, Long scheduleProfileId);
    boolean update(TimeBlock timeBlock, Long scheduleProfileId);
    boolean deleteById(Long id, Long scheduleProfileId);
    List<TimeBlock> findConflicts(int dayOfWeek, int startMinute, int endMinute, Long excludeId, Long scheduleProfileId);
}
