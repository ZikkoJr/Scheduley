package com.scheduley.ui;

import com.scheduley.dao.CourseDAO;
import com.scheduley.dao.ScheduleProfileDAO;
import com.scheduley.dao.SettingsDAO;
import com.scheduley.dao.TaskDAO;
import com.scheduley.dao.TimeBlockDAO;
import com.scheduley.dao.sqlite.SqliteCourseDao;
import com.scheduley.dao.sqlite.SqliteScheduleProfileDAO;
import com.scheduley.dao.sqlite.SqliteSettingsDAO;
import com.scheduley.dao.sqlite.SqliteTaskDAO;
import com.scheduley.dao.sqlite.SqliteTimeBlockDAO;
import com.scheduley.models.ScheduleProfile;
import com.scheduley.service.ScheduleExportService;
import com.scheduley.service.ScheduleImportResult;
import com.scheduley.service.ScheduleImportService;
import com.scheduley.viewmodel.CoursesViewModel;
import com.scheduley.viewmodel.ScheduleProfilesViewModel;
import com.scheduley.viewmodel.TasksViewModel;
import com.scheduley.viewmodel.WeekViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.FileChooser;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;

public class MainView extends BorderPane {
    private final SettingsDAO settingsDAO;
    private final ScheduleExportService scheduleExportService;
    private final ScheduleImportService scheduleImportService;
    private final ScheduleProfilesViewModel scheduleProfilesViewModel;
    private final CoursesViewModel coursesViewModel;
    private final TasksViewModel tasksViewModel;
    private final WeekViewModel weekViewModel;
    private final ComboBox<ScheduleProfile> scheduleSelector = new ComboBox<>();
    private boolean syncingScheduleSelector;

    public MainView() {
        ScheduleProfileDAO scheduleProfileDAO = new SqliteScheduleProfileDAO();
        CourseDAO courseDAO = new SqliteCourseDao();
        TaskDAO taskDAO = new SqliteTaskDAO();
        TimeBlockDAO timeBlockDAO = new SqliteTimeBlockDAO();
        settingsDAO = new SqliteSettingsDAO();
        scheduleExportService = new ScheduleExportService(courseDAO, taskDAO, timeBlockDAO);
        scheduleImportService = new ScheduleImportService(scheduleProfileDAO, courseDAO, taskDAO, timeBlockDAO);

        scheduleProfilesViewModel = new ScheduleProfilesViewModel(scheduleProfileDAO);
        scheduleProfilesViewModel.reload();
        coursesViewModel = new CoursesViewModel(courseDAO, scheduleProfilesViewModel::activeProfileId);
        tasksViewModel = new TasksViewModel(taskDAO, scheduleProfilesViewModel::activeProfileId);
        weekViewModel = new WeekViewModel(timeBlockDAO, scheduleProfilesViewModel::activeProfileId);

        Runnable reloadAll = this::reloadAll;
        CoursesView coursesView = new CoursesView(coursesViewModel, reloadAll);
        TasksView tasksView = new TasksView(tasksViewModel, coursesViewModel, reloadAll);
        DayView dayView = new DayView(weekViewModel, coursesViewModel, tasksViewModel, settingsDAO, reloadAll);
        WeekView weekView = new WeekView(weekViewModel, coursesViewModel, tasksViewModel, settingsDAO, reloadAll);
        SettingsView settingsView = new SettingsView(settingsDAO, reloadAll);

        TabPane tabs = new TabPane();
        tabs.getTabs().add(new Tab("Day View", dayView));
        tabs.getTabs().add(new Tab("Week", weekView));
        tabs.getTabs().add(new Tab("Courses", coursesView));
        tabs.getTabs().add(new Tab("Tasks", tasksView));
        tabs.getTabs().add(new Tab("Settings", settingsView));
        tabs.getTabs().forEach(tab -> tab.setClosable(false));

        VBox header = new VBox(8);
        header.getStyleClass().add("app-header");
        header.setPadding(new Insets(16, 20, 8, 20));
        Label title = new Label("Scheduley");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("Local student schedule builder");
        subtitle.getStyleClass().add("app-subtitle");
        header.getChildren().addAll(title, subtitle, scheduleToolbar());

        getStyleClass().add("app-root");
        setTop(header);
        setCenter(tabs);
        reloadAll();
    }

    private void reloadAll() {
        try {
            applyTheme();
            scheduleProfilesViewModel.reload();
            syncScheduleSelector();
            coursesViewModel.reload();
            tasksViewModel.reload();
            weekViewModel.reload();
        } catch (RuntimeException e) {
            UiUtils.showError("Unable to load saved data", e);
        }
    }

    private void applyTheme() {
        String theme = settingsDAO.getSettings().getTheme();
        UiUtils.setCurrentTheme(theme);
        getStyleClass().removeAll("theme-light", "theme-dark");
        UiUtils.applyTheme(this);
    }

    private HBox scheduleToolbar() {
        Label label = new Label("Schedule:");
        label.getStyleClass().add("app-subtitle");
        scheduleSelector.setItems(scheduleProfilesViewModel.profiles());
        scheduleSelector.getStyleClass().add("schedule-selector");
        scheduleSelector.setCellFactory(list -> new ScheduleProfileCell());
        scheduleSelector.setButtonCell(new ScheduleProfileCell());
        scheduleSelector.valueProperty().addListener((obs, old, selected) -> {
            if (syncingScheduleSelector || selected == null) {
                return;
            }
            try {
                scheduleProfilesViewModel.setActive(selected);
                reloadAll();
            } catch (RuntimeException e) {
                UiUtils.showError("Could not switch schedule", e);
            }
        });
        Button manage = new Button("Manage Schedules");
        manage.setOnAction(event -> new ScheduleProfilesDialog(scheduleProfilesViewModel, this::reloadAll).showAndWait());

        Button export = new Button("Export Current Schedule");
        export.setOnAction(event -> exportCurrentSchedule());

        Button importSchedule = new Button("Import Schedule from JSON");
        importSchedule.setOnAction(event -> importScheduleFromJson());

        HBox toolbar = new HBox(8, label, scheduleSelector, manage, export, importSchedule);
        toolbar.getStyleClass().add("schedule-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);
        return toolbar;
    }

    private void exportCurrentSchedule() {
        ScheduleProfile activeProfile = scheduleProfilesViewModel.getActiveProfile();
        if (activeProfile == null || activeProfile.getId() == null) {
            UiUtils.showError("Could not export schedule", new IllegalStateException("No active schedule profile is selected."));
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Current Schedule");
        fileChooser.setInitialFileName(defaultExportFileName(activeProfile));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        File selectedFile = fileChooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        Path destination = ensureJsonExtension(selectedFile.toPath());
        try {
            scheduleExportService.exportToJsonFile(activeProfile, destination);
            UiUtils.showInfo("Schedule exported", "Exported " + activeProfile.getName() + " to " + destination.toAbsolutePath() + ".");
        } catch (RuntimeException e) {
            UiUtils.showError("Could not export schedule", e);
        }
    }

    private void importScheduleFromJson() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Schedule from JSON");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        File selectedFile = fileChooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        try {
            ScheduleImportResult result = scheduleImportService.importFromJsonFile(selectedFile.toPath());
            reloadAll();
            UiUtils.showInfo(
                    "Schedule imported",
                    "Imported and activated " + result.scheduleProfile().getName() + ".\n"
                            + result.courseCount() + " courses, "
                            + result.taskCount() + " tasks, "
                            + result.timeBlockCount() + " time blocks."
            );
        } catch (RuntimeException e) {
            UiUtils.showError("Could not import schedule", e);
        }
    }

    private static Path ensureJsonExtension(Path path) {
        String fileName = path.getFileName().toString();
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".json")) {
            return path;
        }
        return path.resolveSibling(fileName + ".json");
    }

    private static String defaultExportFileName(ScheduleProfile profile) {
        String name = profile.getName() == null || profile.getName().isBlank() ? "schedule" : profile.getName();
        String safeName = name.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return (safeName.isBlank() ? "schedule" : safeName) + "-export.json";
    }

    private void syncScheduleSelector() {
        syncingScheduleSelector = true;
        try {
            scheduleSelector.setItems(scheduleProfilesViewModel.profiles());
            scheduleSelector.setValue(scheduleProfilesViewModel.getActiveProfile());
        } finally {
            syncingScheduleSelector = false;
        }
    }

    private static final class ScheduleProfileCell extends ListCell<ScheduleProfile> {
        @Override
        protected void updateItem(ScheduleProfile profile, boolean empty) {
            super.updateItem(profile, empty);
            setText(empty || profile == null ? null : profile.getName() + (profile.isActive() ? " (Active)" : ""));
        }
    }
}
