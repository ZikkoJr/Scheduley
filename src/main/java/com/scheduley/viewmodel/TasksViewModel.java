package com.scheduley.viewmodel;

import com.scheduley.dao.TaskDAO;
import com.scheduley.models.Task;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.function.LongSupplier;

public class TasksViewModel {
    private final TaskDAO taskDAO;
    private final LongSupplier activeScheduleProfileId;
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();

    public TasksViewModel(TaskDAO taskDAO, LongSupplier activeScheduleProfileId) {
        this.taskDAO = taskDAO;
        this.activeScheduleProfileId = activeScheduleProfileId;
    }

    public ObservableList<Task> tasks() {
        return tasks;
    }

    public void reload() {
        tasks.setAll(taskDAO.findAll(activeScheduleProfileId.getAsLong()));
    }

    public Task save(Task task) {
        Task saved = task.getId() == null ? taskDAO.create(task, activeScheduleProfileId.getAsLong()) : update(task);
        reload();
        return saved;
    }

    public void delete(Task task) {
        taskDAO.deleteById(task.getId(), activeScheduleProfileId.getAsLong());
        reload();
    }

    private Task update(Task task) {
        taskDAO.update(task, activeScheduleProfileId.getAsLong());
        return task;
    }
}
