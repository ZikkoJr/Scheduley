package com.scheduley.ui;

import com.scheduley.models.Course;
import com.scheduley.models.Task;
import com.scheduley.models.TimeBlock;
import com.scheduley.util.TimeUtils;
import com.scheduley.util.ValidationUtils;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.List;

class TimeBlockDialog extends Dialog<TimeBlock> {
    static final ButtonType DELETE = new ButtonType("Delete", ButtonBar.ButtonData.LEFT);
    static final ButtonType DUPLICATE = new ButtonType("Save as Copy", ButtonBar.ButtonData.APPLY);
    private boolean deleteRequested;
    private TimeBlock duplicatedBlock;

    TimeBlockDialog(TimeBlock existing, List<Course> courses, List<Task> tasks) {
        setTitle(existing == null ? "Add Time Block" : "Edit Time Block");
        setHeaderText(existing == null ? "Add something to the week" : "Edit time block");
        UiUtils.applyTheme(getDialogPane());

        TextField title = new TextField(existing == null ? "" : value(existing.getTitle()));
        ComboBox<String> type = new ComboBox<>();
        type.getItems().addAll("COURSE", "WORK", "STUDY", "TASK", "CUSTOM");
        type.setValue(existing == null ? "CUSTOM" : existing.getBlockType());
        ComboBox<String> day = new ComboBox<>();
        day.getItems().addAll("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
        day.setValue(existing == null || existing.getDayOfWeek() == null
                ? "Monday"
                : TimeUtils.dayName(existing.getDayOfWeek()));
        TextField start = new TextField(existing == null ? "9:00 AM" : TimeUtils.formatMinute(existing.getStartMinute()));
        TextField end = new TextField(existing == null ? "10:00 AM" : TimeUtils.formatMinute(existing.getEndMinute()));
        ComboBox<Course> course = new ComboBox<>();
        course.getItems().addAll(courses);
        course.setPromptText("No course");
        ComboBox<Task> task = new ComboBox<>();
        task.getItems().addAll(tasks);
        task.setPromptText("No task");
        if (existing != null && existing.getCourseId() != null) {
            courses.stream().filter(c -> existing.getCourseId().equals(c.getId())).findFirst().ifPresent(course::setValue);
        }
        if (existing != null && existing.getTaskId() != null) {
            tasks.stream().filter(t -> existing.getTaskId().equals(t.getId())).findFirst().ifPresent(task::setValue);
        }
        TextField location = new TextField(existing == null ? "" : value(existing.getLocationText()));
        ComboBox<ColorChoice> color = ColorChoice.comboBox(
                existing == null ? null : existing.getColorHex(),
                defaultColor(type.getValue())
        );
        TextArea notes = new TextArea(existing == null ? "" : value(existing.getNotes()));
        notes.setPrefRowCount(3);

        type.valueProperty().addListener((obs, old, next) -> {
            String selectedHex = ColorChoice.selectedHex(color);
            if (selectedHex == null || selectedHex.isBlank() || selectedHex.equalsIgnoreCase(defaultColor(old))) {
                ColorChoice.selectHex(color, defaultColor(next));
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));
        grid.addRow(0, label("Title"), title);
        grid.addRow(1, label("Type"), type);
        grid.addRow(2, label("Day"), day);
        grid.addRow(3, label("Start"), start);
        grid.addRow(4, label("End"), end);
        grid.addRow(5, label("Course"), course);
        grid.addRow(6, label("Task"), task);
        grid.addRow(7, label("Location"), location);
        grid.addRow(8, label("Color"), color);
        grid.addRow(9, label("Notes"), notes);
        getDialogPane().setContent(grid);
        if (existing == null) {
            getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        } else {
            getDialogPane().getButtonTypes().addAll(DELETE, DUPLICATE, ButtonType.CANCEL, ButtonType.OK);
        }

        Node ok = getDialogPane().lookupButton(ButtonType.OK);
        if (ok instanceof Button button) {
            button.setText("Save");
            button.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                try {
                    ValidationUtils.validateTimeBlock(build(existing, title, type, day, start, end, course, task, location, color, notes));
                } catch (RuntimeException e) {
                    UiUtils.showError("Check time block details", e);
                    event.consume();
                }
            });
        }
        Node delete = getDialogPane().lookupButton(DELETE);
        if (delete instanceof Button button) {
            button.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                deleteRequested = true;
                close();
            });
        }
        Node duplicate = getDialogPane().lookupButton(DUPLICATE);
        if (duplicate instanceof Button button) {
            button.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                event.consume();
                try {
                    TimeBlock copy = build(null, title, type, day, start, end, course, task, location, color, notes);
                    ValidationUtils.validateTimeBlock(copy);
                    duplicatedBlock = copy;
                    close();
                } catch (RuntimeException e) {
                    UiUtils.showError("Check copy details", e);
                }
            });
        }

        setResultConverter(button -> button == ButtonType.OK
                ? build(existing, title, type, day, start, end, course, task, location, color, notes)
                : null);
    }

    boolean isDeleteRequested() {
        return deleteRequested;
    }

    TimeBlock getDuplicatedBlock() {
        return duplicatedBlock;
    }

    private static TimeBlock build(TimeBlock existing, TextField title, ComboBox<String> type, ComboBox<String> day,
                                   TextField start, TextField end, ComboBox<Course> course, ComboBox<Task> task,
                                   TextField location, ComboBox<ColorChoice> color, TextArea notes) {
        return new TimeBlock(
                existing == null ? null : existing.getId(),
                title.getText().trim(),
                type.getValue(),
                course.getValue() == null ? null : course.getValue().getId(),
                task.getValue() == null ? null : task.getValue().getId(),
                TimeUtils.dayNumber(day.getValue()),
                null,
                TimeUtils.parseTimeToMinute(start.getText()),
                TimeUtils.parseTimeToMinute(end.getText()),
                blankToNull(location.getText()),
                ColorChoice.selectedHex(color),
                blankToNull(notes.getText()),
                existing == null ? null : existing.getCreatedAt(),
                existing == null ? null : existing.getUpdatedAt()
        );
    }

    private static String defaultColor(String type) {
        return switch (type) {
            case "COURSE" -> "#3B82F6";
            case "WORK" -> "#22C55E";
            case "STUDY" -> "#8B5CF6";
            case "TASK" -> "#F97316";
            default -> "#6B7280";
        };
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
