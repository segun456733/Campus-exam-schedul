package com.campus.examscheduler.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.Set;

/** Parsing/formatting helpers for times (minutes after midnight), dates and weekday sets. */
public final class TimeUtil {
    private TimeUtil() { }

    /** Parses "HH:mm" (also "H:mm"). Throws IllegalArgumentException with a friendly message. */
    public static int parseMinutes(String text) {
        if (text == null) throw new IllegalArgumentException("Time is missing.");
        String t = text.trim();
        String[] p = t.split(":");
        if (p.length != 2) throw new IllegalArgumentException("'" + text + "' is not a valid time. Use HH:mm, for example 08:30.");
        try {
            int h = Integer.parseInt(p[0].trim());
            int m = Integer.parseInt(p[1].trim());
            if (h < 0 || h > 24 || m < 0 || m > 59 || (h == 24 && m > 0))
                throw new IllegalArgumentException("'" + text + "' is not a valid time. Use HH:mm, for example 08:30.");
            return h * 60 + m;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + text + "' is not a valid time. Use HH:mm, for example 08:30.");
        }
    }

    public static String formatMinutes(int minutes) { return String.format("%02d:%02d", minutes / 60, minutes % 60); }

    /** Parses yyyy-MM-dd. */
    public static LocalDate parseDate(String text, String fieldName) {
        try {
            return LocalDate.parse(text == null ? "" : text.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid date written as yyyy-MM-dd (for example 2026-11-16).");
        }
    }

    public static String formatDays(Set<DayOfWeek> days) {
        StringBuilder sb = new StringBuilder();
        for (DayOfWeek d : DayOfWeek.values()) if (days.contains(d)) { if (sb.length() > 0) sb.append(','); sb.append(d.name()); }
        return sb.toString();
    }

    public static Set<DayOfWeek> parseDays(String text) {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        if (text != null && !text.isBlank()) for (String s : text.split(",")) days.add(DayOfWeek.valueOf(s.trim()));
        return days;
    }
}
