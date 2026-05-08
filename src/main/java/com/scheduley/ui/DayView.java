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
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class DayView extends BorderPane {
    private static final double HOUR_HEIGHT = 72;
    private static final double DAY_WIDTH = 520;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");

    private final WeekViewModel weekViewModel;
    private final CoursesViewModel coursesViewModel;
    private final TasksViewModel tasksViewModel;
    private final SettingsDAO settingsDAO;
    private final Runnable afterChange;
    private final Label dateLabel = new Label();
    private final Label nowLabel = new Label();
    private final Label timeStatusLabel = new Label();
    private final VBox upcomingList = new VBox(8);
    private final HBox calendar = new HBox();
    private final Timeline clock;
    private LocalDate selectedDate = LocalDate.now();

    public DayView(WeekViewModel weekViewModel, CoursesViewModel coursesViewModel, TasksViewModel tasksViewModel,
                   SettingsDAO settingsDAO, Runnable afterChange) {
        this.weekViewModel = weekViewModel;
        this.coursesViewModel = coursesViewModel;
        this.tasksViewModel = tasksViewModel;
        this.settingsDAO = settingsDAO;
        this.afterChange = afterChange;
        this.clock = new Timeline(new KeyFrame(Duration.seconds(30), event -> render()));
        this.clock.setCycleCount(Timeline.INDEFINITE);
        build();
        this.clock.play();
    }

    private void build() {
        getStyleClass().add("content-view");
        setPadding(new Insets(16));

        Label title = new Label("Day View");
        title.getStyleClass().add("section-title");
        dateLabel.getStyleClass().add("day-date-label");

        Button previous = new Button("Previous");
        Button today = new Button("Today");
        Button next = new Button("Next");
        Button add = new Button("Add Block");
        Button refresh = new Button("Refresh");
        HBox toolbar = new HBox(8, title, previous, today, next, add, refresh);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        VBox top = new VBox(8, toolbar, dateLabel, timeStatusLabel);
        top.setPadding(new Insets(0, 0, 12, 0));
        timeStatusLabel.getStyleClass().add("muted-label");

        Label nowTitle = new Label("Now");
        nowTitle.getStyleClass().add("section-title");
        nowLabel.getStyleClass().add("now-card");
        Label upcomingTitle = new Label("Upcoming");
        upcomingTitle.getStyleClass().add("section-title");
        VBox sidebar = new VBox(12, nowTitle, nowLabel, upcomingTitle, upcomingList);
        sidebar.getStyleClass().add("day-sidebar");
        sidebar.setPrefWidth(280);
        sidebar.setMinWidth(240);

        calendar.setSpacing(0);
        calendar.getStyleClass().add("calendar-container");
        ScrollPane scrollPane = new ScrollPane(calendar);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);

        previous.setOnAction(event -> {
            selectedDate = selectedDate.minusDays(1);
            render();
        });
        today.setOnAction(event -> {
            selectedDate = LocalDate.now();
            render();
        });
        next.setOnAction(event -> {
            selectedDate = selectedDate.plusDays(1);
            render();
        });
        add.setOnAction(event -> openDialog(null));
        refresh.setOnAction(event -> afterChange.run());
        weekViewModel.timeBlocks().addListener((ListChangeListener<TimeBlock>) change -> render());

        setTop(top);
        setLeft(sidebar);
        setCenter(scrollPane);
        render();
    }

    private void render() {
        AppSettings settings = settingsDAO.getSettings();
        int startHour = settings.getCalendarStartHour();
        int endHour = settings.getCalendarEndHour();
        int startMinute = startHour * 60;
        int endMinute = endHour * 60;
        double minuteHeight = HOUR_HEIGHT / 60.0;
        double calendarHeight = Math.max(1, endHour - startHour) * HOUR_HEIGHT;
        List<TimeBlock> blocks = blocksForSelectedDate();

        dateLabel.setText(selectedDate.format(DATE_FORMAT));
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

        VBox dayColumn = new VBox();
        dayColumn.setPrefWidth(DAY_WIDTH);
        dayColumn.setMinWidth(DAY_WIDTH);
        dayColumn.getChildren().add(headerLabel(TimeUtils.dayName(selectedDate.getDayOfWeek().getValue())));

        Pane dayPane = new Pane();
        dayPane.setPrefSize(DAY_WIDTH, calendarHeight);
        dayPane.setMinSize(DAY_WIDTH, calendarHeight);
        dayPane.getStyleClass().add("calendar-day-pane");
        addHourLines(dayPane, startHour, endHour);
        addBlocks(dayPane, blocks, startMinute, endMinute, minuteHeight);
        addCurrentTimeIndicator(dayPane, startMinute, endMinute, minuteHeight);

        dayColumn.getChildren().add(dayPane);
        calendar.getChildren().add(dayColumn);

        updateNowAndUpcoming(blocks);
    }

    private void addBlocks(Pane dayPane, List<TimeBlock> blocks, int startMinute, int endMinute, double minuteHeight) {
        for (TimeBlock block : blocks) {
            int clippedStart = Math.max(block.getStartMinute(), startMinute);
            int clippedEnd = Math.min(block.getEndMinute(), endMinute);
            if (clippedEnd <= clippedStart) continue;
            double y = (clippedStart - startMinute) * minuteHeight;
            double height = Math.max(38, (clippedEnd - clippedStart) * minuteHeight - 4);
            Button blockButton = blockButton(block, height);
            blockButton.setLayoutX(8);
            blockButton.setLayoutY(y + 2);
            blockButton.setPrefSize(DAY_WIDTH - 16, height);
            blockButton.setOnAction(event -> openDialog(block));
            dayPane.getChildren().add(blockButton);
        }
    }

    private void addCurrentTimeIndicator(Pane dayPane, int startMinute, int endMinute, double minuteHeight) {
        if (!selectedDate.equals(LocalDate.now())) {
            timeStatusLabel.setText("Current time is hidden for days other than today.");
            return;
        }

        int nowMinute = TimeUtils.minuteOfDay(LocalTime.now());
        if (nowMinute < startMinute || nowMinute > endMinute) {
            timeStatusLabel.setText("Current time is outside visible schedule hours.");
            return;
        }

        timeStatusLabel.setText("Current time: " + TimeUtils.formatMinute(nowMinute));
        double y = (nowMinute - startMinute) * minuteHeight;
        Pane line = new Pane();
        line.getStyleClass().add("current-time-line");
        line.setPrefSize(DAY_WIDTH - 12, 3);
        line.setLayoutX(6);
        line.setLayoutY(y);
        Label marker = new Label("Now");
        marker.getStyleClass().add("current-time-marker");
        marker.setLayoutX(8);
        marker.setLayoutY(Math.max(0, y - 20));
        dayPane.getChildren().addAll(line, marker);
    }

    private void updateNowAndUpcoming(List<TimeBlock> blocks) {
        upcomingList.getChildren().clear();
        if (!selectedDate.equals(LocalDate.now())) {
            nowLabel.setText("Now is available when viewing today.");
            addUpcoming(blocks, 0);
            return;
        }

        int nowMinute = TimeUtils.minuteOfDay(LocalTime.now());
        TimeBlock current = blocks.stream()
                .filter(block -> block.getStartMinute() <= nowMinute && block.getEndMinute() > nowMinute)
                .min(Comparator.comparingInt(TimeBlock::getStartMinute))
                .orElse(null);

        if (current == null) {
            nowLabel.setText("Now: Free time");
        } else {
            nowLabel.setText("Now: " + current.getTitle()
                    + "\n" + TimeUtils.formatMinute(current.getStartMinute()) + " - " + TimeUtils.formatMinute(current.getEndMinute())
                    + "\n" + current.getBlockType());
        }
        addUpcoming(blocks, nowMinute);
    }

    private void addUpcoming(List<TimeBlock> blocks, int afterMinute) {
        List<TimeBlock> upcoming = blocks.stream()
                .filter(block -> block.getStartMinute() > afterMinute)
                .sorted(Comparator.comparingInt(TimeBlock::getStartMinute))
                .limit(3)
                .toList();
        if (upcoming.isEmpty()) {
            Label empty = new Label("No more scheduled blocks today.");
            empty.getStyleClass().add("muted-label");
            upcomingList.getChildren().add(empty);
            return;
        }
        for (TimeBlock block : upcoming) {
            Label item = new Label(block.getTitle()
                    + "\n" + TimeUtils.formatMinute(block.getStartMinute()) + " - " + TimeUtils.formatMinute(block.getEndMinute())
                    + "\n" + block.getBlockType() + locationLine(block));
            item.getStyleClass().add("upcoming-card");
            upcomingList.getChildren().add(item);
        }
    }

    private Button blockButton(TimeBlock block, double height) {
        String text = block.getTitle()
                + "\n" + TimeUtils.formatMinute(block.getStartMinute()) + " - " + TimeUtils.formatMinute(block.getEndMinute())
                + "\n" + block.getBlockType() + locationLine(block);
        Button button = new Button(text);
        String color = block.getColorHex() == null || block.getColorHex().isBlank() ? fallbackColor(block.getBlockType()) : block.getColorHex();
        button.setStyle("""
                -fx-background-color: %s;
                -fx-text-fill: white;
                -fx-font-size: 12px;
                -fx-font-weight: 700;
                -fx-alignment: top-left;
                -fx-padding: 8;
                -fx-background-radius: 6;
                """.formatted(color));
        button.setWrapText(true);
        button.setMinHeight(height);
        return button;
    }

    private List<TimeBlock> blocksForSelectedDate() {
        int selectedDay = selectedDate.getDayOfWeek().getValue();
        return weekViewModel.timeBlocks().stream()
                .filter(block -> isForSelectedDate(block, selectedDay))
                .sorted(Comparator.comparingInt(TimeBlock::getStartMinute))
                .toList();
    }

    private boolean isForSelectedDate(TimeBlock block, int selectedDay) {
        if (block.getBlockDate() != null && !block.getBlockDate().isBlank()) {
            return LocalDate.parse(block.getBlockDate()).equals(selectedDate);
        }
        return block.getDayOfWeek() != null && block.getDayOfWeek() == selectedDay;
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

    private String locationLine(TimeBlock block) {
        return block.getLocationText() == null || block.getLocationText().isBlank()
                ? ""
                : "\n" + block.getLocationText();
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
