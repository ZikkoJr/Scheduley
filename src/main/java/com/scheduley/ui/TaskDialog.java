package com.scheduley.ui;

import com.scheduley.models.Course;
import com.scheduley.models.Task;
import com.scheduley.util.ValidationUtils;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.util.List;

class TaskDialog extends Dialog<Task> {
    TaskDialog(Task existing, List<Course> courses) {
        setTitle(existing == null ? "Add Task" : "Edit Task");
        setHeaderText(existing == null ? "Add a task" : "Edit task");
        UiUtils.applyTheme(getDialogPane());

        TextField title = new TextField(existing == null ? "" : value(existing.getTitle()));
        ComboBox<Course> course = new ComboBox<>();
        course.getItems().addAll(courses);
        course.setPromptText("No course");
        if (existing != null && existing.getCourseId() != null) {
            courses.stream()
                    .filter(c -> existing.getCourseId().equals(c.getId()))
                    .findFirst()
                    .ifPresent(course::setValue);
        }
        DatePicker dueDate = new DatePicker(existing == null || existing.getDueDate() == null
                ? null
                : LocalDate.parse(existing.getDueDate()));
        TextField estimated = new TextField(existing == null || existing.getEstimatedMinutes() == null
                ? ""
                : existing.getEstimatedMinutes().toString());
        ComboBox<String> priority = new ComboBox<>();
        priority.getItems().addAll("LOW", "MEDIUM", "HIGH");
        priority.setValue(existing == null ? "MEDIUM" : existing.getPriority());
        ComboBox<String> status = new ComboBox<>();
        status.getItems().addAll("NOT_STARTED", "IN_PROGRESS", "COMPLETE");
        status.setValue(existing == null ? "NOT_STARTED" : existing.getStatus());
        TextArea notes = new TextArea(existing == null ? "" : value(existing.getNotes()));
        notes.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));
        grid.addRow(0, label("Title"), title);
        grid.addRow(1, label("Course"), course);
        grid.addRow(2, label("Due Date"), dueDate);
        grid.addRow(3, label("Estimated Minutes"), estimated);
        grid.addRow(4, label("Priority"), priority);
        grid.addRow(5, label("Status"), status);
        grid.addRow(6, label("Notes"), notes);
        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        Node ok = getDialogPane().lookupButton(ButtonType.OK);
        if (ok instanceof Button button) {
            button.setText("Save");
            button.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                try {
                    ValidationUtils.validateTask(build(existing, title, course, dueDate, estimated, priority, status, notes));
                } catch (RuntimeException e) {
                    UiUtils.showError("Check task details", e);
                    event.consume();
                }
            });
        }

        setResultConverter(button -> button == ButtonType.OK
                ? build(existing, title, course, dueDate, estimated, priority, status, notes)
                : null);
    }

    private static Task build(Task existing, TextField title, ComboBox<Course> course, DatePicker dueDate,
                              TextField estimated, ComboBox<String> priority, ComboBox<String> status,
                              TextArea notes) {
        Integer minutes = estimated.getText() == null || estimated.getText().isBlank()
                ? null
                : Integer.parseInt(estimated.getText().trim());
        return new Task(
                existing == null ? null : existing.getId(),
                title.getText().trim(),
                course.getValue() == null ? null : course.getValue().getId(),
                dueDate.getValue() == null ? null : dueDate.getValue().toString(),
                minutes,
                priority.getValue(),
                status.getValue(),
                blankToNull(notes.getText()),
                existing == null ? null : existing.getCreatedAt(),
                existing == null ? null : existing.getUpdatedAt()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }

    private static Label label(String text) {
        return new Label(text);
    }
}
