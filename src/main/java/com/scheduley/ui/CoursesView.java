package com.scheduley.ui;

import com.scheduley.models.Course;
import com.scheduley.util.ValidationUtils;
import com.scheduley.viewmodel.CoursesViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class CoursesView extends BorderPane {
    private final CoursesViewModel viewModel;
    private final Runnable afterChange;
    private final TableView<Course> table = new TableView<>();

    public CoursesView(CoursesViewModel viewModel, Runnable afterChange) {
        this.viewModel = viewModel;
        this.afterChange = afterChange;
        build();
    }

    private void build() {
        getStyleClass().add("content-view");
        setPadding(new Insets(16));
        Label title = new Label("Courses");
        title.getStyleClass().add("section-title");
        Button add = new Button("Add");
        Button edit = new Button("Edit");
        Button delete = new Button("Delete");
        HBox toolbar = new HBox(8, title, add, edit, delete);
        toolbar.setPadding(new Insets(0, 0, 12, 0));

        TableColumn<Course, String> code = new TableColumn<>("Code");
        code.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        TableColumn<Course, String> name = new TableColumn<>("Name");
        name.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        TableColumn<Course, String> instructor = new TableColumn<>("Instructor");
        instructor.setCellValueFactory(data -> new SimpleStringProperty(nullToEmpty(data.getValue().getInstructor())));
        TableColumn<Course, String> location = new TableColumn<>("Location");
        location.setCellValueFactory(data -> new SimpleStringProperty(nullToEmpty(data.getValue().getLocationText())));
        TableColumn<Course, String> color = new TableColumn<>("Color");
        color.setCellValueFactory(data -> new SimpleStringProperty(nullToEmpty(data.getValue().getColorHex())));
        color.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String hex, boolean empty) {
                super.updateItem(hex, empty);
                if (empty || hex == null || hex.isBlank()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(ColorChoice.displayText(hex));
                setGraphic(ColorChoice.swatch(hex));
                setAlignment(Pos.CENTER_LEFT);
            }
        });
        table.getColumns().addAll(code, name, instructor, location, color);
        table.setItems(viewModel.courses());
        table.setPlaceholder(new Label("No courses yet. Add a course to start building this schedule."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        add.setOnAction(event -> openDialog(null));
        edit.setOnAction(event -> {
            Course selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) openDialog(selected);
        });
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && table.getSelectionModel().getSelectedItem() != null) {
                openDialog(table.getSelectionModel().getSelectedItem());
            }
        });
        delete.setOnAction(event -> deleteSelected());

        setTop(toolbar);
        setCenter(table);
    }

    private void openDialog(Course existing) {
        new CourseDialog(existing).showAndWait().ifPresent(course -> {
            try {
                ValidationUtils.validateCourse(course);
                viewModel.save(course);
                afterChange.run();
            } catch (RuntimeException e) {
                UiUtils.showError("Could not save course", e);
            }
        });
    }

    private void deleteSelected() {
        Course selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (!UiUtils.confirm("Delete course", "Delete " + selected.displayName() + "? Linked COURSE blocks will also be deleted.")) {
            return;
        }
        try {
            viewModel.delete(selected);
            afterChange.run();
        } catch (RuntimeException e) {
            UiUtils.showError("Could not delete course", e);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
