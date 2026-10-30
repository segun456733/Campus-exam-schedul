package com.campus.examscheduler;

import com.campus.examscheduler.config.AppConfig;
import com.campus.examscheduler.model.Admin;
import com.campus.examscheduler.model.Course;
import com.campus.examscheduler.model.Department;
import com.campus.examscheduler.model.Venue;
import com.campus.examscheduler.scheduler.ScheduleResult;
import com.campus.examscheduler.service.AppInit;
import com.campus.examscheduler.service.AuthService;
import com.campus.examscheduler.service.CourseService;
import com.campus.examscheduler.service.DepartmentService;
import com.campus.examscheduler.service.ExportService;
import com.campus.examscheduler.service.ReportService;
import com.campus.examscheduler.service.ReportTable;
import com.campus.examscheduler.service.SchedulingService;
import com.campus.examscheduler.service.TimetableRow;
import com.campus.examscheduler.service.ValidationException;
import com.campus.examscheduler.service.VenueService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Headless end-to-end check of the REAL application (real SQLite, real services, real scheduler).
 * Run with:  java -jar target\campus-exam-scheduler.jar --selftest
 * It uses a temporary database, so your own data is never touched. build.bat runs it after packaging.
 */
public final class SelfTest {
    private interface Step { void run() throws Exception; }

    private static int failures;
    private static int steps;

    private SelfTest() { }

    private static boolean step(String name, Step s) {
        steps++;
        try {
            s.run();
            System.out.println("  PASS  " + name);
            return true;
        } catch (Throwable t) {
            failures++;
            System.out.println("  FAIL  " + name + "  ->  " + t);
            return false;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void mustReject(Step s, String what) throws Exception {
        try {
            s.run();
        } catch (ValidationException expected) {
            return;
        }
        throw new IllegalStateException(what + " was NOT rejected");
    }

    /** @return 0 when every step passed, 1 otherwise */
    public static int run() {
        System.out.println("Campus Examination Scheduler - self-test (temporary database)");
        Path dir = null;
        try {
            dir = Files.createTempDirectory("examsched-selftest");
            Path db = dir.resolve("selftest.db");
            final Path dbFile = db;
            final Path tmp = dir;

            if (!step("SQLite JDBC driver is inside the JAR", () -> Class.forName("org.sqlite.JDBC"))) {
                System.out.println("  The SQLite driver is missing from the JAR - the build did not bundle dependencies.");
                return 1;
            }
            step("Database file and tables are created on first launch", () -> {
                AppInit.start(dbFile);
                require(Files.exists(dbFile), "database file was not created");
            });
            step("Demonstration data loaded (10 departments, 60+ courses, venues)", () -> {
                require(DepartmentService.list().size() == 10, "expected 10 departments");
                require(CourseService.list().size() >= 60, "expected 60+ courses");
                require(VenueService.list().size() >= 5, "expected venues");
            });
            step("Login works with the default account, wrong password is rejected", () -> {
                Admin a = AuthService.login(AppConfig.DEFAULT_ADMIN_USERNAME, AppConfig.DEFAULT_ADMIN_PASSWORD.toCharArray());
                require(!a.getPasswordHash().contains(AppConfig.DEFAULT_ADMIN_PASSWORD), "password stored in plaintext");
                mustReject(() -> AuthService.login("admin", "wrong-password".toCharArray()), "wrong password");
            });
            step("Password change works and old password stops working", () -> {
                Admin a = AuthService.login("admin", AppConfig.DEFAULT_ADMIN_PASSWORD.toCharArray());
                AuthService.changePassword(a.getId(), AppConfig.DEFAULT_ADMIN_PASSWORD, "Temp12345", "Temp12345");
                mustReject(() -> AuthService.login("admin", AppConfig.DEFAULT_ADMIN_PASSWORD.toCharArray()), "old password");
                Admin b = AuthService.login("admin", "Temp12345".toCharArray());
                AuthService.changePassword(b.getId(), "Temp12345", AppConfig.DEFAULT_ADMIN_PASSWORD, AppConfig.DEFAULT_ADMIN_PASSWORD);
            });
            step("Department add / duplicate rejected / edit / delete", () -> {
                DepartmentService.save(new Department(0, "GEO", "Geology", "test"));
                mustReject(() -> DepartmentService.save(new Department(0, "GEO", "Other Name", "")), "duplicate department code");
                Department geo = DepartmentService.list().stream().filter(d -> d.getCode().equals("GEO")).findFirst().orElseThrow();
                geo.setDescription("edited");
                DepartmentService.save(geo);
                DepartmentService.delete(geo.getId());
                require(DepartmentService.list().size() == 10, "department was not deleted");
            });
            step("Venue add / duplicate rejected / delete", () -> {
                VenueService.save(new Venue(0, "Test Room", 40, "Classroom", true));
                mustReject(() -> VenueService.save(new Venue(0, "Test Room", 50, "", true)), "duplicate venue");
                mustReject(() -> VenueService.save(new Venue(0, "Bad Room", 0, "", true)), "zero capacity");
                Venue v = VenueService.list().stream().filter(x -> x.getName().equals("Test Room")).findFirst().orElseThrow();
                VenueService.delete(v.getId());
            });
            step("Course validation and duplicate rejection", () -> {
                Course ref = CourseService.list().get(0);
                Course dup = new Course();
                dup.setCode(ref.getCode()); dup.setTitle("Another title"); dup.setDepartmentId(ref.getDepartmentId());
                dup.setLevel("ND1"); dup.setStudentCount(20); dup.setDurationMinutes(60); dup.setGroupId(ref.getGroupId());
                mustReject(() -> CourseService.save(dup), "duplicate course code");
                Course bad = new Course();
                bad.setCode("BAD111"); bad.setTitle("Bad course"); bad.setDepartmentId(ref.getDepartmentId());
                bad.setLevel("ND1"); bad.setStudentCount(0); bad.setDurationMinutes(60); bad.setGroupId(ref.getGroupId());
                mustReject(() -> CourseService.save(bad), "zero students");
            });
            step("Automatic timetable: complete, conflict-free, saved", () -> {
                ScheduleResult r = SchedulingService.generate(0, null);
                require(r.isComplete(), "timetable incomplete: " + r.getUnscheduled() + " " + r.getErrors());
                SchedulingService.save(r);
                require(SchedulingService.validateSaved().isEmpty(), "saved timetable has conflicts");
                require(SchedulingService.stats().unscheduled() == 0, "unscheduled exams remain");
            });
            step("Manual reschedule that clashes with the same student group is rejected", () -> {
                TimetableRow a = find("CSC112");
                TimetableRow b = find("CSC113");
                int start = Integer.parseInt(a.start().substring(0, 2)) * 60 + Integer.parseInt(a.start().substring(3));
                require(!SchedulingService.checkMove(b.eventKey(), a.date(), start, a.venueId()).isEmpty(), "clash not detected");
                mustReject(() -> SchedulingService.applyMove(b.eventKey(), a.date(), start, a.venueId()), "clashing move");
                require(find("CSC113").date().equals(b.date()), "rejected move changed the timetable");
            });
            step("Oversized examination is reported as unscheduled with a reason", () -> {
                Course ref = CourseService.list().get(0);
                Course big = new Course();
                big.setCode("BIG999"); big.setTitle("Oversized exam"); big.setDepartmentId(ref.getDepartmentId());
                big.setLevel("ND1"); big.setStudentCount(4000); big.setDurationMinutes(60); big.setGroupId(ref.getGroupId());
                CourseService.save(big);
                ScheduleResult r = SchedulingService.generate(0, null);
                require(!r.isComplete(), "claimed complete although an exam cannot be placed");
                require(r.getUnscheduled().stream().anyMatch(u -> u.event().codes().equals("BIG999")
                        && u.reason().toLowerCase().contains("capacity")), "no capacity reason reported");
                Course saved = CourseService.list().stream().filter(c -> c.getCode().equals("BIG999")).findFirst().orElseThrow();
                CourseService.delete(saved.getId());
            });
            step("CSV and PDF exports are valid files", () -> {
                ReportTable table = ReportService.build(ReportService.Type.COMPLETE, null);
                require(!table.rows().isEmpty(), "report is empty");
                Path csv = tmp.resolve("timetable.csv");
                Path pdf = tmp.resolve("timetable.pdf");
                ExportService.exportCsv(table, csv);
                ExportService.exportPdf(table, pdf);
                String csvText = Files.readString(csv, StandardCharsets.UTF_8);
                require(csvText.contains("Course Code") && csvText.lines().count() > 20, "CSV content wrong");
                String head = new String(Files.readAllBytes(pdf), StandardCharsets.ISO_8859_1);
                require(head.startsWith("%PDF-") && head.trim().endsWith("%%EOF"), "PDF structure wrong");
                for (ReportService.Type t : ReportService.Type.values()) {
                    String param = t == ReportService.Type.DEPARTMENT ? DepartmentService.list().get(0).getName()
                            : t == ReportService.Type.VENUE ? VenueService.list().get(0).getName() : null;
                    require(ReportService.build(t, param).columns().length > 0, "report " + t + " failed");
                }
            });
            final int scheduledBefore = SchedulingService.stats().scheduled();
            step("Data persists after closing and reopening the database", () -> {
                AppInit.start(dbFile);          // simulates restarting the application
                require(DepartmentService.list().size() == 10, "departments lost or re-seeded");
                require(SchedulingService.stats().scheduled() == scheduledBefore, "saved timetable lost");
                require(SchedulingService.stats().scheduled() > 0, "no timetable was saved");
            });
            step("Demonstration data can be removed and reloaded", () -> {
                AppInit.clearAllData();
                require(DepartmentService.list().isEmpty(), "data not removed");
                AppInit.resetDemoData();
                require(DepartmentService.list().size() == 10, "demo data not restored");
            });
        } catch (Throwable t) {
            failures++;
            System.out.println("  FAIL  self-test could not run: " + t);
        } finally {
            deleteQuietly(dir);
        }
        System.out.println(failures == 0
                ? "SELF-TEST PASSED (" + steps + " checks)"
                : "SELF-TEST FAILED (" + failures + " of " + steps + " checks failed)");
        return failures == 0 ? 0 : 1;
    }

    private static TimetableRow find(String codes) {
        return SchedulingService.timetable().stream().filter(r -> r.codes().equals(codes)).findFirst().orElseThrow();
    }

    private static void deleteQuietly(Path dir) {
        if (dir == null) return;
        try (Stream<Path> files = Files.walk(dir)) {
            files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (Exception ignored) {
            // temporary folder cleanup is best-effort
        }
    }
}
