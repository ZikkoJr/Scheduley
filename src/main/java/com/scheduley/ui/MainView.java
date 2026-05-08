package com.scheduley.ui;

import com.scheduley.dao.CourseDAO;
import com.scheduley.dao.SettingsDAO;
import com.scheduley.dao.TaskDAO;
import com.scheduley.dao.TimeBlockDAO;
import com.scheduley.dao.sqlite.SqliteCourseDao;
import com.scheduley.dao.sqlite.SqliteSettingsDAO;
import com.scheduley.dao.sqlite.SqliteTaskDAO;
import com.scheduley.dao.sqlite.SqliteTimeBlockDAO;
import com.scheduley.viewmodel.CoursesViewModel;
import com.scheduley.viewmodel.TasksViewModel;
import com.scheduley.viewmodel.WeekViewModel;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class MainView extends BorderPane {
    private final SettingsDAO settingsDAO;
    private final CoursesViewModel coursesViewModel;
    private final TasksViewModel tasksViewModel;
    private final WeekViewModel weekViewModel;

    public MainView() {
        CourseDAO courseDAO = new SqliteCourseDao();
        TaskDAO taskDAO = new SqliteTaskDAO();
        TimeBlockDAO timeBlockDAO = new SqliteTimeBlockDAO();
        settingsDAO = new SqliteSettingsDAO();

        coursesViewModel = new CoursesViewModel(courseDAO);
        tasksViewModel = new TasksViewModel(taskDAO);
        weekViewModel = new WeekViewModel(timeBlockDAO);

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

        VBox header = new VBox();
        header.getStyleClass().add("app-header");
        header.setPadding(new Insets(16, 20, 8, 20));
        Label title = new Label("Scheduley");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("Local student schedule builder");
        subtitle.getStyleClass().add("app-subtitle");
        header.getChildren().addAll(title, subtitle);

        getStyleClass().add("app-root");
        setTop(header);
        setCenter(tabs);
        reloadAll();
    }

    private void reloadAll() {
        try {
            applyTheme();
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
}
