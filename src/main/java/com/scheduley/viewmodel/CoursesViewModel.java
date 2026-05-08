package com.scheduley.viewmodel;

import com.scheduley.dao.CourseDAO;
import com.scheduley.models.Course;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class CoursesViewModel {
    private final CourseDAO courseDAO;
    private final ObservableList<Course> courses = FXCollections.observableArrayList();

    public CoursesViewModel(CourseDAO courseDAO) {
        this.courseDAO = courseDAO;
    }

    public ObservableList<Course> courses() {
        return courses;
    }

    public void reload() {
        courses.setAll(courseDAO.findAll());
    }

    public Course save(Course course) {
        Course saved = course.getId() == null ? courseDAO.create(course) : update(course);
        reload();
        return saved;
    }

    public void delete(Course course) {
        courseDAO.deleteById(course.getId());
        reload();
    }

    private Course update(Course course) {
        courseDAO.update(course);
        return course;
    }
}
