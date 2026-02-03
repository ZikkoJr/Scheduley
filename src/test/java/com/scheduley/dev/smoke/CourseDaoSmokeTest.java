package com.scheduley.dev.smoke;

import com.scheduley.dao.CourseDAO;
import com.scheduley.dao.sqlite.SqliteCourseDao;
import com.scheduley.db.ConnectDB;
import com.scheduley.db.Migrations;
import com.scheduley.models.Course;

import java.util.List;
import java.util.Optional;

public class CourseDaoSmokeTest {

    public static void main(String[] args) {
        System.out.println("=== Course DAO Smoke Test ===");

        // 1) Init schema (creates table if missing)
        Migrations.init();

        // 2) Build DAO
        ConnectDB db = new ConnectDB(); // adjust if your ConnectDB is static-only
        CourseDAO dao = new SqliteCourseDao(db);

        // 3) Clean slate (so reruns are predictable)
        wipeCourses(dao);

        // 4) CREATE
        Course c1 = new Course("ITEC3220", "Database Systems", 3, "#00A86B");
        Course created = dao.create(c1);

        require(created.getId() != null, "Create failed: id is null after insert.");
        System.out.println("Created: " + created);

        long id = created.getId();

        // 5) READ by ID
        Optional<Course> byId = dao.getById(id);
        require(byId != null, "getById returned null Optional (should be Optional.empty()).");
        require(byId.isPresent(), "getById failed: course not found for id=" + id);
        System.out.println("Read by id: " + byId.get());

        // 6) READ by Code
        Optional<Course> byCode = dao.getByCode("ITEC3220");
        require(byCode != null, "getByCode returned null Optional (should be Optional.empty()).");
        require(byCode.isPresent(), "getByCode failed: course not found for code=ITEC3220");
        System.out.println("Read by code: " + byCode.get());

        // 7) GET ALL
        List<Course> all = dao.getAll();
        require(all.size() == 1, "Expected 1 course after create, found " + all.size());
        System.out.println("All courses: " + all);

        // 8) UPDATE
        created.setName("Database Systems (Updated)");
        created.setCredits(4);
        created.setColourHex("#2196F3");

        boolean updated = dao.update(created);
        require(updated, "Update failed: dao.update returned false.");
        System.out.println("Updated course: " + created);

        Optional<Course> afterUpdate = dao.getById(id);
        require(afterUpdate != null && afterUpdate.isPresent(), "After update, course missing by id=" + id);
        require("Database Systems (Updated)".equals(afterUpdate.get().getName()), "Update did not persist name.");
        require(afterUpdate.get().getCredits() == 4, "Update did not persist credits.");
        System.out.println("Verified update: " + afterUpdate.get());

        // 9) DELETE
        boolean deleted = dao.delete(id);
        require(deleted, "Delete failed: dao.delete returned false.");
        System.out.println("Deleted id=" + id);

        Optional<Course> afterDelete = dao.getById(id);
        require(afterDelete != null, "getById returned null Optional (should be Optional.empty()).");
        require(afterDelete.isEmpty(), "Expected empty after delete, but course still exists.");


        System.out.println("✅ Course DAO Smoke Test PASSED");
    }

    private static void wipeCourses(CourseDAO dao) {
        List<Course> existing = dao.getAll();
        for (Course c : existing) {
            if (c.getId() != null) {
                dao.delete(c.getId());
            }
        }
        System.out.println("Wiped " + existing.size() + " existing course(s).");
    }

    private static void require(boolean condition, String msg) {
        if (!condition) throw new RuntimeException("SMOKE TEST FAIL: " + msg);
    }
}
