package com.scheduley.viewmodel;

import com.scheduley.dao.TimeBlockDAO;
import com.scheduley.models.TimeBlock;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public class WeekViewModel {
    private final TimeBlockDAO timeBlockDAO;
    private final ObservableList<TimeBlock> timeBlocks = FXCollections.observableArrayList();

    public WeekViewModel(TimeBlockDAO timeBlockDAO) {
        this.timeBlockDAO = timeBlockDAO;
    }

    public ObservableList<TimeBlock> timeBlocks() {
        return timeBlocks;
    }

    public void reload() {
        timeBlocks.setAll(timeBlockDAO.findAll());
    }

    public TimeBlock save(TimeBlock block) {
        TimeBlock saved = block.getId() == null ? timeBlockDAO.create(block) : update(block);
        reload();
        return saved;
    }

    public void delete(TimeBlock block) {
        timeBlockDAO.deleteById(block.getId());
        reload();
    }

    public List<TimeBlock> conflictsFor(TimeBlock block) {
        return timeBlockDAO.findConflicts(
                block.getDayOfWeek(),
                block.getStartMinute(),
                block.getEndMinute(),
                block.getId()
        );
    }

    private TimeBlock update(TimeBlock block) {
        timeBlockDAO.update(block);
        return block;
    }
}
