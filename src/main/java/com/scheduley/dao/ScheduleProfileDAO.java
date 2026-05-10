package com.scheduley.dao;

import com.scheduley.models.ScheduleProfile;

import java.util.List;
import java.util.Optional;

public interface ScheduleProfileDAO {
    ScheduleProfile create(ScheduleProfile profile);
    Optional<ScheduleProfile> findById(Long id);
    List<ScheduleProfile> findAll();
    Optional<ScheduleProfile> findActive();
    boolean update(ScheduleProfile profile);
    boolean deleteById(Long id);
    boolean setActiveProfile(Long id);
    ScheduleProfile ensureDefaultProfileExists();
    void assignExistingDataToDefaultProfile(Long defaultProfileId);
}
