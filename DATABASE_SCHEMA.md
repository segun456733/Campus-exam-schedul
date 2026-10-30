# Database schema

SQLite file: `%APPDATA%\CampusExamScheduler\exam_scheduler.db` (Windows). The folder is writable and independent of where the
application is installed. Override with `-Dexamscheduler.home=C:\some\folder`.
The schema is in `src/main/resources/schema.sql` and is created automatically on first launch.
Every connection runs `PRAGMA foreign_keys = ON`. All queries use prepared statements; related changes use transactions.

| Table | Purpose | Key constraints |
|---|---|---|
| `app_meta` | Flags such as `demo_seeded` (demo data is loaded ONLY once) | PK `key` |
| `admins` | Administrator accounts. Stores PBKDF2-HMAC-SHA256 hash, salt and iteration count - never the password | `username` unique |
| `departments` | Departments: `code`, `name`, `description` | `code`, `name` unique (case-insensitive) |
| `student_groups` | One cohort per department and level, e.g. `CSC-ND1` | FK `department_id` (cascade); unique `(department_id, level)`; level CHECK ND1/ND2 |
| `joint_examinations` | Names of joint (shared-sitting) examinations | `name` unique |
| `courses` | Code, title, department, level, `student_count`, `duration_minutes`, cohort `group_id`, optional `joint_exam_id` | `code` unique; CHECK `student_count > 0`, duration 15-480; FK department RESTRICT |
| `course_extra_groups` | Extra student groups that also sit a (shared) course | PK `(course_id, group_id)`; FKs |
| `venues` | `name`, `capacity`, `venue_type`, `available` (0/1), `notes` | `name` unique; CHECK capacity > 0 |
| `examination_settings` | The single settings row (`id = 1`): dates, permitted days, daily hours, slot step, gap, max simultaneous, strict policies, max exams per group per day | CHECK `id = 1` |
| `exam_breaks` | Daily break periods (label, start, end) | - |
| `exam_schedules` | The saved timetable. `event_key` is `C<courseId>` (one course) or `J<jointId>` (a joint examination = ONE event) | `event_key` unique; FK `venue_id` RESTRICT |

## Why students are modelled as groups
Two examinations clash when they share at least one student group. A course always involves its own cohort (`courses.group_id`)
plus any `course_extra_groups`. Joint examinations merge several courses into one event whose groups are the union of the courses'
groups and whose student count is the sum of the courses' student counts.

## Relationships
```
departments 1--* student_groups 1--* courses *--0..1 joint_examinations
courses *--* student_groups   (course_extra_groups)
venues 1--* exam_schedules          exam_schedules.event_key -> course or joint exam (logical link)
```
