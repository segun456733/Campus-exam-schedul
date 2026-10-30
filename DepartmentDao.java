package com.campus.examscheduler.dao;

import com.campus.examscheduler.model.Department;
import com.campus.examscheduler.model.StudentGroup;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Departments and their student groups (one cohort per level). */
public final class DepartmentDao {
    private DepartmentDao() { }

    private static Department map(ResultSet rs) throws SQLException {
        Department d = new Department(rs.getLong("id"), rs.getString("code"), rs.getString("name"), rs.getString("description"));
        d.setCourseCount(rs.getInt("course_count"));
        return d;
    }

    private static final String SELECT = "SELECT d.id, d.code, d.name, d.description, "
            + "(SELECT COUNT(*) FROM courses c WHERE c.department_id = d.id) AS course_count FROM departments d ";

    public static List<Department> findAll(Connection c) throws SQLException {
        List<Department> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(SELECT + "ORDER BY d.name"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public static Department findByCode(Connection c, String code) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + "WHERE d.code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public static Department findByName(Connection c, String name) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + "WHERE d.name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public static long insert(Connection c, Department d) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO departments (code, name, description) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getCode()); ps.setString(2, d.getName()); ps.setString(3, d.getDescription());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); return keys.getLong(1); }
        }
    }

    public static void update(Connection c, Department d) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE departments SET code = ?, name = ?, description = ? WHERE id = ?")) {
            ps.setString(1, d.getCode()); ps.setString(2, d.getName()); ps.setString(3, d.getDescription()); ps.setLong(4, d.getId());
            ps.executeUpdate();
        }
    }

    public static void delete(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM departments WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    // ---- student groups ------------------------------------------------------------------

    public static long insertGroup(Connection c, String name, long departmentId, String level) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO student_groups (name, department_id, level) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name); ps.setLong(2, departmentId); ps.setString(3, level);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); return keys.getLong(1); }
        }
    }

    public static void renameGroups(Connection c, long departmentId, String code) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE student_groups SET name = ? || '-' || level WHERE department_id = ?")) {
            ps.setString(1, code); ps.setLong(2, departmentId);
            ps.executeUpdate();
        }
    }

    public static List<StudentGroup> findAllGroups(Connection c) throws SQLException {
        List<StudentGroup> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT id, name, department_id, level FROM student_groups ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new StudentGroup(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getString(4)));
        }
        return list;
    }

    /** How many courses use any group of this department as an extra (shared) group. */
    public static int countExtraGroupUses(Connection c, long departmentId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM course_extra_groups e "
                + "JOIN student_groups g ON g.id = e.group_id WHERE g.department_id = ?")) {
            ps.setLong(1, departmentId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }
}
