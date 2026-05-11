package com.scheduley.export;

import com.scheduley.models.ScheduleProfile;

public record ScheduleProfileExport(
        Long id,
        String name,
        String description,
        boolean active,
        String createdAt,
        String updatedAt
) {
    static ScheduleProfileExport from(ScheduleProfile profile) {
        return new ScheduleProfileExport(
                profile.getId(),
                profile.getName(),
                profile.getDescription(),
                profile.isActive(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
