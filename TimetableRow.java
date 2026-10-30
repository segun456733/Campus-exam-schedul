package com.campus.examscheduler.service;

import java.time.LocalDate;

/** One line of the timetable screen/reports: a scheduled or unscheduled examination event. */
public record TimetableRow(String eventKey, LocalDate date, String day, String start, String end, String codes,
                           String titles, String departments, String levels, int students, int durationMin,
                           long venueId, String venue, boolean scheduled, String note) {
    public String status() { return scheduled ? "Scheduled" : "Unscheduled"; }
}
