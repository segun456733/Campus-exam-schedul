package com.campus.examscheduler.dao;

import com.campus.examscheduler.model.Venue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class VenueDao {
    private VenueDao() { }

    private static Venue map(ResultSet rs) throws SQLException {
        Venue v = new Venue(rs.getLong("id"), rs.getString("name"), rs.getInt("capacity"),
                rs.getString("venue_type"), rs.getInt("available") == 1);
        v.setNotes(rs.getString("notes"));
        return v;
    }

    public static List<Venue> findAll(Connection c) throws SQLException {
        List<Venue> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM venues ORDER BY capacity DESC, name"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public static Venue findByName(Connection c, String name) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM venues WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public static long insert(Connection c, Venue v) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO venues (name, capacity, venue_type, available, notes) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, v.getName()); ps.setInt(2, v.getCapacity()); ps.setString(3, v.getType());
            ps.setInt(4, v.isAvailable() ? 1 : 0); ps.setString(5, v.getNotes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); return keys.getLong(1); }
        }
    }

    public static void update(Connection c, Venue v) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE venues SET name = ?, capacity = ?, venue_type = ?, available = ?, notes = ? WHERE id = ?")) {
            ps.setString(1, v.getName()); ps.setInt(2, v.getCapacity()); ps.setString(3, v.getType());
            ps.setInt(4, v.isAvailable() ? 1 : 0); ps.setString(5, v.getNotes()); ps.setLong(6, v.getId());
            ps.executeUpdate();
        }
    }

    public static void delete(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM venues WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public static int countScheduledIn(Connection c, long venueId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM exam_schedules WHERE venue_id = ?")) {
            ps.setLong(1, venueId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }
}
