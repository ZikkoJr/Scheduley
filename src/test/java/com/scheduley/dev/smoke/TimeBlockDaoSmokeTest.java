package com.scheduley.dev.smoke;

import com.scheduley.dao.sqlite.SqliteTimeBlockDao;
import com.scheduley.db.ConnectDB;
import com.scheduley.db.Migrations;
import com.scheduley.models.BlockCategory;
import com.scheduley.models.TimeBlock;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class TimeBlockDaoSmokeTest {

    public static void main(String[] args) {
        System.out.println("=== TimeBlockDaoSmokeTest START ===");

        ConnectDB db = new ConnectDB();
        SqliteTimeBlockDao dao = new SqliteTimeBlockDao(db);

        printDbFileLocation(db);      // shows you EXACT DB file being used
        dropAndRecreateTables(db);    // guarantees old schema is gone

        Migrations.init();            // rebuild schema fresh

        verifyColumnExists(db, "time_block", "colour_hex");

        // 2) Clean slate (deterministic test)
        resetTables(db);

        // 3) Create two courses (for course-linked blocks + cascade test)
        long courseAId = insertCourse(db, "CSE101", "Intro to CS", 3);
        long courseBId = insertCourse(db, "MAT101", "Calculus I", 3);

        // 4) CREATE blocks
        TimeBlock workMon = new TimeBlock(
                "Work Shift",
                BlockCategory.WORK_SHIFT,
                null,             // no course
                1,                // Monday
                toMin(9, 0),
                toMin(17, 0),
                "McDonalds",
                "#FF0000"
        );

        TimeBlock gymTue = new TimeBlock(
                "Gym",
                BlockCategory.HOBBY,
                null,
                2,                // Tuesday
                toMin(18, 0),
                toMin(19, 0),
                "Leg day",
                "#00FF00"
        );

        TimeBlock studyMonA = new TimeBlock(
                "Study Session (CSE101)",
                BlockCategory.COURSE_STUDY,
                courseAId,
                1,                // Monday
                toMin(19, 0),
                toMin(21, 0),
                "Review lecture",
                "#0000FF"
        );

        TimeBlock studyWedB = new TimeBlock(
                "Study Session (MAT101)",
                BlockCategory.COURSE_STUDY,
                courseBId,
                3,                // Wednesday
                toMin(10, 0),
                toMin(11, 30),
                "Practice problems",
                "#123456"
        );

        workMon = dao.create(workMon);
        gymTue = dao.create(gymTue);
        studyMonA = dao.create(studyMonA);
        studyWedB = dao.create(studyWedB);

        assertTrue(workMon.getId() != null, "Create should set id for workMon");
        assertTrue(gymTue.getId() != null, "Create should set id for gymTue");
        assertTrue(studyMonA.getId() != null, "Create should set id for studyMonA");
        assertTrue(studyWedB.getId() != null, "Create should set id for studyWedB");

        // 5) GET BY ID
        Optional<TimeBlock> fetched = dao.getById(workMon.getId());
        assertTrue(fetched.isPresent(), "getById should return present");
        assertEquals("Work Shift", fetched.get().getTitle(), "getById title should match");
        assertEquals(BlockCategory.WORK_SHIFT, fetched.get().getCategory(), "getById category should match");
        assertEquals(null, fetched.get().getCourseId(), "Work shift courseId should be null");

        // 6) GET ALL (should be sorted by day_of_week, start_min)
        List<TimeBlock> all = dao.getAll();
        assertEquals(4, all.size(), "getAll should return 4 rows");

        // basic sort check: each next block should not be "earlier" than previous
        for (int i = 1; i < all.size(); i++) {
            TimeBlock prev = all.get(i - 1);
            TimeBlock curr = all.get(i);
            assertTrue(compare(prev, curr) <= 0, "getAll should be ordered by dayOfWeek then startMin");
        }

        // 7) GET BY DAY (Monday = 1)
        List<TimeBlock> monday = dao.getByDay(1);
        assertEquals(2, monday.size(), "getByDay(1) should return 2 blocks");
        assertTrue(monday.get(0).getStartMin() <= monday.get(1).getStartMin(), "Monday blocks should be ordered by startMin");

        // 8) GET BY COURSE ID
        List<TimeBlock> byCourseA = dao.getByCourseId(courseAId);
        assertEquals(1, byCourseA.size(), "getByCourseId(courseA) should return 1 block");
        assertEquals(courseAId, byCourseA.get(0).getCourseId(), "Returned block should match courseAId");

        // 9) GET BY CATEGORY
        List<TimeBlock> hobbies = dao.getByCategory(BlockCategory.HOBBY);
        assertEquals(1, hobbies.size(), "getByCategory(HOBBY) should return 1 block");
        assertEquals("Gym", hobbies.get(0).getTitle(), "HOBBY title should be Gym");

        // 10) UPDATE
        workMon.setTitle("Work Shift (Updated)");
        workMon.setNotes("Closing shift");
        workMon.setColourHex("#ABCDEF");
        workMon.setStartMin(toMin(10, 0));
        workMon.setEndMin(toMin(18, 0));

        boolean updated = dao.update(workMon);
        assertTrue(updated, "update should return true");

        TimeBlock updatedWork = dao.getById(workMon.getId()).orElseThrow(() ->
                new RuntimeException("Updated work block not found"));

        assertEquals("Work Shift (Updated)", updatedWork.getTitle(), "Updated title should persist");
        assertEquals("Closing shift", updatedWork.getNotes(), "Updated notes should persist");
        assertEquals("#ABCDEF", updatedWork.getColourHex(), "Updated colourHex should persist");
        assertEquals(toMin(10, 0), updatedWork.getStartMin(), "Updated startMin should persist");
        assertEquals(toMin(18, 0), updatedWork.getEndMin(), "Updated endMin should persist");

        // 11) DELETE
        boolean deleted = dao.delete(gymTue.getId());
        assertTrue(deleted, "delete should return true");
        assertTrue(dao.getById(gymTue.getId()).isEmpty(), "Deleted block should not be found");

        List<TimeBlock> allAfterDelete = dao.getAll();
        assertEquals(3, allAfterDelete.size(), "After deleting 1 block, getAll should return 3");

        // 12) FK CASCADE TEST: deleting courseB should delete its linked time blocks
        deleteCourse(db, courseBId);

        List<TimeBlock> byCourseBAfter = dao.getByCourseId(courseBId);
        assertEquals(0, byCourseBAfter.size(), "After deleting courseB, its blocks should be gone (cascade)");

        List<TimeBlock> allAfterCascade = dao.getAll();
        assertEquals(2, allAfterCascade.size(), "After cascade delete, total blocks should be 2");

        System.out.println("=== TimeBlockDaoSmokeTest PASSED ✅ ===");
    }

    // --- Helpers ---

    private static int toMin(int hour, int minute) {
        return hour * 60 + minute;
    }

    /**
     * Compare by dayOfWeek then startMin (like your ORDER BY)
     */
    private static int compare(TimeBlock a, TimeBlock b) {
        if (a.getDayOfWeek() != b.getDayOfWeek()) {
            return Integer.compare(a.getDayOfWeek(), b.getDayOfWeek());
        }
        return Integer.compare(a.getStartMin(), b.getStartMin());
    }

    private static void resetTables(ConnectDB db) {
        try (Connection conn = db.getConnection();
             Statement st = conn.createStatement()) {

            // order matters due to FK
            st.executeUpdate("DELETE FROM time_block");
            st.executeUpdate("DELETE FROM course");

            System.out.println("Reset tables: time_block + course cleared.");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to reset tables", e);
        }
    }

    private static long insertCourse(ConnectDB db, String code, String name, int credits) {
        String sql = "INSERT INTO course(code, name, credits) VALUES (?, ?, ?)";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, code);
            ps.setString(2, name);
            ps.setInt(3, credits);

            int rows = ps.executeUpdate();
            if (rows != 1) throw new SQLException("Course insert failed, rows affected: " + rows);

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }

            throw new SQLException("Course insert succeeded but no generated key returned.");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert course: " + code, e);
        }
    }

    private static void deleteCourse(ConnectDB db, long courseId) {
        String sql = "DELETE FROM course WHERE id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, courseId);
            int rows = ps.executeUpdate();
            assertTrue(rows == 1, "deleteCourse should delete exactly 1 row");

            System.out.println("Deleted course id=" + courseId + " (should cascade).");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete course id=" + courseId, e);
        }
    }

    private static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new RuntimeException("ASSERT FAIL: " + msg);
    }

    private static void assertEquals(Object expected, Object actual, String msg) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new RuntimeException("ASSERT FAIL: " + msg + " | expected=" + expected + " actual=" + actual);
    }

    private static void printDbFileLocation(ConnectDB db) {
        try (Connection conn = db.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA database_list;")) {

            System.out.println("=== SQLite database_list ===");
            while (rs.next()) {
                System.out.println("name=" + rs.getString("name")
                        + " file=" + rs.getString("file"));
            }
            System.out.println("============================");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to print DB location", e);
        }
    }

    private static void dropAndRecreateTables(ConnectDB db) {
        try (Connection conn = db.getConnection();
             Statement st = conn.createStatement()) {

            // Drop child tables first due to FK relationships
            st.executeUpdate("DROP TABLE IF EXISTS time_block;");
            st.executeUpdate("DROP TABLE IF EXISTS course;");

            System.out.println("Dropped tables: time_block, course");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to drop tables", e);
        }
    }

    private static void verifyColumnExists(ConnectDB db, String table, String column) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("PRAGMA table_info(" + table + ");");
             ResultSet rs = ps.executeQuery()) {

            boolean found = false;
            System.out.println("=== PRAGMA table_info(" + table + ") ===");
            while (rs.next()) {
                String colName = rs.getString("name");
                System.out.println(colName);
                if (column.equalsIgnoreCase(colName)) found = true;
            }
            System.out.println("==============================");

            if (!found) {
                throw new RuntimeException("Column '" + column + "' NOT found in table '" + table + "'");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to verify table info", e);
        }
    }

}
