package com.campus.examscheduler.scheduler;

/** A broken scheduling rule, with a human-readable explanation. */
public record Violation(Type type, String message) {
    public enum Type {
        INVALID_SETTINGS, DATE_RANGE, DAY_NOT_ALLOWED, OUTSIDE_HOURS, BREAK,
        VENUE_UNAVAILABLE, CAPACITY, VENUE_CLASH, GROUP_CLASH,
        MAX_SIMULTANEOUS, POLICY_DEPARTMENT, POLICY_LEVEL, DAILY_LIMIT, DUPLICATE_EVENT
    }
    @Override public String toString() { return message; }
}
