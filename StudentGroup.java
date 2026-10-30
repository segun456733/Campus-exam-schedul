package com.campus.examscheduler.model;

/** A student cohort, e.g. "CSC-ND1". Two exams that share a group can never overlap. */
public class StudentGroup {
    private final long id;
    private final String name;
    private final long departmentId;
    private final String level;

    public StudentGroup(long id, String name, long departmentId, String level) {
        this.id = id; this.name = name; this.departmentId = departmentId; this.level = level;
    }
    public long getId() { return id; }
    public String getName() { return name; }
    public long getDepartmentId() { return departmentId; }
    public String getLevel() { return level; }
    @Override public String toString() { return name; }
}
