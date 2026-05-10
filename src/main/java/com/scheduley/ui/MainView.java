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
import com.scheduley.viewmodel.CoursesViewModel;
import com.scheduley.viewmodel.ScheduleProfilesViewModel;
import com.scheduley.viewmodel.TasksViewModel;
import com.scheduley.viewmodel.WeekViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MainView extends BorderPane {
    private final SettingsDAO settingsDAO;
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
        scheduleSelector.setPrefWidth(240);
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

        HBox toolbar = new HBox(8, label, scheduleSelector, manage);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        return toolbar;
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
}
