package com.scheduley.ui;

import com.scheduley.models.Course;
import com.scheduley.models.Task;
import com.scheduley.util.ValidationUtils;
import com.scheduley.viewmodel.CoursesViewModel;
import com.scheduley.viewmodel.TasksViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.util.List;

public class TasksView extends BorderPane {
    private final TasksViewModel tasksViewModel;
    private final CoursesViewModel coursesViewModel;
    private final Runnable afterChange;
    private final TableView<Task> table = new TableView<>();

    public TasksView(TasksViewModel tasksViewModel, CoursesViewModel coursesViewModel, Runnable afterChange) {
        this.tasksViewModel = tasksViewModel;
        this.coursesViewModel = coursesViewModel;
        this.afterChange = afterChange;
        build();
    }

    private void build() {
        getStyleClass().add("content-view");
        setPadding(new Insets(16));
        Label title = new Label("Tasks");
        title.getStyleClass().add("section-title");
        Button add = new Button("Add");
        Button edit = new Button("Edit");
        Button complete = new Button("Complete");
        Button delete = new Button("Delete");
        HBox toolbar = new HBox(8, title, add, edit, complete, delete);
        toolbar.setPadding(new Insets(0, 0, 12, 0));

        TableColumn<Task, String> taskTitle = new TableColumn<>("Title");
        taskTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        TableColumn<Task, String> course = new TableColumn<>("Course");
        course.setCellValueFactory(data -> new SimpleStringProperty(courseName(data.getValue().getCourseId())));
        TableColumn<Task, String> due = new TableColumn<>("Due");
        due.setCellValueFactory(data -> new SimpleStringProperty(nullToEmpty(data.getValue().getDueDate())));
        TableColumn<Task, String> priority = new TableColumn<>("Priority");
        priority.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPriority()));
        TableColumn<Task, String> status = new TableColumn<>("Status");
        status.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        table.getColumns().addAll(taskTitle, course, due, priority, status);
        table.setItems(tasksViewModel.tasks());
        table.setPlaceholder(new Label("No tasks yet. Add a task to keep this schedule on track."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        add.setOnAction(event -> openDialog(null));
        edit.setOnAction(event -> {
            Task selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) openDialog(selected);
        });
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && table.getSelectionModel().getSelectedItem() != null) {
                openDialog(table.getSelectionModel().getSelectedItem());
            }
        });
        complete.setOnAction(event -> markComplete());
        delete.setOnAction(event -> deleteSelected());

        setTop(toolbar);
        setCenter(table);
    }

    private void openDialog(Task existing) {
        List<Course> courses = List.copyOf(coursesViewModel.courses());
        new TaskDialog(existing, courses).showAndWait().ifPresent(task -> {
            try {
                ValidationUtils.validateTask(task);
                tasksViewModel.save(task);
                afterChange.run();
            } catch (RuntimeException e) {
                UiUtils.showError("Could not save task", e);
            }
        });
    }

    private void markComplete() {
        Task selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        selected.setStatus("COMPLETE");
        try {
            tasksViewModel.save(selected);
            afterChange.run();
        } catch (RuntimeException e) {
            UiUtils.showError("Could not complete task", e);
        }
    }

    private void deleteSelected() {
        Task selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (!UiUtils.confirm("Delete task", "Delete " + selected.getTitle() + "?")) return;
        try {
            tasksViewModel.delete(selected);
            afterChange.run();
        } catch (RuntimeException e) {
            UiUtils.showError("Could not delete task", e);
        }
    }

    private String courseName(Long courseId) {
        if (courseId == null) return "";
        return coursesViewModel.courses().stream()
                .filter(course -> courseId.equals(course.getId()))
                .findFirst()
                .map(Course::displayName)
                .orElse("");
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
