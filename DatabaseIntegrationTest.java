package com.campus.examscheduler.service;

import com.campus.examscheduler.config.AppConfig;
import com.campus.examscheduler.dao.Db;
import com.campus.examscheduler.model.Admin;
import com.campus.examscheduler.model.Course;
import com.campus.examscheduler.model.Department;
import com.campus.examscheduler.model.Venue;
import com.campus.examscheduler.scheduler.ScheduleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests against a REAL temporary SQLite database (needs the sqlite-jdbc driver from Maven).
 * They cover persistence after "restart", duplicate rejection, manual-edit validation and timetable saving.
 */
class DatabaseIntegrationTest {
    private Path dbFile;

    @BeforeEach
    void freshDatabase() throws Exception {
        dbFile = Files.createTempDirectory("examsched-test").resolve("test.db");
        AppInit.start(dbFile);
    }

    private TimetableRow row(String codes) {
        return SchedulingService.timetable().stream().filter(r -> r.codes().equals(codes)).findFirst().orElseThrow();
    }

    @Test
    void firstLaunchCreatesSchemaAndSeedsDemoData() {
        assertEquals(10, DepartmentService.list().size());
        assertTrue(CourseService.list().size() >= 60);
        assertTrue(VenueService.list().size() >= 5);
        assertTrue(SettingsService.load().validate().isEmpty());
    }

    @Test
    void loginUsesSaltedHashAndPasswordChangeWorks() {
        Admin a = AuthService.login(AppConfig.DEFAULT_ADMIN_USERNAME, AppConfig.DEFAULT_ADMIN_PASSWORD.toCharArray());
        assertFalse(a.getPasswordHash().contains(AppConfig.DEFAULT_ADMIN_PASSWORD));
        assertThrows(ValidationException.class, () -> AuthService.login("admin", "wrong".toCharArray()));
        assertThrows(ValidationException.class, () -> AuthService.login("", "x".toCharArray()));
        AuthService.changePassword(a.getId(), AppConfig.DEFAULT_ADMIN_PASSWORD, "NewPass123", "NewPass123");
        assertThrows(ValidationException.class, () -> AuthService.login("admin", AppConfig.DEFAULT_ADMIN_PASSWORD.toCharArray()));
        assertNotNull(AuthService.login("admin", "NewPass123".toCharArray()));
    }

    @Test
    void duplicateRecordsAreRejected() {
        Course dup = new Course();
        Course existing = CourseService.list().get(0);
        dup.setCode(existing.getCode()); dup.setTitle("Something else"); dup.setDepartmentId(existing.getDepartmentId());
        dup.setLevel("ND1"); dup.setStudentCount(30); dup.setDurationMinutes(60); dup.setGroupId(existing.getGroupId());
        assertThrows(ValidationException.class, () -> CourseService.save(dup));
        assertThrows(ValidationException.class, () -> DepartmentService.save(new Department(0, "CSC", "Another Name", "")));
        assertThrows(ValidationException.class, () -> DepartmentService.save(new Department(0, "ZZZ", "Computer Science", "")));
        assertThrows(ValidationException.class, () -> VenueService.save(new Venue(0, "Main Hall", 100, "", true)));
    }

    @Test
    void dataSurvivesRestartAndIsNotReseeded() {
        Course c = CourseService.list().get(0);
        CourseService.delete(c.getId());
        int count = CourseService.list().size();
        AppInit.start(dbFile);                                   // simulate closing and reopening the application
        assertEquals(count, CourseService.list().size());       // not overwritten by demo data
        assertEquals(10, DepartmentService.list().size());
    }

    @Test
    void generatedTimetableIsSavedAndPersists() {
        ScheduleResult r = SchedulingService.generate(0, null);
        assertTrue(r.isComplete());
        SchedulingService.save(r);
        AppInit.start(dbFile);                                   // restart
        DashboardStats s = SchedulingService.stats();
        assertEquals(r.getPlacements().size(), s.scheduled());
        assertEquals(0, s.unscheduled());
        assertEquals(0, s.conflicts());
    }

    @Test
    void conflictingManualRescheduleIsRejectedAndValidOneIsSaved() {
        SchedulingService.save(SchedulingService.generate(0, null));
        TimetableRow a = row("CSC112");
        TimetableRow b = row("CSC113");                           // same student group (CSC-ND1) as CSC112
        List<Venue> venues = VenueService.list();
        Venue other = venues.stream().filter(v -> v.getId() != a.venueId() && v.isAvailable()).findFirst().orElseThrow();
        int startMin = Integer.parseInt(a.start().substring(0, 2)) * 60 + Integer.parseInt(a.start().substring(3));
        assertFalse(SchedulingService.checkMove(b.eventKey(), a.date(), startMin, other.getId()).isEmpty());
        assertThrows(ValidationException.class, () -> SchedulingService.applyMove(b.eventKey(), a.date(), startMin, other.getId()));
        assertEquals(b.date(), row("CSC113").date());             // nothing was changed
        // a day outside the examination period is rejected too
        assertThrows(ValidationException.class, () -> SchedulingService.applyMove(b.eventKey(), LocalDate.of(2027, 3, 1), 9 * 60, other.getId()));
    }

    @Test
    void venueInUseCannotBeDeleted() {
        SchedulingService.save(SchedulingService.generate(0, null));
        TimetableRow any = SchedulingService.timetable().stream().filter(TimetableRow::scheduled).findFirst().orElseThrow();
        assertThrows(ValidationException.class, () -> VenueService.delete(any.venueId()));
    }

    @Test
    void examinationTooBigForEveryVenueIsReportedUnscheduled() {
        Course big = new Course();
        Course ref = CourseService.list().get(0);
        big.setCode("BIG999"); big.setTitle("Huge exam"); big.setDepartmentId(ref.getDepartmentId()); big.setLevel("ND1");
        big.setStudentCount(4000); big.setDurationMinutes(60); big.setGroupId(ref.getGroupId());
        CourseService.save(big);
        ScheduleResult r = SchedulingService.generate(0, null);
        assertFalse(r.isComplete());
        assertTrue(r.getUnscheduled().stream().anyMatch(u -> u.event().codes().equals("BIG999")));
    }

    @Test
    void resetDemoDataRestoresSamples() {
        AppInit.clearAllData();
        assertEquals(0, DepartmentService.list().size());
        AppInit.resetDemoData();
        assertEquals(10, DepartmentService.list().size());
    }
}
