package com.campus.examscheduler.service;

import com.campus.examscheduler.scheduler.Violation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Builds the report tables (complete, department, ND1, ND2, venue, unscheduled, conflicts). */
public final class ReportService {
    private ReportService() { }

    public enum Type {
        COMPLETE("Complete examination timetable"), DEPARTMENT("Department timetable"), ND1("ND1 timetable"),
        ND2("ND2 timetable"), VENUE("Venue timetable"), UNSCHEDULED("Unscheduled examinations"),
        CONFLICTS("Conflict / validation report");
        private final String label;
        Type(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    private static final String[] TIMETABLE_COLUMNS = {"Date", "Day", "Start", "End", "Course Code", "Course Title",
            "Department", "Level", "Students", "Venue", "Status"};

    /** @param parameter department name (DEPARTMENT) or venue name (VENUE); ignored by the other reports */
    public static ReportTable build(Type type, String parameter) {
        String stamp = "Generated " + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        switch (type) {
            case UNSCHEDULED: return unscheduled(stamp);
            case CONFLICTS: return conflicts(stamp);
            default: break;
        }
        List<TimetableRow> rows = SchedulingService.timetable();
        List<String[]> out = new ArrayList<>();
        String title = type.toString();
        for (TimetableRow r : rows) {
            if (!r.scheduled()) continue;
            boolean keep = switch (type) {
                case COMPLETE -> true;
                case DEPARTMENT -> parameter != null && r.departments().contains(parameter);
                case ND1 -> r.levels().contains("ND1");
                case ND2 -> r.levels().contains("ND2");
                case VENUE -> parameter != null && r.venue().equals(parameter);
                default -> false;
            };
            if (keep) out.add(new String[]{r.date().toString(), r.day(), r.start(), r.end(), r.codes(), r.titles(),
                    r.departments(), r.levels(), String.valueOf(r.students()), r.venue(), r.status()});
        }
        if (type == Type.DEPARTMENT) title += ": " + parameter;
        if (type == Type.VENUE) title += ": " + parameter;
        return new ReportTable(title, stamp + " - " + out.size() + " scheduled examination(s)", TIMETABLE_COLUMNS, out);
    }

    private static ReportTable unscheduled(String stamp) {
        List<String[]> out = new ArrayList<>();
        for (TimetableRow r : SchedulingService.timetable()) {
            if (r.scheduled()) continue;
            out.add(new String[]{r.codes(), r.titles(), r.departments(), r.levels(), String.valueOf(r.students()),
                    String.valueOf(r.durationMin()), r.note()});
        }
        return new ReportTable("Unscheduled examinations", stamp + " - " + out.size() + " examination(s) without a slot",
                new String[]{"Course Code", "Course Title", "Department", "Level", "Students", "Duration (min)", "Reason"}, out);
    }

    private static ReportTable conflicts(String stamp) {
        List<String[]> out = new ArrayList<>();
        int n = 1;
        for (Violation v : SchedulingService.validateSaved()) out.add(new String[]{String.valueOf(n++), v.type().name(), v.message()});
        var last = SchedulingService.getLastGeneration();
        if (last != null) for (String w : last.getWarnings()) out.add(new String[]{String.valueOf(n++), "WARNING", w});
        if (out.isEmpty()) out.add(new String[]{"-", "OK", "No scheduling conflicts were detected in the saved timetable."});
        return new ReportTable("Scheduling conflict and validation report", stamp,
                new String[]{"#", "Type", "Description"}, out);
    }
}
