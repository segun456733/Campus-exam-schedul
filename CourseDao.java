package com.campus.examscheduler.dao;

import com.campus.examscheduler.model.Course;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CourseDao {
    private CourseDao() { }

    private static final String SELECT = "SELECT c.id, c.code, c.title, c.department_id, d.name AS dept_name, c.level, "
            + "c.student_count, c.duration_minutes, c.group_id, g.name AS group_name, c.joint_exam_id, j.name AS joint_name "
            + "FROM courses c JOIN departments d ON d.id = c.department_id JOIN student_groups g ON g.id = c.group_id "
            + "LEFT JOIN joint_examinations j ON j.id = c.joint_exam_id ";

    private static Course map(ResultSet rs) throws SQLException {
        Course c = new Course();
        c.setId(rs.getLong("id")); c.setCode(rs.getString("code")); c.setTitle(rs.getString("title"));
        c.setDepartmentId(rs.getLong("department_id")); c.setDepartmentName(rs.getString("dept_name"));
        c.setLevel(rs.getString("level")); c.setStudentCount(rs.getInt("student_count"));
        c.setDurationMinutes(rs.getInt("duration_minutes")); c.setGroupId(rs.getLong("group_id"));
        c.setGroupName(rs.getString("group_name"));
        long joint = rs.getLong("joint_exam_id");
        c.setJointExamId(rs.wasNull() ? null : joint);
        c.setJointName(rs.getString("joint_name"));
        return c;
    }

    public static List<Course> findAll(Connection c) throws SQLException {
        List<Course> list = new ArrayList<>();
        Map<Long, Course> byId = new HashMap<>();
        try (PreparedStatement ps = c.prepareStatement(SELECT + "ORDER BY c.code"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) { Course course = map(rs); list.add(course); byId.put(course.getId(), course); }
        }
        try (PreparedStatement ps = c.prepareStatement("SELECT e.course_id, e.group_id, g.name FROM course_extra_groups e "
                + "JOIN student_groups g ON g.id = e.group_id ORDER BY g.name"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Course course = byId.get(rs.getLong(1));
                if (course != null) { course.getExtraGroupIds().add(rs.getLong(2)); course.getExtraGroupNames().add(rs.getString(3)); }
            }
        }
        return list;
    }

    public static Course findById(Connection c, long id) throws SQLException {
        for (Course course : findAll(c)) if (course.getId() == id) return course;
        return null;
    }

    public static Course findByCode(Connection c, String code) throws SQLException {
        for (Course course : findAll(c)) if (course.getCode().equalsIgnoreCase(code)) return course;
        return null;
    }

    /** Looks up a joint-examination name, creating it when new. Returns null for blank names. */
    public static Long findOrCreateJoint(Connection c, String name) throws SQLException {
        if (name == null || name.isBlank()) return null;
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM joint_examinations WHERE name = ?")) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getLong(1); }
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO joint_examinations (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name.trim());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); return keys.getLong(1); }
        }
    }

    public static List<String> findAllJointNames(Connection c) throws SQLException {
        List<String> names = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT name FROM joint_examinations ORDER BY name"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) names.add(rs.getString(1));
        }
        return names;
    }

    /** Inserts a course and its extra groups; the joint exam must already be resolved in course.getJointExamId(). */
    public static long insert(Connection c, Course course) throws SQLException {
        long id;
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO courses (code, title, department_id, level, student_count, "
                + "duration_minutes, group_id, joint_exam_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, course);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); id = keys.getLong(1); }
        }
        saveExtraGroups(c, id, course.getExtraGroupIds());
        return id;
    }

    public static void update(Connection c, Course course) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE courses SET code = ?, title = ?, department_id = ?, level = ?, "
                + "student_count = ?, duration_minutes = ?, group_id = ?, joint_exam_id = ? WHERE id = ?")) {
            bind(ps, course);
            ps.setLong(9, course.getId());
            ps.executeUpdate();
        }
        saveExtraGroups(c, course.getId(), course.getExtraGroupIds());
    }

    public static void delete(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM courses WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /** Removes joint-examination names that no course uses any more. */
    public static void deleteUnusedJoints(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM joint_examinations WHERE id NOT IN "
                + "(SELECT joint_exam_id FROM courses WHERE joint_exam_id IS NOT NULL)")) {
            ps.executeUpdate();
        }
    }

    private static void bind(PreparedStatement ps, Course course) throws SQLException {
        ps.setString(1, course.getCode()); ps.setString(2, course.getTitle()); ps.setLong(3, course.getDepartmentId());
        ps.setString(4, course.getLevel()); ps.setInt(5, course.getStudentCount()); ps.setInt(6, course.getDurationMinutes());
        ps.setLong(7, course.getGroupId());
        if (course.getJointExamId() == null) ps.setNull(8, java.sql.Types.INTEGER); else ps.setLong(8, course.getJointExamId());
    }

    private static void saveExtraGroups(Connection c, long courseId, List<Long> groupIds) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM course_extra_groups WHERE course_id = ?")) {
            ps.setLong(1, courseId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO course_extra_groups (course_id, group_id) VALUES (?, ?)")) {
            for (long g : groupIds) { ps.setLong(1, courseId); ps.setLong(2, g); ps.addBatch(); }
            ps.executeBatch();
        }
    }
}
