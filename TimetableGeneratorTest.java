package com.campus.examscheduler.scheduler;

import com.campus.examscheduler.model.Venue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.campus.examscheduler.scheduler.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

/** Tests of the automatic generator (requirements 4, 5, 7-13, 16). */
class TimetableGeneratorTest {
    private SchedulingRules rules;
    private List<Venue> venues;

    @BeforeEach
    void setUp() {
        rules = rules();
        venues = new ArrayList<>(List.of(venue(1, "Hall", 300, true), venue(2, "Room A", 80, true),
                venue(3, "Room B", 80, true), venue(4, "Closed", 1000, false)));
    }

    private List<ExamEvent> manyEvents() {
        List<ExamEvent> events = new ArrayList<>();
        long id = 1;
        for (long dept = 1; dept <= 6; dept++) {
            for (int lv = 1; lv <= 2; lv++) {
                long group = dept * 10 + lv;
                for (int k = 0; k < 3; k++) {
                    events.add(event(id, "D" + dept + "L" + lv + "C" + k, dept, "ND" + lv, 40 + k * 10, 60 + 30 * k, group));
                    id++;
                }
            }
        }
        return events;
    }

    @Test
    void generatesCompleteConflictFreeTimetable() {
        List<ExamEvent> events = manyEvents();
        ScheduleResult r = new TimetableGenerator(rules).generate(events, venues, null);
        assertTrue(r.isComplete(), "errors=" + r.getErrors() + " unscheduled=" + r.getUnscheduled());
        assertEquals(events.size(), r.getPlacements().size());
        assertTrue(ScheduleValidator.validate(r.getPlacements(), rules).isEmpty());
        for (Placement p : r.getPlacements()) {
            assertFalse(p.venue().getName().equals("Closed"));            // unavailable venue never used
            assertTrue(p.venue().getCapacity() >= p.event().getStudents());
            assertTrue(p.date().getDayOfWeek().getValue() <= 5);
            assertTrue(p.startMin() >= 8 * 60 && p.endMin() <= 17 * 60);
            assertFalse(p.startMin() < 13 * 60 && p.endMin() > 12 * 60);   // never touches the lunch break
        }
    }

    @Test
    void differentDepartmentsRunSimultaneouslyWhenAllowed() {
        List<ExamEvent> events = new ArrayList<>();
        for (long i = 1; i <= 3; i++) events.add(event(i, "X" + i, i, "ND1", 40, 120, i * 10));   // 3 available venues
        rules.setDayEndMin(10 * 60);            // only one 2-hour slot per day -> must overlap in time
        rules.setBreaks(new ArrayList<>());
        rules.setEndDate(LocalDate.of(2026, 11, 16));   // single day
        ScheduleResult r = new TimetableGenerator(rules).generate(events, venues, null);
        assertTrue(r.isComplete(), r.getUnscheduled().toString());
        assertEquals(3, r.getPlacements().size());
    }

    @Test
    void strictPolicyPreventsSimultaneousDepartments() {
        List<ExamEvent> events = new ArrayList<>();
        for (long i = 1; i <= 2; i++) events.add(event(i, "X" + i, i, "ND1", 40, 120, i * 10));
        rules.setDayEndMin(10 * 60);
        rules.setBreaks(new ArrayList<>());
        rules.setEndDate(LocalDate.of(2026, 11, 16));
        rules.setNoCrossDepartment(true);
        ScheduleResult r = new TimetableGenerator(rules).generate(events, venues, null);
        assertEquals(1, r.getPlacements().size());          // second one cannot fit at all
        assertEquals(1, r.getUnscheduled().size());
        assertTrue(r.getUnscheduled().get(0).reason().length() > 10);
        assertFalse(r.isComplete());
    }

    @Test
    void examLargerThanEveryVenueIsReportedUnscheduled() {
        ExamEvent huge = event(99, "HUGE101", 1, "ND1", 5000, 120, 11);
        ExamEvent ok = event(98, "OK101", 1, "ND1", 30, 120, 12);
        ScheduleResult r = new TimetableGenerator(rules).generate(List.of(huge, ok), venues, null);
        assertEquals(1, r.getPlacements().size());
        assertEquals(1, r.getUnscheduled().size());
        assertEquals("HUGE101", r.getUnscheduled().get(0).event().codes());
        assertTrue(r.getUnscheduled().get(0).reason().contains("capacity"));
        assertFalse(r.isComplete());          // never claims completeness
    }

    @Test
    void examLongerThanTheDayIsReportedUnscheduled() {
        ExamEvent longExam = event(1, "LONG101", 1, "ND1", 30, 600, 11);
        ScheduleResult r = new TimetableGenerator(rules).generate(List.of(longExam), venues, null);
        assertEquals(1, r.getUnscheduled().size());
        assertTrue(r.getUnscheduled().get(0).reason().contains("duration") || r.getUnscheduled().get(0).reason().contains("Duration"));
    }

    @Test
    void overloadedPeriodGivesPartialTimetableWithReasons() {
        // 12 exams for the SAME student group, max 1 per day, 1 day only -> at most 1 can be placed.
        List<ExamEvent> events = new ArrayList<>();
        for (long i = 1; i <= 12; i++) events.add(event(i, "S" + i, 1, "ND1", 30, 120, 11));
        rules.setEndDate(LocalDate.of(2026, 11, 16));
        rules.setMaxExamsPerGroupPerDay(2);
        ScheduleResult r = new TimetableGenerator(rules).generate(events, venues, null);
        assertEquals(2, r.getPlacements().size());
        assertEquals(10, r.getUnscheduled().size());
        assertTrue(r.isUsedFallback());
        assertFalse(r.isComplete());
        assertTrue(ScheduleValidator.validate(r.getPlacements(), rules).isEmpty());   // what IS placed is valid
        for (Unscheduled u : r.getUnscheduled()) assertTrue(u.reason().length() > 20);
    }

    @Test
    void invalidSettingsAreReportedAndNothingIsScheduled() {
        rules.setEndDate(LocalDate.of(2026, 11, 1));
        ScheduleResult r = new TimetableGenerator(rules).generate(manyEvents(), venues, null);
        assertFalse(r.getErrors().isEmpty());
        assertEquals(0, r.getPlacements().size());
        assertEquals(manyEvents().size(), r.getUnscheduled().size());
    }

    @Test
    void regenerateVariantProducesAnotherValidTimetable() {
        List<ExamEvent> events = manyEvents();
        TimetableGenerator.Options o = new TimetableGenerator.Options();
        o.variant = 3;
        ScheduleResult r = new TimetableGenerator(rules).generate(events, venues, o);
        assertTrue(r.isComplete());
        assertTrue(ScheduleValidator.validate(r.getPlacements(), rules).isEmpty());
    }
}
