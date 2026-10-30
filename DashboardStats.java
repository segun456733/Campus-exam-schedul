package com.campus.examscheduler.service;

import java.util.List;

/** Figures shown on the dashboard - all computed from the database. */
public record DashboardStats(int departments, int courses, int venues, int availableVenues, int totalExams,
                             int scheduled, int unscheduled, int examDays, int conflicts, List<TimetableRow> upcoming,
                             boolean upcomingIsFuture) { }
