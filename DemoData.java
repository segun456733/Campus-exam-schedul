package com.campus.examscheduler.dao;

import com.campus.examscheduler.model.Course;
import com.campus.examscheduler.model.Department;
import com.campus.examscheduler.model.Venue;
import com.campus.examscheduler.scheduler.SchedulingRules;
import com.campus.examscheduler.scheduler.TimeRange;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DEMONSTRATION DATA ONLY. Departments, courses, student numbers and venues below are invented sample
 * records for demonstrating the system. A real institution must replace them with its own data.
 */
public final class DemoData {
    private DemoData() { }

    /** code, name, description */
    public static final String[][] DEPARTMENTS = {
        {"CSC", "Computer Science", "Computing, programming and information technology"},
        {"STA", "Statistics", "Statistical theory and data analysis"},
        {"MTH", "Mathematics", "Pure and applied mathematics"},
        {"PHY", "Physics", "Classical and modern physics"},
        {"CHM", "Chemistry", "Organic, inorganic and physical chemistry"},
        {"BIO", "Biology", "General and applied biology"},
        {"MCB", "Microbiology", "Study of micro-organisms"},
        {"BCH", "Biochemistry", "Chemistry of living organisms"},
        {"SLT", "Science Laboratory Technology", "Laboratory techniques and instrumentation"},
        {"FST", "Food Science and Technology", "Food processing, safety and nutrition"},
    };

    /** Students per cohort: {ND1, ND2} for each department in the order above. */
    private static final int[][] COHORT = {
        {110, 95}, {80, 70}, {90, 75}, {70, 60}, {75, 65}, {100, 85}, {85, 70}, {65, 55}, {60, 50}, {55, 45}
    };

    /** code | title | department | level | students (0 = whole cohort) | minutes | extra shared groups | joint exam name */
    public static final String[] COURSES = {
        // ---- ND1
        "CSC111|Introduction to Computing|CSC|ND1|0|120||ICT Foundations (Joint)",
        "CSC112|Programming Fundamentals|CSC|ND1|0|120||",
        "CSC113|Digital Logic Basics|CSC|ND1|0|90||",
        "STA111|Descriptive Statistics|STA|ND1|0|90||",
        "STA112|Introduction to Probability|STA|ND1|0|120||",
        "STA119|Communication in English I|STA|ND1|0|90||English I (Joint)",
        "MTH111|Elementary Algebra|MTH|ND1|0|120||",
        "MTH112|Calculus I (shared course)|MTH|ND1|350|120|CSC-ND1,STA-ND1,PHY-ND1|",
        "MTH119|Communication in English I|MTH|ND1|0|90||English I (Joint)",
        "PHY111|Mechanics|PHY|ND1|0|120||",
        "PHY112|Heat and Thermodynamics|PHY|ND1|0|90||",
        "PHY114|Computer Appreciation|PHY|ND1|0|120||ICT Foundations (Joint)",
        "PHY119|Communication in English I|PHY|ND1|0|90||English I (Joint)",
        "CHM111|General Chemistry I|CHM|ND1|0|120||",
        "CHM112|Organic Chemistry I|CHM|ND1|0|120||",
        "CHM113|Chemistry Practicals (Theory)|CHM|ND1|0|60||",
        "BIO111|General Biology I|BIO|ND1|0|120||",
        "BIO112|Cell Biology|BIO|ND1|0|90||",
        "BIO113|Plant Biology|BIO|ND1|0|90||",
        "MCB111|Introduction to Microbiology|MCB|ND1|0|120||",
        "MCB112|Microbial Techniques|MCB|ND1|0|90||",
        "MCB113|Basic Bacteriology|MCB|ND1|0|60||",
        "BCH111|Biomolecules|BCH|ND1|0|120||",
        "BCH112|Introduction to Biochemistry|BCH|ND1|0|90||",
        "BCH113|Laboratory Safety and Methods|BCH|ND1|0|60||",
        "SLT111|Laboratory Techniques|SLT|ND1|0|120||",
        "SLT112|Laboratory Safety and Ethics|SLT|ND1|0|60||",
        "SLT114|Computer Appreciation|SLT|ND1|0|120||ICT Foundations (Joint)",
        "FST111|Introduction to Food Science|FST|ND1|0|120||",
        "FST112|Food Chemistry|FST|ND1|0|90||",
        "FST113|Nutrition Basics|FST|ND1|0|60||",
        // ---- ND2
        "CSC211|Data Structures|CSC|ND2|0|120||",
        "CSC212|Database Systems|CSC|ND2|0|120||",
        "CSC213|Computer Networks|CSC|ND2|0|90||",
        "STA211|Probability Theory (shared course)|STA|ND2|240|120|CSC-ND2,MTH-ND2|",
        "STA212|Statistical Inference|STA|ND2|0|90||",
        "STA213|Sampling Techniques|STA|ND2|0|90||",
        "MTH211|Linear Algebra|MTH|ND2|0|120||",
        "MTH212|Differential Equations|MTH|ND2|0|120||",
        "MTH213|Numerical Methods|MTH|ND2|0|90||",
        "PHY211|Electricity and Magnetism|PHY|ND2|0|120||",
        "PHY212|Optics|PHY|ND2|0|90||",
        "PHY213|Modern Physics|PHY|ND2|0|90||",
        "CHM211|Inorganic Chemistry|CHM|ND2|0|120||",
        "CHM212|Physical Chemistry|CHM|ND2|0|120||",
        "CHM213|Analytical Chemistry|CHM|ND2|0|90||",
        "BIO211|Genetics|BIO|ND2|0|120||",
        "BIO212|Ecology|BIO|ND2|0|90||",
        "BIO213|Animal Physiology|BIO|ND2|0|90||",
        "MCB211|Virology|MCB|ND2|0|120||",
        "MCB212|Immunology|MCB|ND2|0|90||",
        "MCB213|Industrial Microbiology|MCB|ND2|0|90||",
        "BCH211|Enzymology|BCH|ND2|0|120||",
        "BCH212|Metabolism|BCH|ND2|0|120||",
        "BCH213|Clinical Biochemistry|BCH|ND2|0|90||",
        "SLT211|Instrumentation|SLT|ND2|0|120||",
        "SLT212|Quality Control|SLT|ND2|0|90||",
        "SLT213|Laboratory Management|SLT|ND2|0|60||",
        "FST211|Food Processing|FST|ND2|0|120||",
        "FST212|Food Safety|FST|ND2|0|90||",
        "FST213|Food Microbiology|FST|ND2|0|90||",
    };

    /** name, capacity, type, available, notes */
    public static final Object[][] VENUES = {
        {"Main Hall", 500, "Hall", true, "Largest hall"},
        {"Lecture Theatre A", 300, "Lecture theatre", true, ""},
        {"Lecture Theatre B", 250, "Lecture theatre", true, ""},
        {"Science Auditorium", 200, "Auditorium", true, ""},
        {"Old Annex Hall", 150, "Hall", false, "Under renovation (demonstrates a disabled venue)"},
        {"Lab Hall 1", 120, "Laboratory", true, ""},
        {"Room 101", 80, "Classroom", true, ""},
        {"Room 102", 80, "Classroom", true, ""},
        {"Room 201", 60, "Classroom", true, ""},
        {"Room 202", 60, "Classroom", true, ""},
    };

    /** Sample examination period: Mon 16 Nov - Fri 27 Nov 2026, 08:00-17:00, lunch break 12:00-13:00. */
    public static SchedulingRules demoRules() {
        SchedulingRules r = new SchedulingRules();
        r.setStartDate(LocalDate.of(2026, 11, 16));
        r.setEndDate(LocalDate.of(2026, 11, 27));
        r.setAllowedDays(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));
        r.setDayStartMin(8 * 60);
        r.setDayEndMin(17 * 60);
        r.setSlotStepMin(30);
        r.setGapMin(30);
        r.setMaxSimultaneous(4);
        r.setNoCrossDepartment(false);
        r.setNoCrossLevel(false);
        r.setMaxExamsPerGroupPerDay(2);
        r.setBreaks(new ArrayList<>(List.of(new TimeRange("Lunch break", 12 * 60, 13 * 60))));
        return r;
    }

    public static List<Venue> demoVenues() {
        List<Venue> list = new ArrayList<>();
        long id = 1;
        for (Object[] v : VENUES) {
            Venue venue = new Venue(id++, (String) v[0], (Integer) v[1], (String) v[2], (Boolean) v[3]);
            venue.setNotes((String) v[4]);
            list.add(venue);
        }
        return list;
    }

    /** Group names are "<DEPT>-<LEVEL>", e.g. CSC-ND1. */
    public static String groupName(String deptCode, String level) { return deptCode + "-" + level; }

    /** Builds course objects. The id maps connect the sample rows to real (or, in tests, synthetic) ids. */
    public static List<Course> buildCourses(Map<String, Long> deptIdByCode, Map<String, Long> groupIdByName,
                                            Map<String, Long> jointIdByName) {
        Map<String, String> deptNames = new HashMap<>();
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < DEPARTMENTS.length; i++) { deptNames.put(DEPARTMENTS[i][0], DEPARTMENTS[i][1]); index.put(DEPARTMENTS[i][0], i); }
        List<Course> courses = new ArrayList<>();
        long id = 1;
        for (String row : COURSES) {
            String[] f = row.split("\\|", -1);
            Course c = new Course();
            c.setId(id++);
            c.setCode(f[0]); c.setTitle(f[1]);
            c.setDepartmentId(deptIdByCode.get(f[2])); c.setDepartmentName(deptNames.get(f[2]));
            c.setLevel(f[3]);
            int students = Integer.parseInt(f[4]);
            if (students == 0) students = COHORT[index.get(f[2])][f[3].equals("ND1") ? 0 : 1];
            c.setStudentCount(students);
            c.setDurationMinutes(Integer.parseInt(f[5]));
            String own = groupName(f[2], f[3]);
            c.setGroupName(own); c.setGroupId(groupIdByName.get(own));
            if (!f[6].isBlank()) {
                for (String g : f[6].split(",")) { c.getExtraGroupNames().add(g.trim()); c.getExtraGroupIds().add(groupIdByName.get(g.trim())); }
            }
            if (!f[7].isBlank()) { c.setJointName(f[7]); c.setJointExamId(jointIdByName.get(f[7])); }
            courses.add(c);
        }
        return courses;
    }

    // ------------------------------------------------------------------ database seeding

    /** Inserts all sample data (settings, departments, groups, venues, courses). Call inside a transaction. */
    public static void seed(Connection c) throws SQLException {
        Map<String, Long> deptIds = new HashMap<>();
        Map<String, Long> groupIds = new HashMap<>();
        for (String[] d : DEPARTMENTS) {
            long id = DepartmentDao.insert(c, new Department(0, d[0], d[1], d[2]));
            deptIds.put(d[0], id);
            for (String level : new String[]{"ND1", "ND2"}) {
                groupIds.put(groupName(d[0], level), DepartmentDao.insertGroup(c, groupName(d[0], level), id, level));
            }
        }
        Map<String, Long> jointIds = new HashMap<>();
        Set<String> jointNames = new LinkedHashSet<>();
        for (String row : COURSES) { String j = row.split("\\|", -1)[7]; if (!j.isBlank()) jointNames.add(j); }
        for (String j : jointNames) jointIds.put(j, CourseDao.findOrCreateJoint(c, j));
        for (Course course : buildCourses(deptIds, groupIds, jointIds)) CourseDao.insert(c, course);
        for (Venue v : demoVenues()) VenueDao.insert(c, v);
        SettingsDao.save(c, demoRules());
    }

    /** Removes all institution data (departments, courses, venues, settings, timetable). Administrator accounts are kept. */
    public static void wipe(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate("DELETE FROM exam_schedules");
            st.executeUpdate("DELETE FROM course_extra_groups");
            st.executeUpdate("DELETE FROM courses");
            st.executeUpdate("DELETE FROM joint_examinations");
            st.executeUpdate("DELETE FROM student_groups");
            st.executeUpdate("DELETE FROM departments");
            st.executeUpdate("DELETE FROM venues");
            st.executeUpdate("DELETE FROM exam_breaks");
            st.executeUpdate("DELETE FROM examination_settings");
        }
    }
}
