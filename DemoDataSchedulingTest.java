package com.campus.examscheduler.scheduler;

import com.campus.examscheduler.dao.DemoData;
import com.campus.examscheduler.model.Course;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The bundled demonstration data must schedule completely (so the lecturer demo always works). */
class DemoDataSchedulingTest {

    private List<Course> demoCourses() {
        Map<String, Long> depts = new HashMap<>();
        Map<String, Long> groups = new HashMap<>();
        Map<String, Long> joints = new HashMap<>();
        long id = 1;
        for (String[] d : DemoData.DEPARTMENTS) {
            depts.put(d[0], id++);
            groups.put(DemoData.groupName(d[0], "ND1"), id++);
            groups.put(DemoData.groupName(d[0], "ND2"), id++);
        }
        for (String row : DemoData.COURSES) {
            String j = row.split("\\|", -1)[7];
            if (!j.isBlank()) joints.putIfAbsent(j, id++);
        }
        return DemoData.buildCourses(depts, groups, joints);
    }

    @Test
    void demoDataHas10DepartmentsAndBothLevels() {
        assertEquals(10, DemoData.DEPARTMENTS.length);
        List<Course> courses = demoCourses();
        assertTrue(courses.stream().anyMatch(c -> c.getLevel().equals("ND1")));
        assertTrue(courses.stream().anyMatch(c -> c.getLevel().equals("ND2")));
        assertTrue(courses.size() >= 60);
    }

    @Test
    void demoDataSchedulesCompletelyAndWithoutConflicts() {
        List<String> warnings = new java.util.ArrayList<>();
        List<ExamEvent> events = ExamEventBuilder.build(demoCourses(), warnings);
        assertTrue(warnings.isEmpty(), warnings.toString());
        SchedulingRules rules = DemoData.demoRules();
        assertTrue(rules.validate().isEmpty());
        ScheduleResult r = new TimetableGenerator(rules).generate(events, DemoData.demoVenues(), null);
        assertTrue(r.isComplete(), "unscheduled=" + r.getUnscheduled() + " errors=" + r.getErrors());
        assertEquals(events.size(), r.getPlacements().size());
        assertTrue(ScheduleValidator.validate(r.getPlacements(), rules).isEmpty());
        // there is real parallelism: some pair of exams runs at the same time
        boolean simultaneous = false;
        for (Placement a : r.getPlacements())
            for (Placement b : r.getPlacements())
                if (a != b && a.date().equals(b.date()) && a.startMin() < b.endMin() && b.startMin() < a.endMin()) simultaneous = true;
        assertTrue(simultaneous);
        // the disabled venue is never used
        for (Placement p : r.getPlacements()) assertFalse(p.venue().getName().equals("Old Annex Hall"));
        System.out.println("   demo: " + events.size() + " events scheduled in " + r.getElapsedMs() + " ms, nodes=" + r.getNodesExplored());
    }

    @Test
    void demoDataSchedulesWithStrictPolicies() {
        List<ExamEvent> events = ExamEventBuilder.build(demoCourses(), null);
        SchedulingRules rules = DemoData.demoRules();
        rules.setNoCrossLevel(true);          // only one level at a time -> still must be valid
        ScheduleResult r = new TimetableGenerator(rules).generate(events, DemoData.demoVenues(), null);
        assertTrue(ScheduleValidator.validate(r.getPlacements(), rules).isEmpty());
        assertEquals(events.size(), r.getPlacements().size() + r.getUnscheduled().size());   // nothing silently dropped
    }
}
