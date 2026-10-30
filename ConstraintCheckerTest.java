package com.campus.examscheduler.scheduler;

import com.campus.examscheduler.model.Venue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.campus.examscheduler.scheduler.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

/** Tests of the scheduling rules (requirements 1-11, 14 and 16). */
class ConstraintCheckerTest {
    private SchedulingRules rules;
    private ConstraintChecker checker;
    private Venue hall;
    private Venue room;

    @BeforeEach
    void setUp() {
        rules = rules();
        checker = new ConstraintChecker(rules);
        hall = venue(1, "Main Hall", 300, true);
        room = venue(2, "Room 1", 50, true);
    }

    private static boolean has(List<Violation> list, Violation.Type t) {
        return list.stream().anyMatch(v -> v.type() == t);
    }

    @Test
    void test01_sameVenueCannotHostOverlappingExams() {
        ExamEvent a = event(1, "AAA101", 1, "ND1", 40, 120, 11);
        ExamEvent b = event(2, "BBB101", 2, "ND1", 40, 120, 22);
        Placement pa = new Placement(a, monday(), 8 * 60, hall);
        assertTrue(has(checker.check(b, monday(), 9 * 60, hall, List.of(pa)), Violation.Type.VENUE_CLASH));
        // Back-to-back in the same venue is fine when the gap is 0.
        assertFalse(has(checker.check(b, monday(), 10 * 60, hall, List.of(pa)), Violation.Type.VENUE_CLASH));
    }

    @Test
    void test02_sameStudentGroupCannotOverlap() {
        ExamEvent a = event(1, "AAA101", 1, "ND1", 40, 120, 11, 99);   // 99 = shared group
        ExamEvent b = event(2, "BBB101", 2, "ND1", 40, 120, 22, 99);
        Placement pa = new Placement(a, monday(), 8 * 60, hall);
        assertTrue(has(checker.check(b, monday(), 9 * 60, room, List.of(pa)), Violation.Type.GROUP_CLASH));
    }

    @Test
    void test03_sameDepartmentAndLevelCannotOverlapWhenStudentsShared() {
        ExamEvent a = event(1, "CSC111", 1, "ND1", 40, 120, 11);
        ExamEvent b = event(2, "CSC112", 1, "ND1", 40, 120, 11);        // same cohort
        Placement pa = new Placement(a, monday(), 8 * 60, hall);
        assertTrue(has(checker.check(b, monday(), 9 * 60, room, List.of(pa)), Violation.Type.GROUP_CLASH));
    }

    @Test
    void test04_differentDepartmentsMaySitSimultaneouslyWhenAllowed() {
        ExamEvent a = event(1, "CSC111", 1, "ND1", 40, 120, 11);
        ExamEvent b = event(2, "PHY111", 2, "ND1", 30, 120, 22);
        Placement pa = new Placement(a, monday(), 8 * 60, hall);
        assertTrue(checker.check(b, monday(), 8 * 60, room, List.of(pa)).isEmpty());
        // ...but the strict policy forbids it.
        rules.setNoCrossDepartment(true);
        assertTrue(has(new ConstraintChecker(rules).check(b, monday(), 8 * 60, room, List.of(pa)),
                Violation.Type.POLICY_DEPARTMENT));
        rules.setNoCrossDepartment(false);
        rules.setNoCrossLevel(true);
        ExamEvent c = event(3, "PHY211", 2, "ND2", 30, 120, 23);
        assertTrue(has(new ConstraintChecker(rules).check(c, monday(), 8 * 60, room, List.of(pa)),
                Violation.Type.POLICY_LEVEL));
    }

    @Test
    void test05_venueCapacityIsRespected() {
        ExamEvent big = event(1, "AAA101", 1, "ND1", 80, 60, 11);
        assertTrue(has(checker.check(big, monday(), 8 * 60, room, List.of()), Violation.Type.CAPACITY));
        assertTrue(checker.check(big, monday(), 8 * 60, hall, List.of()).isEmpty());
    }

    @Test
    void test06_unavailableVenueIsNeverAccepted() {
        Venue closed = venue(3, "Closed Hall", 500, false);
        ExamEvent a = event(1, "AAA101", 1, "ND1", 10, 60, 11);
        assertTrue(has(checker.check(a, monday(), 8 * 60, closed, List.of()), Violation.Type.VENUE_UNAVAILABLE));
    }

    @Test
    void test07_dateMustBeInsideExaminationPeriod() {
        ExamEvent a = event(1, "AAA101", 1, "ND1", 10, 60, 11);
        assertTrue(has(checker.check(a, LocalDate.of(2026, 11, 23), 8 * 60, hall, List.of()), Violation.Type.DATE_RANGE));
        assertTrue(has(checker.check(a, LocalDate.of(2026, 11, 13), 8 * 60, hall, List.of()), Violation.Type.DATE_RANGE));
    }

    @Test
    void test08_prohibitedDaysAreRejected() {
        rules.setEndDate(LocalDate.of(2026, 11, 22));        // include the weekend in the range
        ExamEvent a = event(1, "AAA101", 1, "ND1", 10, 60, 11);
        LocalDate saturday = LocalDate.of(2026, 11, 21);
        assertTrue(has(new ConstraintChecker(rules).check(a, saturday, 9 * 60, hall, List.of()), Violation.Type.DAY_NOT_ALLOWED));
    }

    @Test
    void test09_dailyHoursAndDurationAreRespected() {
        ExamEvent a = event(1, "AAA101", 1, "ND1", 10, 180, 11);
        assertTrue(has(checker.check(a, monday(), 7 * 60, hall, List.of()), Violation.Type.OUTSIDE_HOURS));   // too early
        assertTrue(has(checker.check(a, monday(), 15 * 60, hall, List.of()), Violation.Type.OUTSIDE_HOURS));  // ends 18:00
        assertTrue(checker.check(a, monday(), 14 * 60, hall, List.of()).isEmpty());                          // ends 17:00
    }

    @Test
    void test10_breakPeriodsAreRespected() {
        ExamEvent a = event(1, "AAA101", 1, "ND1", 10, 120, 11);
        assertTrue(has(checker.check(a, monday(), 11 * 60, hall, List.of()), Violation.Type.BREAK));          // 11-13 crosses lunch
        assertTrue(has(checker.check(a, monday(), 12 * 60 + 30, hall, List.of()), Violation.Type.BREAK));
        assertTrue(checker.check(a, monday(), 13 * 60, hall, List.of()).isEmpty());
        assertTrue(checker.check(a, monday(), 10 * 60, hall, List.of()).isEmpty());                           // ends exactly 12:00
    }

    @Test
    void test11_jointExaminationIsOneSharedEvent() {
        com.campus.examscheduler.model.Course c1 = course(1, "MTH119", 1, 90, 120, 11L, 77L);
        com.campus.examscheduler.model.Course c2 = course(2, "STA119", 2, 80, 120, 22L, 77L);
        com.campus.examscheduler.model.Course c3 = course(3, "PHY111", 3, 50, 90, 33L, null);
        List<String> warnings = new java.util.ArrayList<>();
        List<ExamEvent> events = ExamEventBuilder.build(List.of(c1, c2, c3), warnings);
        assertEquals(2, events.size());                         // joint pair = ONE event + the normal course
        ExamEvent joint = events.get(0);
        assertTrue(joint.isJoint());
        assertEquals(170, joint.getStudents());                 // students counted once per course, not duplicated
        assertEquals(2, joint.getCourseCodes().size());
        assertTrue(joint.getGroupIds().containsAll(List.of(11L, 22L)));
        // Scheduling it needs a venue for all 170 students.
        ScheduleResult r = new TimetableGenerator(rules).generate(events,
                List.of(room, hall), null);
        assertTrue(r.isComplete());
        assertEquals(2, r.getPlacements().size());
        Placement p = r.getPlacements().stream().filter(x -> x.event().isJoint()).findFirst().orElseThrow();
        assertEquals("Main Hall", p.venue().getName());
    }

    private static com.campus.examscheduler.model.Course course(long id, String code, long dept, int students,
                                                                 int duration, long group, Long joint) {
        com.campus.examscheduler.model.Course c = new com.campus.examscheduler.model.Course();
        c.setId(id); c.setCode(code); c.setTitle(code + " title"); c.setDepartmentId(dept);
        c.setDepartmentName("Dept" + dept); c.setLevel("ND1"); c.setStudentCount(students);
        c.setDurationMinutes(duration); c.setGroupId(group); c.setJointExamId(joint);
        return c;
    }

    @Test
    void test14_manualChangesAreValidatedWithTheSameRules() {
        ExamEvent a = event(1, "AAA101", 1, "ND1", 40, 120, 11);
        ExamEvent b = event(2, "AAA102", 1, "ND1", 40, 120, 11);
        Placement pa = new Placement(a, monday(), 8 * 60, hall);
        // moving b onto a's time (same cohort) is rejected with an explanation
        List<Violation> v = checker.check(b, monday(), 9 * 60, hall, List.of(pa));
        assertFalse(v.isEmpty());
        assertTrue(v.get(0).message().length() > 20);
        // a valid move is accepted
        assertTrue(checker.check(b, monday(), 13 * 60, hall, List.of(pa)).isEmpty());
        // moving an event onto itself never clashes with its own old placement
        assertTrue(checker.check(a, monday(), 8 * 60, hall, List.of(pa)).isEmpty());
    }

    @Test
    void test16_finalValidatorDetectsRemainingConflicts() {
        ExamEvent a = event(1, "AAA101", 1, "ND1", 40, 120, 11);
        ExamEvent b = event(2, "AAA102", 1, "ND1", 40, 120, 11);
        List<Placement> bad = List.of(new Placement(a, monday(), 8 * 60, hall), new Placement(b, monday(), 9 * 60, hall));
        List<Violation> v = ScheduleValidator.validate(bad, rules);
        assertTrue(has(v, Violation.Type.VENUE_CLASH));
        assertTrue(has(v, Violation.Type.GROUP_CLASH));
        List<Placement> good = List.of(new Placement(a, monday(), 8 * 60, hall), new Placement(b, monday(), 13 * 60, hall));
        assertTrue(ScheduleValidator.validate(good, rules).isEmpty());
    }

    @Test
    void maxSimultaneousExamsIsRespected() {
        rules.setMaxSimultaneous(2);
        ConstraintChecker ck = new ConstraintChecker(rules);
        Venue v3 = venue(3, "Room 3", 50, true);
        ExamEvent a = event(1, "A", 1, "ND1", 10, 120, 11);
        ExamEvent b = event(2, "B", 2, "ND1", 10, 120, 22);
        ExamEvent c = event(3, "C", 3, "ND1", 10, 120, 33);
        List<Placement> existing = List.of(new Placement(a, monday(), 8 * 60, hall), new Placement(b, monday(), 8 * 60, room));
        assertTrue(has(ck.check(c, monday(), 9 * 60, v3, existing), Violation.Type.MAX_SIMULTANEOUS));
        assertFalse(has(ck.check(c, monday(), 13 * 60, v3, existing), Violation.Type.MAX_SIMULTANEOUS));
    }

    @Test
    void settingsValidationCatchesBadPeriods() {
        rules.setEndDate(LocalDate.of(2026, 11, 1));
        assertFalse(rules.validate().isEmpty());
        rules = rules();
        rules.setDayEndMin(7 * 60);
        assertFalse(rules.validate().isEmpty());
        rules = rules();
        assertTrue(rules.validate().isEmpty());
    }
}
