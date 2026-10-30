package com.campus.examscheduler.service;

import com.campus.examscheduler.model.Course;
import com.campus.examscheduler.model.Department;
import com.campus.examscheduler.model.Venue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Requirement 12: invalid course / department / venue records are rejected (pure field validation, no database). */
class ValidationRulesTest {

    private Course validCourse() {
        Course c = new Course();
        c.setCode("CSC111"); c.setTitle("Introduction to Computing"); c.setDepartmentId(1); c.setLevel("ND1");
        c.setStudentCount(80); c.setDurationMinutes(120); c.setGroupId(5);
        return c;
    }

    @Test
    void validCourseIsAccepted() {
        assertTrue(CourseService.validate(validCourse()).isEmpty());
    }

    @Test
    void invalidCoursesAreRejected() {
        Course c = validCourse(); c.setCode("");
        assertFalse(CourseService.validate(c).isEmpty());
        c = validCourse(); c.setCode("bad code!");
        assertFalse(CourseService.validate(c).isEmpty());
        c = validCourse(); c.setTitle("");
        assertFalse(CourseService.validate(c).isEmpty());
        c = validCourse(); c.setStudentCount(0);
        assertFalse(CourseService.validate(c).isEmpty());
        c = validCourse(); c.setStudentCount(-4);
        assertFalse(CourseService.validate(c).isEmpty());
        c = validCourse(); c.setDurationMinutes(0);
        assertFalse(CourseService.validate(c).isEmpty());
        c = validCourse(); c.setDurationMinutes(7);
        assertFalse(CourseService.validate(c).isEmpty());
        c = validCourse(); c.setDurationMinutes(900);
        assertFalse(CourseService.validate(c).isEmpty());
        c = validCourse(); c.setLevel("HND1");
        assertFalse(CourseService.validate(c).isEmpty());
        c = validCourse(); c.setDepartmentId(0);
        assertFalse(CourseService.validate(c).isEmpty());
        c = validCourse(); c.setGroupId(0);
        assertFalse(CourseService.validate(c).isEmpty());
        c = validCourse(); c.setExtraGroupIds(new java.util.ArrayList<>(List.of(5L)));    // own group listed as shared
        assertFalse(CourseService.validate(c).isEmpty());
    }

    @Test
    void invalidDepartmentsAreRejected() {
        assertTrue(DepartmentService.validate(new Department(0, "CSC", "Computer Science", "")).isEmpty());
        assertFalse(DepartmentService.validate(new Department(0, "", "Computer Science", "")).isEmpty());
        assertFalse(DepartmentService.validate(new Department(0, "C", "Computer Science", "")).isEmpty());
        assertFalse(DepartmentService.validate(new Department(0, "CSC", "  ", "")).isEmpty());
    }

    @Test
    void invalidVenuesAreRejected() {
        assertTrue(VenueService.validate(new Venue(0, "Hall A", 100, "Hall", true)).isEmpty());
        assertFalse(VenueService.validate(new Venue(0, "", 100, "Hall", true)).isEmpty());
        assertFalse(VenueService.validate(new Venue(0, "Hall A", 0, "Hall", true)).isEmpty());
        assertFalse(VenueService.validate(new Venue(0, "Hall A", -5, "Hall", true)).isEmpty());
    }
}
