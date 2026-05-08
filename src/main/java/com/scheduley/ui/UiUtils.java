package com.scheduley.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.Parent;

import java.util.Optional;

final class UiUtils {
    private static final String STYLESHEET = "/com/scheduley/app.css";
    private static String currentTheme = "LIGHT";

    private UiUtils() {
    }

    static void setCurrentTheme(String theme) {
        currentTheme = "DARK".equalsIgnoreCase(theme) ? "DARK" : "LIGHT";
    }

    static void applyTheme(Parent parent) {
        String stylesheet = UiUtils.class.getResource(STYLESHEET).toExternalForm();
        if (!parent.getStylesheets().contains(stylesheet)) {
            parent.getStylesheets().add(stylesheet);
        }
        parent.getStyleClass().removeAll("theme-light", "theme-dark");
        parent.getStyleClass().add("DARK".equals(currentTheme) ? "theme-dark" : "theme-light");
    }

    static void applyTheme(DialogPane dialogPane) {
        applyTheme((Parent) dialogPane);
    }

    static void showError(String title, Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(rootMessage(error));
        applyTheme(alert.getDialogPane());
        alert.showAndWait();
    }

    static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        applyTheme(alert.getDialogPane());
        alert.showAndWait();
    }

    static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        applyTheme(alert.getDialogPane());
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? error.toString() : current.getMessage();
    }
}
