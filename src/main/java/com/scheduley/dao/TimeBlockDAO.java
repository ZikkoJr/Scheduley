package com.scheduley.dao;

import com.scheduley.models.BlockCategory;
import com.scheduley.models.TimeBlock;

import java.util.List;
import java.util.Optional;

public interface TimeBlockDAO {

    TimeBlock create(TimeBlock block);
    Optional<TimeBlock> getById(Long id);
    List<TimeBlock> getAll();
    List<TimeBlock> getByDay(int dayOfWeek);
    List<TimeBlock> getByCourseId(long courseId);
    List<TimeBlock> getByCategory(BlockCategory category);
    boolean update (TimeBlock block);
    boolean delete (Long id);
}
