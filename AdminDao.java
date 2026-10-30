package com.campus.examscheduler.dao;

import com.campus.examscheduler.model.Admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class AdminDao {
    private AdminDao() { }

    private static Admin map(ResultSet rs) throws SQLException {
        return new Admin(rs.getLong("id"), rs.getString("username"), rs.getString("password_hash"),
                rs.getString("salt"), rs.getInt("iterations"));
    }

    public static Admin findByUsername(Connection c, String username) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM admins WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public static Admin findById(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM admins WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public static int count(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM admins"); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public static void insert(Connection c, String username, String hash, String salt, int iterations) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO admins (username, password_hash, salt, iterations) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, username); ps.setString(2, hash); ps.setString(3, salt); ps.setInt(4, iterations);
            ps.executeUpdate();
        }
    }

    public static void updatePassword(Connection c, long id, String hash, String salt, int iterations) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE admins SET password_hash = ?, salt = ?, iterations = ? WHERE id = ?")) {
            ps.setString(1, hash); ps.setString(2, salt); ps.setInt(3, iterations); ps.setLong(4, id);
            ps.executeUpdate();
        }
    }
}
