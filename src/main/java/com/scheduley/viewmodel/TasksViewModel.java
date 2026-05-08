package com.scheduley.viewmodel;

import com.scheduley.dao.TaskDAO;
import com.scheduley.models.Task;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class TasksViewModel {
    private final TaskDAO taskDAO;
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();

    public TasksViewModel(TaskDAO taskDAO) {
        this.taskDAO = taskDAO;
    }

    public ObservableList<Task> tasks() {
        return tasks;
    }

    public void reload() {
        tasks.setAll(taskDAO.findAll());
    }

    public Task save(Task task) {
        Task saved = task.getId() == null ? taskDAO.create(task) : update(task);
        reload();
        return saved;
    }

    public void delete(Task task) {
        taskDAO.deleteById(task.getId());
        reload();
    }

    private Task update(Task task) {
        taskDAO.update(task);
        return task;
    }
}
