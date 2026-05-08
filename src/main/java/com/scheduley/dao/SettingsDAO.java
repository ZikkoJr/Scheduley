package com.scheduley.dao;

import com.scheduley.models.AppSettings;

public interface SettingsDAO {
    AppSettings getSettings();
    boolean updateSettings(AppSettings settings);
}
