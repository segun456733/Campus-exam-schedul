package com.campus.examscheduler.dao;

import com.campus.examscheduler.scheduler.SchedulingRules;
import com.campus.examscheduler.scheduler.TimeRange;
import com.campus.examscheduler.util.TimeUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Examination period settings (single row) and daily break periods. */
public final class SettingsDao {
    private SettingsDao() { }

    /** Returns the saved rules, or null when none have been saved yet. */
    public static SchedulingRules load(Connection c) throws SQLException {
        SchedulingRules r = new SchedulingRules();
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM examination_settings WHERE id = 1"); ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) return null;
            r.setStartDate(LocalDate.parse(rs.getString("start_date")));
            r.setEndDate(LocalDate.parse(rs.getString("end_date")));
            r.setAllowedDays(TimeUtil.parseDays(rs.getString("allowed_days")));
            r.setDayStartMin(TimeUtil.parseMinutes(rs.getString("day_start")));
            r.setDayEndMin(TimeUtil.parseMinutes(rs.getString("day_end")));
            r.setSlotStepMin(rs.getInt("slot_step_minutes"));
            r.setGapMin(rs.getInt("gap_minutes"));
            r.setMaxSimultaneous(rs.getInt("max_simultaneous"));
            r.setNoCrossDepartment(rs.getInt("no_cross_department") == 1);
            r.setNoCrossLevel(rs.getInt("no_cross_level") == 1);
            r.setMaxExamsPerGroupPerDay(rs.getInt("max_exams_per_group_per_day"));
        }
        List<TimeRange> breaks = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT label, start_time, end_time FROM exam_breaks ORDER BY start_time");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) breaks.add(new TimeRange(rs.getString(1), TimeUtil.parseMinutes(rs.getString(2)), TimeUtil.parseMinutes(rs.getString(3))));
        }
        r.setBreaks(breaks);
        return r;
    }

    public static void save(Connection c, SchedulingRules r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO examination_settings (id, start_date, end_date, allowed_days, "
                + "day_start, day_end, slot_step_minutes, gap_minutes, max_simultaneous, no_cross_department, no_cross_level, "
                + "max_exams_per_group_per_day) VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(id) DO UPDATE SET start_date = excluded.start_date, end_date = excluded.end_date, "
                + "allowed_days = excluded.allowed_days, day_start = excluded.day_start, day_end = excluded.day_end, "
                + "slot_step_minutes = excluded.slot_step_minutes, gap_minutes = excluded.gap_minutes, "
                + "max_simultaneous = excluded.max_simultaneous, no_cross_department = excluded.no_cross_department, "
                + "no_cross_level = excluded.no_cross_level, max_exams_per_group_per_day = excluded.max_exams_per_group_per_day")) {
            ps.setString(1, r.getStartDate().toString());
            ps.setString(2, r.getEndDate().toString());
            ps.setString(3, TimeUtil.formatDays(r.getAllowedDays()));
            ps.setString(4, TimeUtil.formatMinutes(r.getDayStartMin()));
            ps.setString(5, TimeUtil.formatMinutes(r.getDayEndMin()));
            ps.setInt(6, r.getSlotStepMin());
            ps.setInt(7, r.getGapMin());
            ps.setInt(8, r.getMaxSimultaneous());
            ps.setInt(9, r.isNoCrossDepartment() ? 1 : 0);
            ps.setInt(10, r.isNoCrossLevel() ? 1 : 0);
            ps.setInt(11, r.getMaxExamsPerGroupPerDay());
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM exam_breaks")) { ps.executeUpdate(); }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO exam_breaks (label, start_time, end_time) VALUES (?, ?, ?)")) {
            for (TimeRange b : r.getBreaks()) {
                ps.setString(1, b.label());
                ps.setString(2, TimeUtil.formatMinutes(b.start()));
                ps.setString(3, TimeUtil.formatMinutes(b.end()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
