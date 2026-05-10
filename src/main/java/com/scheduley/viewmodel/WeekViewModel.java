package com.scheduley.viewmodel;

import com.scheduley.dao.TimeBlockDAO;
import com.scheduley.models.TimeBlock;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.function.LongSupplier;

public class WeekViewModel {
    private final TimeBlockDAO timeBlockDAO;
    private final LongSupplier activeScheduleProfileId;
    private final ObservableList<TimeBlock> timeBlocks = FXCollections.observableArrayList();

    public WeekViewModel(TimeBlockDAO timeBlockDAO, LongSupplier activeScheduleProfileId) {
        this.timeBlockDAO = timeBlockDAO;
        this.activeScheduleProfileId = activeScheduleProfileId;
    }

    public ObservableList<TimeBlock> timeBlocks() {
        return timeBlocks;
    }

    public void reload() {
        timeBlocks.setAll(timeBlockDAO.findAll(activeScheduleProfileId.getAsLong()));
    }

    public TimeBlock save(TimeBlock block) {
        TimeBlock saved = block.getId() == null ? timeBlockDAO.create(block, activeScheduleProfileId.getAsLong()) : update(block);
        reload();
        return saved;
    }

    public void delete(TimeBlock block) {
        timeBlockDAO.deleteById(block.getId(), activeScheduleProfileId.getAsLong());
        reload();
    }

    public List<TimeBlock> conflictsFor(TimeBlock block) {
        return timeBlockDAO.findConflicts(
                block.getDayOfWeek(),
                block.getStartMinute(),
                block.getEndMinute(),
                block.getId(),
                activeScheduleProfileId.getAsLong()
        );
    }

    private TimeBlock update(TimeBlock block) {
        timeBlockDAO.update(block, activeScheduleProfileId.getAsLong());
        return block;
    }
}
