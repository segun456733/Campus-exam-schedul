# Architecture

Package root: `com.campus.examscheduler`

| Package | Responsibility |
|---|---|
| `config` | `AppConfig` - names, default admin, writable data folder |
| `model` | Plain data classes: `Admin`, `Department`, `StudentGroup`, `Course`, `Venue` |
| `util` | `PasswordUtil` (PBKDF2), `CsvUtil`, `SimplePdfWriter` (dependency-free PDF), `TimeUtil` |
| `dao` | SQL only: `Db` (connections, transactions, schema), one DAO class per table group, `DemoData` (sample data + seeding) |
| `scheduler` | Pure scheduling logic, **no database and no Swing**: `SchedulingRules`, `ExamEvent(+Builder)`, `ConstraintChecker`, `TimetableGenerator`, `ScheduleValidator` |
| `service` | Business rules and orchestration: validation, transactions, `SchedulingService`, reports, export |
| `ui` | Swing screens (CardLayout navigation), dialogs, theme and shared widgets |

Dependency direction: `ui -> service -> dao / scheduler -> model`. The `scheduler` package never imports `dao` or `ui`,
which is why it can be unit-tested without a database.

## The scheduling engine
1. **Events** - `ExamEventBuilder` turns courses into events. Courses with the same joint examination become ONE event.
2. **Single rule source** - `ConstraintChecker` holds every rule (period, permitted days, hours, breaks, venue availability and capacity,
   venue clash, student-group clash, max simultaneous, strict policies, daily limit per group). The generator, the manual-edit validator
   and the final `ScheduleValidator` all call it, so a manual change can never bypass a rule.
3. **Generation** (`TimetableGenerator`)
   - Pre-check each event alone (capacity vs. biggest available venue, duration vs. daily window) -> reasons for impossible ones.
   - Backtracking search with *most-constrained-first* (fewest remaining slot/venue options) and forward checking
     (a branch with an event that has zero options is abandoned). Options are tried best-first: spread a group's exams over
     different days, then earliest date/time, then smallest suitable venue. Bounded by a step limit and a time limit.
   - If no complete timetable exists, a greedy pass places as many events as possible and gives a reason for every event it cannot place.
   - The result is validated again before it is returned; saving validates a third time.
4. **Threading** - `GeneratePanel` runs generation in a `SwingWorker`; everything else is short and runs on the EDT.

## Saved timetable and course changes
Changing a course (students, duration, groups, joint exam) or deleting it removes its saved timetable row so it appears as
unscheduled instead of keeping a stale placement. Changing venues or settings does not delete anything: the saved timetable is
re-validated against the current rules and problems appear on the dashboard and in the conflict report.
