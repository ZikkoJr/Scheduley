package com.scheduley.ui;

import com.scheduley.dao.SettingsDAO;
import com.scheduley.models.AppSettings;
import com.scheduley.util.ValidationUtils;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class SettingsView extends VBox {
    private final SettingsDAO settingsDAO;
    private final Runnable afterChange;

    public SettingsView(SettingsDAO settingsDAO, Runnable afterChange) {
        this.settingsDAO = settingsDAO;
        this.afterChange = afterChange;
        build();
    }

    private void build() {
        getStyleClass().add("content-view");
        setPadding(new Insets(16));
        setSpacing(12);
        Label title = new Label("Settings");
        title.getStyleClass().add("section-title");
        AppSettings settings = settingsDAO.getSettings();

        ComboBox<String> weekStart = new ComboBox<>();
        weekStart.getItems().addAll("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
        weekStart.setValue(com.scheduley.util.TimeUtils.dayName(settings.getWeekStartDay()));

        Spinner<Integer> startHour = new Spinner<>();
        startHour.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, settings.getCalendarStartHour()));
        startHour.setEditable(true);
        Spinner<Integer> endHour = new Spinner<>();
        endHour.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 24, settings.getCalendarEndHour()));
        endHour.setEditable(true);

        ComboBox<String> theme = new ComboBox<>();
        theme.getItems().addAll("LIGHT", "DARK");
        theme.setValue(settings.getTheme() == null ? "LIGHT" : settings.getTheme());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Week starts on"), weekStart);
        grid.addRow(1, new Label("Calendar start hour"), startHour);
        grid.addRow(2, new Label("Calendar end hour"), endHour);
        grid.addRow(3, new Label("Theme"), theme);

        Button save = new Button("Save Settings");
        save.setOnAction(event -> {
            AppSettings next = new AppSettings(
                    1,
                    com.scheduley.util.TimeUtils.dayNumber(weekStart.getValue()),
                    startHour.getValue(),
                    endHour.getValue(),
                    theme.getValue()
            );
            try {
                ValidationUtils.validateSettings(next);
                settingsDAO.updateSettings(next);
                afterChange.run();
                UiUtils.showInfo("Settings saved", "Your settings were saved.");
            } catch (RuntimeException e) {
                UiUtils.showError("Could not save settings", e);
            }
        });

        getChildren().addAll(title, grid, save);
    }
}
