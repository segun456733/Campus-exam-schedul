package com.campus.examscheduler.scheduler;

/** A time-of-day range in minutes after midnight (end exclusive). Used for daily breaks. */
public record TimeRange(String label, int start, int end) {
    public boolean overlaps(int s, int e) { return s < end && start < e; }
}
