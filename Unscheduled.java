package com.campus.examscheduler.scheduler;

/** An examination that could not be scheduled, with the reason. */
public record Unscheduled(ExamEvent event, String reason) { }
