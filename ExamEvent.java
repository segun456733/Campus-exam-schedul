package com.campus.examscheduler.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * One examination sitting. A normal course is one event; a joint examination
 * (several courses sat together) is ONE event holding all of its courses, so it is
 * never double-booked or double-counted.
 */
public class ExamEvent {
    private final String key;                 // "C<courseId>" or "J<jointId>"
    private final List<Long> courseIds = new ArrayList<>();
    private final List<String> courseCodes = new ArrayList<>();
    private final List<String> titles = new ArrayList<>();
    private final Set<String> departmentNames = new TreeSet<>();
    private final Set<Long> departmentIds = new TreeSet<>();
    private final Set<String> levels = new TreeSet<>();
    private final Set<Long> groupIds = new TreeSet<>();
    private int students;
    private int durationMin;
    private boolean joint;

    public ExamEvent(String key, boolean joint) { this.key = key; this.joint = joint; }

    public void addCourse(long id, String code, String title, long deptId, String deptName,
                          String level, int students, int duration, Set<Long> groups) {
        courseIds.add(id); courseCodes.add(code); titles.add(title);
        departmentIds.add(deptId); departmentNames.add(deptName); levels.add(level);
        groupIds.addAll(groups);
        this.students += students;
        this.durationMin = Math.max(this.durationMin, duration);
    }

    public String getKey() { return key; }
    public List<Long> getCourseIds() { return courseIds; }
    public List<String> getCourseCodes() { return courseCodes; }
    public List<String> getTitles() { return titles; }
    public Set<String> getDepartmentNames() { return departmentNames; }
    public Set<Long> getDepartmentIds() { return departmentIds; }
    public Set<String> getLevels() { return levels; }
    public Set<Long> getGroupIds() { return groupIds; }
    public int getStudents() { return students; }
    public int getDurationMin() { return durationMin; }
    public boolean isJoint() { return joint; }

    public String codes() { return String.join(" / ", courseCodes); }
    public String titlesText() { return String.join(" / ", titles); }
    public String departmentsText() { return String.join(", ", departmentNames); }
    public String levelsText() { return String.join("/", levels); }

    @Override public String toString() { return codes(); }
}
