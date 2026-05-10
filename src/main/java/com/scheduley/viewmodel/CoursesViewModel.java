package com.scheduley.viewmodel;

import com.scheduley.dao.CourseDAO;
import com.scheduley.models.Course;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.function.LongSupplier;

public class CoursesViewModel {
    private final CourseDAO courseDAO;
    private final LongSupplier activeScheduleProfileId;
    private final ObservableList<Course> courses = FXCollections.observableArrayList();

    public CoursesViewModel(CourseDAO courseDAO, LongSupplier activeScheduleProfileId) {
        this.courseDAO = courseDAO;
        this.activeScheduleProfileId = activeScheduleProfileId;
    }

    public ObservableList<Course> courses() {
        return courses;
    }

    public void reload() {
        courses.setAll(courseDAO.findAll(activeScheduleProfileId.getAsLong()));
    }

    public Course save(Course course) {
        Course saved = course.getId() == null ? courseDAO.create(course, activeScheduleProfileId.getAsLong()) : update(course);
        reload();
        return saved;
    }

    public void delete(Course course) {
        courseDAO.deleteById(course.getId(), activeScheduleProfileId.getAsLong());
        reload();
    }

    private Course update(Course course) {
        courseDAO.update(course, activeScheduleProfileId.getAsLong());
        return course;
    }
}
