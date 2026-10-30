package com.campus.examscheduler.dao;

import com.campus.examscheduler.util.TimeUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** The saved timetable (table exam_schedules). */
public final class ScheduleDao {
    private ScheduleDao() { }

    /** One saved row. */
    public record Row(String eventKey, LocalDate date, int startMin, long venueId) { }

    public static Map<String, Row> findAll(Connection c) throws SQLException {
        Map<String, Row> rows = new LinkedHashMap<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT event_key, exam_date, start_time, venue_id FROM exam_schedules "
                + "ORDER BY exam_date, start_time"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.put(rs.getString(1), new Row(rs.getString(1), LocalDate.parse(rs.getString(2)),
                        TimeUtil.parseMinutes(rs.getString(3)), rs.getLong(4)));
            }
        }
        return rows;
    }

    public static void upsert(Connection c, String key, LocalDate date, int startMin, int endMin, long venueId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO exam_schedules (event_key, exam_date, start_time, end_time, venue_id) "
                + "VALUES (?, ?, ?, ?, ?) ON CONFLICT(event_key) DO UPDATE SET exam_date = excluded.exam_date, "
                + "start_time = excluded.start_time, end_time = excluded.end_time, venue_id = excluded.venue_id")) {
            ps.setString(1, key); ps.setString(2, date.toString());
            ps.setString(3, TimeUtil.formatMinutes(startMin)); ps.setString(4, TimeUtil.formatMinutes(endMin));
            ps.setLong(5, venueId);
            ps.executeUpdate();
        }
    }

    public static void delete(Connection c, String key) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM exam_schedules WHERE event_key = ?")) {
            ps.setString(1, key);
            ps.executeUpdate();
        }
    }

    public static void deleteAll(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM exam_schedules")) { ps.executeUpdate(); }
    }
}
