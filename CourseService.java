package com.campus.examscheduler.service;

import com.campus.examscheduler.dao.CourseDao;
import com.campus.examscheduler.dao.Db;
import com.campus.examscheduler.dao.DepartmentDao;
import com.campus.examscheduler.dao.ScheduleDao;
import com.campus.examscheduler.model.Course;
import com.campus.examscheduler.model.StudentGroup;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CourseService {
    private CourseService() { }

    public static List<Course> list() { return Db.read(CourseDao::findAll); }

    /** Search by code/title text, optionally filtered by department and level (null = no filter). */
    public static List<Course> search(String text, Long departmentId, String level) {
        String needle = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        List<Course> out = new ArrayList<>();
        for (Course c : list()) {
            if (departmentId != null && c.getDepartmentId() != departmentId) continue;
            if (level != null && !level.equals(c.getLevel())) continue;
            if (!needle.isEmpty() && !(c.getCode().toLowerCase(Locale.ROOT).contains(needle)
                    || c.getTitle().toLowerCase(Locale.ROOT).contains(needle))) continue;
            out.add(c);
        }
        return out;
    }

    public static List<StudentGroup> groups() { return Db.read(DepartmentDao::findAllGroups); }
    public static List<String> jointNames() { return Db.read(CourseDao::findAllJointNames); }

    /** Pure field validation (requirement: invalid course records are rejected). Testable without a database. */
    public static List<String> validate(Course c) {
        List<String> errors = new ArrayList<>();
        String code = c.getCode() == null ? "" : c.getCode().trim();
        String title = c.getTitle() == null ? "" : c.getTitle().trim();
        if (code.isEmpty()) errors.add("Course code is required.");
        else if (!code.matches("[A-Za-z0-9\\-]{3,12}")) errors.add("Course code must be 3-12 letters/digits without spaces (for example CSC111).");
        if (title.isEmpty()) errors.add("Course title is required.");
        else if (title.length() < 3 || title.length() > 120) errors.add("Course title must be between 3 and 120 characters.");
        if (c.getDepartmentId() <= 0) errors.add("Select a department.");
        if (!"ND1".equals(c.getLevel()) && !"ND2".equals(c.getLevel())) errors.add("Student level must be ND1 or ND2.");
        if (c.getStudentCount() < 1) errors.add("Number of students must be at least 1.");
        else if (c.getStudentCount() > 5000) errors.add("Number of students must not exceed 5000.");
        if (c.getDurationMinutes() < 15 || c.getDurationMinutes() > 480) errors.add("Examination duration must be between 15 and 480 minutes.");
        else if (c.getDurationMinutes() % 5 != 0) errors.add("Examination duration must be a multiple of 5 minutes.");
        if (c.getGroupId() <= 0) errors.add("Select the student group (cohort).");
        if (c.getExtraGroupIds().contains(c.getGroupId())) errors.add("The shared groups must not include the course's own student group.");
        if (c.getJointName() != null && c.getJointName().length() > 80) errors.add("Joint examination name must not exceed 80 characters.");
        return errors;
    }

    /** Converts "CSC-ND1, STA-ND1" into group ids. */
    public static List<Long> resolveGroups(String commaSeparated) {
        List<Long> ids = new ArrayList<>();
        if (commaSeparated == null || commaSeparated.isBlank()) return ids;
        List<StudentGroup> all = groups();
        List<String> unknown = new ArrayList<>();
        Set<Long> unique = new LinkedHashSet<>();
        for (String name : commaSeparated.split(",")) {
            String n = name.trim();
            if (n.isEmpty()) continue;
            StudentGroup match = null;
            for (StudentGroup g : all) if (g.getName().equalsIgnoreCase(n)) match = g;
            if (match == null) unknown.add(n); else unique.add(match.getId());
        }
        if (!unknown.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (StudentGroup g : all) names.add(g.getName());
            throw new ValidationException("Unknown student group(s): " + String.join(", ", unknown)
                    + ". Existing groups: " + String.join(", ", names));
        }
        ids.addAll(unique);
        return ids;
    }

    /** Adds (id == 0) or updates a course. Any saved timetable entry affected by the change is removed so it is rescheduled. */
    public static void save(Course course) {
        course.setCode(course.getCode() == null ? "" : course.getCode().trim().toUpperCase());
        course.setTitle(course.getTitle() == null ? "" : course.getTitle().trim());
        List<String> errors = validate(course);
        if (!errors.isEmpty()) throw new ValidationException(errors);
        Db.inTransaction(c -> {
            Course sameCode = CourseDao.findByCode(c, course.getCode());
            if (sameCode != null && sameCode.getId() != course.getId())
                throw new ValidationException("A course with the code '" + course.getCode() + "' already exists.");
            for (Course other : CourseDao.findAll(c)) {
                if (other.getId() != course.getId() && other.getDepartmentId() == course.getDepartmentId()
                        && other.getLevel().equals(course.getLevel()) && other.getTitle().equalsIgnoreCase(course.getTitle()))
                    throw new ValidationException("The course '" + other.getCode() + " - " + other.getTitle()
                            + "' already exists for this department and level.");
            }
            Course old = course.getId() == 0 ? null : CourseDao.findById(c, course.getId());
            Long jointId = CourseDao.findOrCreateJoint(c, course.getJointName());
            course.setJointExamId(jointId);
            if (course.getId() == 0) course.setId(CourseDao.insert(c, course)); else CourseDao.update(c, course);
            invalidate(c, course.getId(), old == null ? null : old.getJointExamId(), jointId);
            CourseDao.deleteUnusedJoints(c);
            return null;
        });
    }

    public static void delete(long id) {
        Db.inTransaction(c -> {
            Course old = CourseDao.findById(c, id);
            if (old == null) throw new ValidationException("The course no longer exists.");
            invalidate(c, id, old.getJointExamId(), null);
            CourseDao.delete(c, id);
            CourseDao.deleteUnusedJoints(c);
            return null;
        });
    }

    private static void invalidate(java.sql.Connection c, long courseId, Long oldJoint, Long newJoint) throws java.sql.SQLException {
        ScheduleDao.delete(c, "C" + courseId);
        if (oldJoint != null) ScheduleDao.delete(c, "J" + oldJoint);
        if (newJoint != null) ScheduleDao.delete(c, "J" + newJoint);
    }
}
