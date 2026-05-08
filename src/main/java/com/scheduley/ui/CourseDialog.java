package com.scheduley.ui;

import com.scheduley.models.Course;
import com.scheduley.util.ValidationUtils;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

class CourseDialog extends Dialog<Course> {
    CourseDialog(Course existing) {
        setTitle(existing == null ? "Add Course" : "Edit Course");
        setHeaderText(existing == null ? "Add a course" : "Edit course");
        UiUtils.applyTheme(getDialogPane());

        TextField code = new TextField(existing == null ? "" : value(existing.getCode()));
        TextField name = new TextField(existing == null ? "" : value(existing.getName()));
        TextField instructor = new TextField(existing == null ? "" : value(existing.getInstructor()));
        TextField location = new TextField(existing == null ? "" : value(existing.getLocationText()));
        ComboBox<ColorChoice> color = ColorChoice.comboBox(existing == null ? null : existing.getColorHex(), "#3B82F6");
        TextArea notes = new TextArea(existing == null ? "" : value(existing.getNotes()));
        notes.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));
        grid.addRow(0, label("Code"), code);
        grid.addRow(1, label("Name"), name);
        grid.addRow(2, label("Instructor"), instructor);
        grid.addRow(3, label("Location"), location);
        grid.addRow(4, label("Color"), color);
        grid.addRow(5, label("Notes"), notes);
        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        Node ok = getDialogPane().lookupButton(ButtonType.OK);
        if (ok instanceof Button button) {
            button.setText("Save");
            button.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                try {
                    ValidationUtils.validateCourse(build(existing, code, name, instructor, location, color, notes));
                } catch (IllegalArgumentException e) {
                    UiUtils.showError("Check course details", e);
                    event.consume();
                }
            });
        }

        setResultConverter(button -> button == ButtonType.OK
                ? build(existing, code, name, instructor, location, color, notes)
                : null);
    }

    private static Course build(Course existing, TextField code, TextField name, TextField instructor,
                                TextField location, ComboBox<ColorChoice> color, TextArea notes) {
        Course course = new Course(
                existing == null ? null : existing.getId(),
                code.getText().trim(),
                name.getText().trim(),
                blankToNull(instructor.getText()),
                blankToNull(location.getText()),
                ColorChoice.selectedHex(color),
                blankToNull(notes.getText()),
                existing == null ? null : existing.getCreatedAt(),
                existing == null ? null : existing.getUpdatedAt()
        );
        return course;
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
