package com.campus.examscheduler.scheduler;

import com.campus.examscheduler.model.Venue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Small helpers that build rules, venues and examination events for the tests. */
final class TestData {
    private TestData() { }

    /** Mon 16 Nov - Fri 20 Nov 2026, Mon-Fri, 08:00-17:00, lunch break 12:00-13:00, 30-minute step. */
    static SchedulingRules rules() {
        SchedulingRules r = new SchedulingRules();
        r.setStartDate(LocalDate.of(2026, 11, 16));
        r.setEndDate(LocalDate.of(2026, 11, 20));
        r.setAllowedDays(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));
        r.setDayStartMin(8 * 60);
        r.setDayEndMin(17 * 60);
        r.setSlotStepMin(30);
        r.setGapMin(0);
        r.setMaxSimultaneous(5);
        r.setBreaks(new java.util.ArrayList<>(List.of(new TimeRange("Lunch", 12 * 60, 13 * 60))));
        return r;
    }

    static Venue venue(long id, String name, int capacity, boolean available) {
        return new Venue(id, name, capacity, "Hall", available);
    }

    /** A single-course event sat by one cohort (group). */
    static ExamEvent event(long id, String code, long deptId, String level, int students, int duration, long... groups) {
        ExamEvent e = new ExamEvent("C" + id, false);
        Set<Long> g = new java.util.TreeSet<>();
        for (long x : groups) g.add(x);
        e.addCourse(id, code, code + " title", deptId, "Dept" + deptId, level, students, duration, g);
        return e;
    }

    static LocalDate monday() { return LocalDate.of(2026, 11, 16); }
}
