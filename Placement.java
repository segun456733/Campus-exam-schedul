package com.campus.examscheduler.scheduler;

import com.campus.examscheduler.model.Venue;

import java.time.LocalDate;

/** An examination event placed on a date, start time and venue. */
public record Placement(ExamEvent event, LocalDate date, int startMin, Venue venue) {
    public int endMin() { return startMin + event.getDurationMin(); }
}
