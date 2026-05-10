package com.scheduley.viewmodel;

import com.scheduley.dao.ScheduleProfileDAO;
import com.scheduley.models.ScheduleProfile;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ScheduleProfilesViewModel {
    private final ScheduleProfileDAO scheduleProfileDAO;
    private final ObservableList<ScheduleProfile> profiles = FXCollections.observableArrayList();
    private final ObjectProperty<ScheduleProfile> activeProfile = new SimpleObjectProperty<>();

    public ScheduleProfilesViewModel(ScheduleProfileDAO scheduleProfileDAO) {
        this.scheduleProfileDAO = scheduleProfileDAO;
    }

    public ObservableList<ScheduleProfile> profiles() {
        return profiles;
    }

    public ObjectProperty<ScheduleProfile> activeProfileProperty() {
        return activeProfile;
    }

    public ScheduleProfile getActiveProfile() {
        return activeProfile.get();
    }

    public Long activeProfileId() {
        ScheduleProfile profile = activeProfile.get();
        if (profile == null || profile.getId() == null) {
            throw new IllegalStateException("No active schedule profile is selected.");
        }
        return profile.getId();
    }

    public void reload() {
        scheduleProfileDAO.ensureDefaultProfileExists();
        profiles.setAll(scheduleProfileDAO.findAll());
        Long activeId = scheduleProfileDAO.findActive()
                .map(ScheduleProfile::getId)
                .orElse(null);
        activeProfile.set(profiles.stream()
                .filter(profile -> profile.getId().equals(activeId))
                .findFirst()
                .orElseGet(() -> profiles.isEmpty() ? null : profiles.getFirst()));
    }

    public ScheduleProfile createAndActivate(String name, String description) {
        ScheduleProfile profile = scheduleProfileDAO.create(new ScheduleProfile(name.trim(), blankToNull(description)));
        scheduleProfileDAO.setActiveProfile(profile.getId());
        reload();
        return getActiveProfile();
    }

    public void update(ScheduleProfile profile) {
        scheduleProfileDAO.update(profile);
        reload();
    }

    public void delete(ScheduleProfile profile) {
        scheduleProfileDAO.deleteById(profile.getId());
        reload();
    }

    public void setActive(ScheduleProfile profile) {
        if (profile == null || profile.getId() == null) {
            return;
        }
        scheduleProfileDAO.setActiveProfile(profile.getId());
        reload();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
