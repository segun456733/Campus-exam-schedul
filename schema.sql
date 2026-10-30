-- Campus Examination Scheduler System - SQLite schema.
-- Executed automatically on first launch (statements are separated by semicolons).
-- Foreign keys are enforced: every connection runs PRAGMA foreign_keys = ON.

CREATE TABLE IF NOT EXISTS app_meta (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS admins (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT NOT NULL UNIQUE COLLATE NOCASE,
    password_hash TEXT NOT NULL,          -- Base64 PBKDF2-HMAC-SHA256 hash (never plaintext)
    salt          TEXT NOT NULL,          -- Base64 random 16-byte salt
    iterations    INTEGER NOT NULL,
    created_at    TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS departments (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    code        TEXT NOT NULL UNIQUE COLLATE NOCASE,
    name        TEXT NOT NULL UNIQUE COLLATE NOCASE,
    description TEXT NOT NULL DEFAULT ''
);

-- One cohort per department and level (e.g. CSC-ND1). Exams sharing a group may never overlap.
CREATE TABLE IF NOT EXISTS student_groups (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT NOT NULL UNIQUE COLLATE NOCASE,
    department_id INTEGER NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    level         TEXT NOT NULL CHECK (level IN ('ND1','ND2')),
    UNIQUE (department_id, level)
);

-- A joint examination: all courses pointing at the same row are sat together as ONE event.
CREATE TABLE IF NOT EXISTS joint_examinations (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE COLLATE NOCASE
);

CREATE TABLE IF NOT EXISTS courses (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    code             TEXT NOT NULL UNIQUE COLLATE NOCASE,
    title            TEXT NOT NULL,
    department_id    INTEGER NOT NULL REFERENCES departments(id) ON DELETE RESTRICT,
    level            TEXT NOT NULL CHECK (level IN ('ND1','ND2')),
    student_count    INTEGER NOT NULL CHECK (student_count > 0),
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes BETWEEN 15 AND 480),
    group_id         INTEGER NOT NULL REFERENCES student_groups(id) ON DELETE RESTRICT,
    joint_exam_id    INTEGER REFERENCES joint_examinations(id) ON DELETE SET NULL
);

-- Additional student groups that also sit a course (shared courses).
CREATE TABLE IF NOT EXISTS course_extra_groups (
    course_id INTEGER NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    group_id  INTEGER NOT NULL REFERENCES student_groups(id) ON DELETE RESTRICT,
    PRIMARY KEY (course_id, group_id)
);

CREATE TABLE IF NOT EXISTS venues (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT NOT NULL UNIQUE COLLATE NOCASE,
    capacity   INTEGER NOT NULL CHECK (capacity > 0),
    venue_type TEXT NOT NULL DEFAULT '',
    available  INTEGER NOT NULL DEFAULT 1 CHECK (available IN (0,1)),
    notes      TEXT NOT NULL DEFAULT ''
);

-- Exactly one settings row (id = 1).
CREATE TABLE IF NOT EXISTS examination_settings (
    id                          INTEGER PRIMARY KEY CHECK (id = 1),
    start_date                  TEXT NOT NULL,
    end_date                    TEXT NOT NULL,
    allowed_days                TEXT NOT NULL,      -- e.g. MONDAY,TUESDAY,...
    day_start                   TEXT NOT NULL,      -- HH:mm
    day_end                     TEXT NOT NULL,      -- HH:mm
    slot_step_minutes           INTEGER NOT NULL,
    gap_minutes                 INTEGER NOT NULL,
    max_simultaneous            INTEGER NOT NULL,
    no_cross_department         INTEGER NOT NULL DEFAULT 0,
    no_cross_level              INTEGER NOT NULL DEFAULT 0,
    max_exams_per_group_per_day INTEGER NOT NULL DEFAULT 2
);

-- Daily break periods (no examination may overlap them).
CREATE TABLE IF NOT EXISTS exam_breaks (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    label      TEXT NOT NULL,
    start_time TEXT NOT NULL,
    end_time   TEXT NOT NULL
);

-- The saved timetable. event_key is "C<courseId>" for a single course or "J<jointId>" for a joint examination.
CREATE TABLE IF NOT EXISTS exam_schedules (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    event_key  TEXT NOT NULL UNIQUE,
    exam_date  TEXT NOT NULL,
    start_time TEXT NOT NULL,
    end_time   TEXT NOT NULL,
    venue_id   INTEGER NOT NULL REFERENCES venues(id) ON DELETE RESTRICT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_courses_department ON courses(department_id);
CREATE INDEX IF NOT EXISTS idx_schedules_date ON exam_schedules(exam_date);
