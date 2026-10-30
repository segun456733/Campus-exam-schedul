package com.campus.examscheduler.scheduler;

import com.campus.examscheduler.model.Venue;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The single source of truth for every scheduling rule.
 * The automatic generator, the manual-edit validator and the final timetable validator
 * ALL use these same methods, so a manual change can never bypass the rules.
 */
public final class ConstraintChecker {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE dd MMM yyyy", Locale.ENGLISH);
    private final SchedulingRules rules;

    public ConstraintChecker(SchedulingRules rules) { this.rules = rules; }

    public static String time(int minutes) { return String.format("%02d:%02d", minutes / 60, minutes % 60); }
    public static String date(LocalDate d) { return DATE_FMT.format(d); }

    // ------------------------------------------------------------------ static rules (no other exams needed)

    /** Rules 7-9: examination period, permitted weekdays, daily hours and breaks. */
    public Violation slotViolation(LocalDate date, int start, int duration) {
        int end = start + duration;
        if (rules.getStartDate() == null || rules.getEndDate() == null)
            return new Violation(Violation.Type.INVALID_SETTINGS, "The examination period has not been configured.");
        if (date.isBefore(rules.getStartDate()) || date.isAfter(rules.getEndDate()))
            return new Violation(Violation.Type.DATE_RANGE, date(date) + " is outside the examination period ("
                    + rules.getStartDate() + " to " + rules.getEndDate() + ").");
        if (!rules.getAllowedDays().contains(date.getDayOfWeek()))
            return new Violation(Violation.Type.DAY_NOT_ALLOWED, date(date) + " is not a permitted examination day.");
        if (start < rules.getDayStartMin() || end > rules.getDayEndMin())
            return new Violation(Violation.Type.OUTSIDE_HOURS, "Time " + time(start) + "-" + time(end)
                    + " is outside the daily examination hours (" + time(rules.getDayStartMin()) + "-"
                    + time(rules.getDayEndMin()) + ").");
        for (TimeRange b : rules.getBreaks()) {
            if (b.overlaps(start, end))
                return new Violation(Violation.Type.BREAK, "Time " + time(start) + "-" + time(end)
                        + " overlaps the break '" + b.label() + "' (" + time(b.start()) + "-" + time(b.end()) + ").");
        }
        return null;
    }

    /** Rules 9 and venue availability. */
    public Violation venueViolation(Venue v, int students) {
        if (!v.isAvailable())
            return new Violation(Violation.Type.VENUE_UNAVAILABLE, "Venue '" + v.getName() + "' is marked unavailable.");
        if (v.getCapacity() < students)
            return new Violation(Violation.Type.CAPACITY, "Venue '" + v.getName() + "' seats " + v.getCapacity()
                    + " but " + students + " students sit this examination.");
        return null;
    }

    // ------------------------------------------------------------------ rules involving other examinations

    /** Rule 1: the same venue may not host overlapping examinations (a configurable gap is kept between sittings). */
    public Violation venueClash(Venue v, LocalDate date, int start, int dur, Collection<Placement> sameDay) {
        List<Violation> out = new ArrayList<>(1);
        collectVenueClashes(null, v, date, start, dur, sameDay, out, true);
        return out.isEmpty() ? null : out.get(0);
    }

    private void collectVenueClashes(ExamEvent self, Venue v, LocalDate date, int start, int dur,
                                     Collection<Placement> sameDay, List<Violation> out, boolean firstOnly) {
        int end = start + dur;
        for (Placement q : sameDay) {
            if (q.venue().getId() != v.getId()) continue;
            if (self != null && q.event().getKey().equals(self.getKey())) continue;
            if (overlaps(start, end, q.startMin(), q.endMin(), rules.getGapMin())) {
                String a = self == null ? "(this exam)" : self.codes();
                out.add(new Violation(Violation.Type.VENUE_CLASH, pair("Venue clash in " + v.getName() + " on " + date(date),
                        a, time(start) + "-" + time(end), q.event().codes(), time(q.startMin()) + "-" + time(q.endMin()))));
                if (firstOnly) return;
            }
        }
    }

    /** Rules 2-6: shared students, simultaneity limit, optional strict policies and the daily limit per group. */
    public Violation studentViolation(ExamEvent e, LocalDate date, int start, int dur, Collection<Placement> sameDay) {
        List<Violation> out = new ArrayList<>(1);
        collectStudentViolations(e, date, start, dur, sameDay, out, true);
        return out.isEmpty() ? null : out.get(0);
    }

    private void collectStudentViolations(ExamEvent e, LocalDate date, int start, int dur,
                                          Collection<Placement> sameDay, List<Violation> out, boolean firstOnly) {
        int end = start + dur;
        int gap = rules.getGapMin();
        List<Placement> overlapping = null;
        for (Placement q : sameDay) {
            if (q.event().getKey().equals(e.getKey())) continue;
            ExamEvent o = q.event();
            if (overlaps(start, end, q.startMin(), q.endMin(), gap) && intersects(e.getGroupIds(), o.getGroupIds())) {
                out.add(new Violation(Violation.Type.GROUP_CLASH, pair("Student clash on " + date(date)
                        + " (the two exams share students)", e.codes(), time(start) + "-" + time(end),
                        o.codes(), time(q.startMin()) + "-" + time(q.endMin()))));
                if (firstOnly) return;
            }
            if (overlaps(start, end, q.startMin(), q.endMin(), 0)) {
                if (rules.isNoCrossDepartment() && !sameSingle(e.getDepartmentIds(), o.getDepartmentIds())) {
                    out.add(new Violation(Violation.Type.POLICY_DEPARTMENT, pair("Policy: no simultaneous exams across departments ("
                            + date(date) + ")", e.codes(), time(start) + "-" + time(end),
                            o.codes(), time(q.startMin()) + "-" + time(q.endMin()))));
                    if (firstOnly) return;
                }
                if (rules.isNoCrossLevel() && !sameSingle(e.getLevels(), o.getLevels())) {
                    out.add(new Violation(Violation.Type.POLICY_LEVEL, pair("Policy: no simultaneous exams across levels ("
                            + date(date) + ")", e.codes(), time(start) + "-" + time(end),
                            o.codes(), time(q.startMin()) + "-" + time(q.endMin()))));
                    if (firstOnly) return;
                }
                if (overlapping == null) overlapping = new ArrayList<>();
                overlapping.add(q);
            }
        }
        // Maximum simultaneous examinations: check the busiest moment inside the candidate's interval.
        if (overlapping != null && overlapping.size() + 1 > rules.getMaxSimultaneous()) {
            List<Integer> points = new ArrayList<>();
            points.add(start);
            for (Placement q : overlapping) if (q.startMin() > start) points.add(q.startMin());
            for (int t : points) {
                int count = 1;
                for (Placement q : overlapping) if (q.startMin() <= t && t < q.endMin()) count++;
                if (count > rules.getMaxSimultaneous()) {
                    out.add(new Violation(Violation.Type.MAX_SIMULTANEOUS, "More than " + rules.getMaxSimultaneous()
                            + " examinations would run at the same time on " + date(date) + " at " + time(t) + "."));
                    if (firstOnly) return;
                    break;
                }
            }
        }
        // Daily limit per student group.
        for (long g : e.getGroupIds()) {
            int count = 0;
            for (Placement q : sameDay) {
                if (!q.event().getKey().equals(e.getKey()) && q.event().getGroupIds().contains(g)) count++;
            }
            if (count + 1 > rules.getMaxExamsPerGroupPerDay()) {
                out.add(new Violation(Violation.Type.DAILY_LIMIT, "A student group of " + e.codes() + " would have more than "
                        + rules.getMaxExamsPerGroupPerDay() + " examinations on " + date(date) + "."));
                if (firstOnly) return;
                break;
            }
        }
    }

    // ------------------------------------------------------------------ complete checks

    /**
     * Full check of one proposed placement against every rule.
     * {@code others} are all OTHER placements (any date); the event itself is ignored if present.
     */
    public List<Violation> check(ExamEvent e, LocalDate date, int start, Venue venue, Collection<Placement> others) {
        List<Violation> out = new ArrayList<>();
        Violation v = slotViolation(date, start, e.getDurationMin());
        if (v != null) out.add(v);
        v = venueViolation(venue, e.getStudents());
        if (v != null) out.add(v);
        List<Placement> sameDay = new ArrayList<>();
        for (Placement q : others) {
            if (q.date().equals(date) && !q.event().getKey().equals(e.getKey())) sameDay.add(q);
        }
        collectVenueClashes(e, venue, date, start, e.getDurationMin(), sameDay, out, false);
        collectStudentViolations(e, date, start, e.getDurationMin(), sameDay, out, false);
        return out;
    }

    // ------------------------------------------------------------------ helpers

    static boolean overlaps(int s1, int e1, int s2, int e2, int gap) {
        return s1 < e2 + gap && s2 < e1 + gap;
    }

    private static boolean intersects(Set<Long> a, Set<Long> b) {
        Set<Long> small = a.size() <= b.size() ? a : b;
        Set<Long> big = small == a ? b : a;
        for (Long x : small) if (big.contains(x)) return true;
        return false;
    }

    private static boolean sameSingle(Set<?> a, Set<?> b) { return a.size() == 1 && a.equals(b); }

    /** Builds a message that reads the same no matter which of the two exams is checked first (lets us de-duplicate). */
    private static String pair(String prefix, String aCodes, String aTime, String bCodes, String bTime) {
        if (aCodes.compareTo(bCodes) > 0) { String t = aCodes; aCodes = bCodes; bCodes = t; t = aTime; aTime = bTime; bTime = t; }
        return prefix + ": " + aCodes + " (" + aTime + ") conflicts with " + bCodes + " (" + bTime + ").";
    }
}
