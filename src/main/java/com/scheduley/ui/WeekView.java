package com.scheduley.ui;

import com.scheduley.dao.SettingsDAO;
import com.scheduley.models.AppSettings;
import com.scheduley.models.Course;
import com.scheduley.models.Task;
import com.scheduley.models.TimeBlock;
import com.scheduley.util.TimeUtils;
import com.scheduley.util.ValidationUtils;
import com.scheduley.viewmodel.CoursesViewModel;
import com.scheduley.viewmodel.TasksViewModel;
import com.scheduley.viewmodel.WeekViewModel;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeekView extends BorderPane {
    private static final double HOUR_HEIGHT = 72;
    private static final double DAY_WIDTH = 148;

    private final WeekViewModel weekViewModel;
    private final CoursesViewModel coursesViewModel;
    private final TasksViewModel tasksViewModel;
    private final SettingsDAO settingsDAO;
    private final Runnable afterChange;
    private final Label conflictSummary = new Label();
    private final HBox calendar = new HBox();

    public WeekView(WeekViewModel weekViewModel, CoursesViewModel coursesViewModel, TasksViewModel tasksViewModel,
                    SettingsDAO settingsDAO, Runnable afterChange) {
        this.weekViewModel = weekViewModel;
        this.coursesViewModel = coursesViewModel;
        this.tasksViewModel = tasksViewModel;
        this.settingsDAO = settingsDAO;
        this.afterChange = afterChange;
        build();
    }

    private void build() {
        getStyleClass().add("content-view");
        setPadding(new Insets(16));
        Label title = new Label("Week View");
        title.getStyleClass().add("section-title");
        Button add = new Button("Add Block");
        Button refresh = new Button("Refresh");
        conflictSummary.getStyleClass().add("conflict-summary");
        HBox toolbar = new HBox(10, title, add, refresh, conflictSummary);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 12, 0));

        calendar.setSpacing(0);
        calendar.getStyleClass().add("calendar-container");
        ScrollPane scrollPane = new ScrollPane(calendar);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);

        add.setOnAction(event -> openDialog(null));
        refresh.setOnAction(event -> afterChange.run());
        weekViewModel.timeBlocks().addListener((ListChangeListener<TimeBlock>) change -> render());

        setTop(toolbar);
        setCenter(scrollPane);
        render();
    }

    private void render() {
        AppSettings settings = settingsDAO.getSettings();
        int startHour = settings.getCalendarStartHour();
        int endHour = settings.getCalendarEndHour();
        int startMinute = startHour * 60;
        double minuteHeight = HOUR_HEIGHT / 60.0;
        double calendarHeight = Math.max(1, endHour - startHour) * HOUR_HEIGHT;
        calendar.getChildren().clear();

        VBox timeGutter = new VBox();
        timeGutter.setPrefWidth(76);
        timeGutter.setMinWidth(76);
        timeGutter.getChildren().add(headerLabel(""));
        for (int hour = startHour; hour <= endHour; hour++) {
            Label label = new Label(TimeUtils.formatMinute(Math.min(hour * 60, 1439)));
            label.setPrefHeight(HOUR_HEIGHT);
            label.setAlignment(Pos.TOP_RIGHT);
            label.setPadding(new Insets(2, 8, 0, 0));
            label.getStyleClass().add("hour-label");
            label.setStyle("-fx-font-size: 11px;");
            timeGutter.getChildren().add(label);
        }
        calendar.getChildren().add(timeGutter);

        Map<Integer, Pane> dayPanes = new HashMap<>();
        for (int day : orderedDays(settings.getWeekStartDay())) {
            VBox column = new VBox();
            column.setPrefWidth(DAY_WIDTH);
            column.setMinWidth(DAY_WIDTH);
            column.getChildren().add(headerLabel(TimeUtils.dayName(day)));
            Pane pane = new Pane();
            pane.setPrefSize(DAY_WIDTH, calendarHeight);
            pane.setMinSize(DAY_WIDTH, calendarHeight);
            pane.getStyleClass().add("calendar-day-pane");
            addHourLines(pane, startHour, endHour);
            column.getChildren().add(pane);
            calendar.getChildren().add(column);
            dayPanes.put(day, pane);
        }

        for (TimeBlock block : weekViewModel.timeBlocks()) {
            if (block.getDayOfWeek() == null) continue;
            Pane pane = dayPanes.get(block.getDayOfWeek());
            if (pane == null) continue;
            int clippedStart = Math.max(block.getStartMinute(), startMinute);
            int clippedEnd = Math.min(block.getEndMinute(), endHour * 60);
            if (clippedEnd <= clippedStart) continue;
            double y = (clippedStart - startMinute) * minuteHeight;
            double height = Math.max(28, (clippedEnd - clippedStart) * minuteHeight - 4);
            Button blockButton = blockButton(block, height);
            blockButton.setLayoutX(6);
            blockButton.setLayoutY(y + 2);
            blockButton.setPrefSize(DAY_WIDTH - 12, height);
            blockButton.setOnAction(event -> openDialog(block));
            pane.getChildren().add(blockButton);
        }
        updateConflictSummary();
    }

    private Button blockButton(TimeBlock block, double height) {
        String time = TimeUtils.formatMinute(block.getStartMinute()) + " - " + TimeUtils.formatMinute(block.getEndMinute());
        String location = block.getLocationText() == null || block.getLocationText().isBlank()
                ? ""
                : "\n" + block.getLocationText();
        Button button = new Button(block.getTitle() + "\n" + time + location);
        String color = block.getColorHex() == null || block.getColorHex().isBlank() ? fallbackColor(block.getBlockType()) : block.getColorHex();
        boolean hasConflict = !weekViewModel.conflictsFor(block).isEmpty();
        button.setStyle("""
                -fx-background-color: %s;
                -fx-text-fill: white;
                -fx-font-size: 11px;
                -fx-font-weight: 700;
                -fx-alignment: top-left;
                -fx-padding: 6;
                -fx-background-radius: 6;
                -fx-border-color: %s;
                -fx-border-width: %s;
                -fx-border-radius: 6;
                """.formatted(color, hasConflict ? "#DC2626" : color, hasConflict ? "3" : "0"));
        button.setWrapText(true);
        button.setMinHeight(height);
        return button;
    }

    private void openDialog(TimeBlock existing) {
        TimeBlockDialog dialog = new TimeBlockDialog(existing, List.copyOf(coursesViewModel.courses()), List.copyOf(tasksViewModel.tasks()));
        java.util.Optional<TimeBlock> result = dialog.showAndWait();
        if (dialog.isDeleteRequested() && existing != null) {
            deleteBlock(existing);
            return;
        }
        if (dialog.getDuplicatedBlock() != null) {
            saveBlock(dialog.getDuplicatedBlock());
            return;
        }
        result.ifPresent(this::saveBlock);
    }

    private void saveBlock(TimeBlock block) {
        try {
            if (block == null) return;
            ValidationUtils.validateTimeBlock(block);
            List<TimeBlock> conflicts = weekViewModel.conflictsFor(block);
            if (!conflicts.isEmpty()) {
                String names = conflicts.stream()
                        .map(TimeBlock::getTitle)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("another block");
                UiUtils.showInfo("Schedule conflict", "This overlaps with: " + names + ". The block will still be saved.");
            }
            weekViewModel.save(block);
            afterChange.run();
        } catch (RuntimeException e) {
            UiUtils.showError("Could not save time block", e);
        }
    }

    private void deleteBlock(TimeBlock block) {
        if (!UiUtils.confirm("Delete time block", "Delete " + block.getTitle() + "?")) {
            return;
        }
        try {
            weekViewModel.delete(block);
            afterChange.run();
        } catch (RuntimeException e) {
            UiUtils.showError("Could not delete time block", e);
        }
    }

    private void updateConflictSummary() {
        long count = weekViewModel.timeBlocks().stream()
                .filter(block -> block.getDayOfWeek() != null && !weekViewModel.conflictsFor(block).isEmpty())
                .count();
        conflictSummary.setText(count == 0 ? "" : count + " conflicting block(s)");
    }

    private void addHourLines(Pane pane, int startHour, int endHour) {
        for (int hour = startHour; hour <= endHour; hour++) {
            Pane line = new Pane();
            line.getStyleClass().add("calendar-grid-line");
            line.setPrefHeight(1);
            line.setPrefWidth(DAY_WIDTH);
            line.setLayoutY((hour - startHour) * HOUR_HEIGHT);
            pane.getChildren().add(line);
        }
    }

    private Label headerLabel(String text) {
        Label label = new Label(text);
        label.setPrefHeight(34);
        label.setMinHeight(34);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        label.getStyleClass().add("calendar-header");
        return label;
    }

    private int[] orderedDays(int weekStartDay) {
        int[] days = new int[7];
        for (int i = 0; i < 7; i++) {
            days[i] = ((weekStartDay - 1 + i) % 7) + 1;
        }
        return days;
    }

    private String fallbackColor(String type) {
        return switch (type) {
            case "COURSE" -> "#2563EB";
            case "WORK" -> "#0F766E";
            case "STUDY" -> "#7C3AED";
            case "TASK" -> "#D97706";
            default -> "#4B5563";
        };
    }
}
