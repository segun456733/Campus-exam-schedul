package com.campus.examscheduler.scheduler;

import com.campus.examscheduler.model.Course;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Turns course records into examination events, merging joint examinations into a single event. */
public final class ExamEventBuilder {
    private ExamEventBuilder() { }

    public static List<ExamEvent> build(List<Course> courses, List<String> warnings) {
        Map<String, ExamEvent> events = new LinkedHashMap<>();
        Map<String, Integer> firstDuration = new LinkedHashMap<>();
        for (Course c : courses) {
            boolean joint = c.getJointExamId() != null;
            String key = joint ? "J" + c.getJointExamId() : "C" + c.getId();
            ExamEvent ev = events.computeIfAbsent(key, k -> new ExamEvent(k, joint));
            // Every student group affected by this course: its own cohort plus any shared groups.
            Set<Long> groups = new HashSet<>(c.getExtraGroupIds());
            groups.add(c.getGroupId());
            if (joint && firstDuration.containsKey(key) && firstDuration.get(key) != c.getDurationMinutes() && warnings != null) {
                warnings.add("Joint examination '" + c.getJointName() + "' has courses with different durations; "
                        + "the longest duration is used.");
            }
            firstDuration.putIfAbsent(key, c.getDurationMinutes());
            ev.addCourse(c.getId(), c.getCode(), c.getTitle(), c.getDepartmentId(), c.getDepartmentName(),
                    c.getLevel(), c.getStudentCount(), c.getDurationMinutes(), groups);
        }
        return new java.util.ArrayList<>(events.values());
    }
}
