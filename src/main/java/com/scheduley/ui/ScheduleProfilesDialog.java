package com.scheduley.ui;

import com.scheduley.models.ScheduleProfile;
import com.scheduley.viewmodel.ScheduleProfilesViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

class ScheduleProfilesDialog extends Dialog<Void> {
    private final ScheduleProfilesViewModel viewModel;
    private final Runnable afterChange;
    private final ListView<ScheduleProfile> profileList = new ListView<>();
    private final TextField name = new TextField();
    private final TextArea description = new TextArea();
    private final Label activeStatus = new Label("Select a schedule to edit it.");
    private ScheduleProfile editingProfile;

    ScheduleProfilesDialog(ScheduleProfilesViewModel viewModel, Runnable afterChange) {
        this.viewModel = viewModel;
        this.afterChange = afterChange;
        setTitle("Manage Schedules");
        setHeaderText("Create, rename, or delete schedules");
        UiUtils.applyTheme(getDialogPane());
        build();
    }

    private void build() {
        profileList.setItems(viewModel.profiles());
        profileList.setCellFactory(list -> new ScheduleProfileCell());
        profileList.setPlaceholder(new Label("No schedules yet."));
        profileList.setPrefWidth(250);
        profileList.setPrefHeight(280);
        profileList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> loadProfile(selected));

        description.setPrefRowCount(4);
        name.setPrefWidth(300);
        description.setPrefWidth(300);
        name.setPromptText("Schedule name");
        description.setPromptText("Description");

        activeStatus.getStyleClass().add("muted-label");
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Name"), name);
        form.addRow(1, new Label("Description"), description);
        form.add(activeStatus, 1, 2);

        Button newProfile = new Button("New");
        Button save = new Button("Save or Rename");
        Button activate = new Button("Activate");
        Button delete = new Button("Delete");
        HBox actions = new HBox(8, newProfile, save, activate, delete);
        actions.getStyleClass().add("dialog-actions");
        actions.setAlignment(Pos.CENTER_LEFT);

        BorderPane content = new BorderPane();
        content.setPadding(new Insets(12));
        content.setLeft(profileList);
        BorderPane.setMargin(profileList, new Insets(0, 14, 0, 0));
        content.setCenter(form);
        content.setBottom(actions);
        BorderPane.setMargin(actions, new Insets(14, 0, 0, 0));

        newProfile.setOnAction(event -> startNewProfile());
        save.setOnAction(event -> saveProfile());
        activate.setOnAction(event -> activateSelected());
        delete.setOnAction(event -> deleteSelected());

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        if (viewModel.getActiveProfile() != null) {
            profileList.getSelectionModel().select(viewModel.getActiveProfile());
        } else if (!viewModel.profiles().isEmpty()) {
            profileList.getSelectionModel().selectFirst();
        }
    }

    private void loadProfile(ScheduleProfile profile) {
        editingProfile = profile;
        name.setText(profile == null ? "" : value(profile.getName()));
        description.setText(profile == null ? "" : value(profile.getDescription()));
        activeStatus.setText(profile == null
                ? "Creating a new schedule."
                : profile.isActive() ? "This is the active schedule." : "Activate this schedule to use it.");
    }

    private void startNewProfile() {
        profileList.getSelectionModel().clearSelection();
        editingProfile = null;
        name.clear();
        description.clear();
        name.requestFocus();
    }

    private void saveProfile() {
        String profileName = name.getText() == null ? "" : name.getText().trim();
        if (profileName.isBlank()) {
            UiUtils.showError("Check schedule details", new IllegalArgumentException("Schedule name is required."));
            return;
        }

        try {
            if (editingProfile == null) {
                viewModel.createAndActivate(profileName, description.getText());
            } else {
                editingProfile.setName(profileName);
                editingProfile.setDescription(blankToNull(description.getText()));
                viewModel.update(editingProfile);
            }
            afterChange.run();
            selectActiveProfile();
        } catch (RuntimeException e) {
            UiUtils.showError("Could not save schedule", e);
        }
    }

    private void activateSelected() {
        ScheduleProfile selected = profileList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            viewModel.setActive(selected);
            afterChange.run();
            selectActiveProfile();
        } catch (RuntimeException e) {
            UiUtils.showError("Could not switch schedule", e);
        }
    }

    private void deleteSelected() {
        ScheduleProfile selected = profileList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (viewModel.profiles().size() <= 1) {
            UiUtils.showInfo("Cannot delete schedule", "Scheduley needs at least one schedule profile.");
            return;
        }
        if (!UiUtils.confirm(
                "Delete schedule",
                "Delete " + selected.getName() + "? This will delete its courses, tasks, and time blocks.")) {
            return;
        }

        try {
            viewModel.delete(selected);
            afterChange.run();
            selectActiveProfile();
        } catch (RuntimeException e) {
            UiUtils.showError("Could not delete schedule", e);
        }
    }

    private void selectActiveProfile() {
        ScheduleProfile active = viewModel.getActiveProfile();
        if (active != null) {
            profileList.getSelectionModel().select(active);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }

    private static final class ScheduleProfileCell extends ListCell<ScheduleProfile> {
        @Override
        protected void updateItem(ScheduleProfile profile, boolean empty) {
            super.updateItem(profile, empty);
            getStyleClass().remove("active-schedule-cell");
            if (empty || profile == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            Label name = new Label(value(profile.getName()));
            name.getStyleClass().add("section-title");
            name.setStyle("-fx-font-size: 13px;");
            Label detail = new Label(profile.isActive() ? "Active schedule" : value(profile.getDescription()));
            detail.getStyleClass().add(profile.isActive() ? "active-pill" : "muted-label");
            VBox text = new VBox(4, name, detail);
            setGraphic(text);
            if (profile.isActive()) {
                getStyleClass().add("active-schedule-cell");
            }
        }
    }
}
