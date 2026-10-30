# Campus Examination Scheduler System

Offline Windows desktop application (Java 17+, Swing, SQLite, Maven) that automatically builds clash-free examination timetables for
10 science departments (ND1 and ND2), manages venues and settings, lets the administrator edit and validate changes, and exports/prints reports.
**No internet, web server or cloud service is needed to run it.**

## 1. Quick start (first-time user, Windows 10/11)
**Prerequisites** (install once):
1. **JDK 17 or newer** (a JDK, not only a JRE) - e.g. Eclipse Temurin 17 or 21 from https://adoptium.net. Tick "Add to PATH" / "Set JAVA_HOME" in the installer.
2. **Apache Maven 3.9+** - download the *Binary zip archive* from https://maven.apache.org/download.cgi, unzip to `C:\Tools\apache-maven-3.9.9`, add `C:\Tools\apache-maven-3.9.9\bin` to PATH.
3. Internet for the **first build only** (Maven downloads SQLite JDBC, JUnit and plugins once). After that the application and `build.bat offline` need no internet.

**Build and start**
1. Extract the ZIP to a normal folder, e.g. `C:\CampusExamScheduler` (avoid OneDrive/Desktop-synced folders and folders needing admin rights).
2. Double-click **`build.bat`**. It checks Java and Maven, compiles, **runs all JUnit tests**, packages `target\campus-exam-scheduler.jar`, and then
   **starts the packaged JAR twice to prove it works**: `--version` (the main class is inside the JAR and the manifest is right) and `--selftest`
   (a headless end-to-end run on real SQLite: login, CRUD, scheduling, clashes, exports, persistence). If any step fails the script stops with `[ERROR]`.
3. Double-click **`run.bat`** (or `java -jar target\campus-exam-scheduler.jar`) to open the application. Log in with the account in section 2.
   (`build.bat skiptests` skips the unit tests - not recommended. `build.bat offline` builds without internet once dependencies are cached.)

### Troubleshooting
| Message | Cause and fix |
|---|---|
| `Could not find or load main class com.campus.examscheduler.Main` / `ClassNotFoundException` | The JAR does not contain `Main.class`. This happens when `src\main\java\com\campus\examscheduler\Main.java` is missing from the project folder (Maven still reports BUILD SUCCESS because no other class needs it). `build.bat` now detects this: it runs `java -jar ... --version` and stops with an error, and the test `MainClassPackagingTest` fails the build. Re-extract the ZIP and make sure the folder `src` is complete. |
| `'mvn' is not recognized` | Maven is not on PATH - see prerequisites, then open a NEW command window. |
| `Java ... is too old` / `release version 17 not supported` | Install JDK 17+ and make sure `java -version` shows 17 or higher. |
| `Could not resolve dependencies` / `Connection refused` | First build needs internet (Maven Central). Check proxy/firewall. |
| `The SQLite driver is missing from the JAR` (self-test) | The shaded JAR was not built; run `mvn clean package` and read the first error. |
| Window does not appear | Run `run.bat` from a command window and read the error text; the database folder is `%APPDATA%\CampusExamScheduler`. |

## 2. Login
| Username | Password |
|---|---|
| `admin` | `Admin@123` |

Change it in **Administrator Settings > Change password** (8+ characters, letters and digits). Passwords are stored only as salted PBKDF2-HMAC-SHA256 hashes.
If you forget the new password, close the app and delete the database file (see section 4) - the default account and sample data are recreated.

## 3. Features
- Local administrator login, change password, logout.
- Dashboard with live statistics (departments, courses, venues, scheduled/unscheduled, examination days, conflicts) and upcoming examinations.
- Departments, Courses (search + filters, shared student groups, joint examinations) and Venues (capacity, enable/disable) - add / edit / delete with validation and duplicate protection.
- Examination Settings: dates, permitted weekdays, daily hours, break periods, start-time step, gap between sessions, max simultaneous exams,
  optional strict policies (no simultaneous exams across departments / levels), max exams per student group per day.
- **Automatic timetable generation**: backtracking + most-constrained-first heuristic + forward checking, runs in a `SwingWorker`;
  preview, regenerate (alternative timetable), save, clear (with confirmation), unscheduled list with reasons, warnings/errors tab.
- Timetable screen with filters (date, department, level, venue, course code, status) and **manual rescheduling / venue change validated by the same rules**.
- Reports: complete, per department, ND1, ND2, per venue, unscheduled, conflict/validation. **CSV export, PDF export (built-in writer, no library needed), printing** (Java print API).
- Demonstration data (10 departments, 61 courses, 10 venues incl. one disabled, sample period) loaded **only on first launch**; can be reloaded or removed from Administrator Settings (with confirmation).

### Scheduling rules enforced (all in `ConstraintChecker`)
Same venue never overlaps - same student group never overlaps (this covers same department+level, shared courses and joint exams) -
different departments may run in parallel unless the strict policies are on - durations respected - exams only inside the period, permitted days and daily hours,
never across breaks - venue must be available and big enough - joint exams are ONE event - max simultaneous exams - max exams per group per day.
Exams that cannot be placed are listed as **unscheduled with a reason**; the generator never claims completeness when something is missing.

## 4. Where data is stored
`%APPDATA%\CampusExamScheduler\exam_scheduler.db` (writable, independent of the install folder). Created automatically. Details: `docs/DATABASE_SCHEMA.md`.
To reset everything, close the app and delete that file. To restore only the samples use *Administrator Settings > Reload demonstration data*.

## 5. Testing
`mvn test` (or `build.bat`) runs JUnit 5 tests in `src/test/java`:
- `ConstraintCheckerTest` - rules 1-11, 14, 16 (venue/group/department clashes, capacity, unavailable venue, dates, prohibited days, hours, breaks, joint exams, manual-change validation, final validator)
- `TimetableGeneratorTest` - complete timetables, simultaneity, strict policy, unscheduled reporting, partial timetables, invalid settings, regenerate
- `DemoDataSchedulingTest` - the sample data always schedules completely
- `ValidationRulesTest`, `SecurityAndExportTest` - invalid records, hashing, CSV, PDF
- `DatabaseIntegrationTest` - real SQLite: first-launch seeding, login/password change, duplicates, persistence after restart, manual-move rejection, venue protection, unscheduled reporting
See `docs/VERIFICATION.md` for exactly which of these were already executed and which you must run yourself.

## 6. Windows EXE (optional)
Run `build.bat` first.
`package-exe.bat` creates `dist\CampusExamScheduler\CampusExamScheduler.exe` (with bundled Java runtime) using `jpackage`.
`package-exe.bat installer` makes an installer and needs the free WiX Toolset 3.x. Run `build.bat` first.

## 7. Project layout
```
pom.xml  build.bat  run.bat  package-exe.bat  README.md
docs/        DATABASE_SCHEMA.md  ARCHITECTURE.md  DEMO_GUIDE.md  VERIFICATION.md
src/main/resources/schema.sql
src/main/java/com/campus/examscheduler/{config,model,util,dao,scheduler,service,ui}  (+ Main.java)
src/test/java/com/campus/examscheduler/...
```
Presenting to a lecturer: follow `docs/DEMO_GUIDE.md`.

## 8. Known limitations
- Dates are typed as `yyyy-MM-dd` (Swing has no built-in date picker).
- Each examination uses ONE venue; an exam larger than every available venue is reported unscheduled (it is never split across venues).
- Break periods apply to every examination day; no per-date exclusions (holidays) - narrow the dates or permitted days instead.
- The search is bounded (30,000 steps / 20 s); for very constrained data it returns the best partial timetable and says so.
- The PDF writer produces simple tables with the standard Helvetica font (Latin characters).
- Only ND1 and ND2 levels are supported; a single administrator role.
- The Dribbble reference could not be opened while building, so the look follows its described style (navy sidebar, light cards, blue accents) rather than a pixel match.
- Items listed as unverified in `docs/VERIFICATION.md` were not executed in the build environment.
- # matric number
 1. F/ND/25/3210201  
2. F/ND/25/3210291  
3. F/ND/25/3210390  
4. F/ND/25/3210242  
5. F/ND/25/3210188  
6. F/ND/25/3210384  
7. F/ND/25/3210149  
8. F/ND/25/3210280  
9. F/ND/25/3210078  
10. F/ND/25/3210333  
11. F/ND/25/3210055  
12. F/ND/25/3210331  
13. F/ND/25/3210121  
14. F/ND/25/3210130  
15. F/ND/25/3210213  
16. F/ND/25/3210309  
17. F/ND/25/3210069  
18. F/ND/25/3210379  
19. F/ND/25/3210388  
20. F/ND/25/3210126  
21. F/ND/25/3210231  
22. F/ND/25/3210407  
23. F/ND/25/3210130  
24. F/ND/25/3210236  
25. F/ND/25/3210327  
26. F/ND/25/3210339  
27. F/ND/25/3210205  
28. F/ND/25/3210026  
29. F/ND/25/3210047  
30. F/ND/25/3210109  
31. F/ND/25/3210385  
32. F/ND/25/3210393  
33. F/ND/25/3210311  
34. F/ND/25/3210377  
35. F/ND/25/3210364  
36. F/ND/25/3210319  
37. F/ND/25/3210278  
38. F/ND/25/3210358  
39. F/ND/25/3210199  
40. F/ND/25/3210324  
41. F/ND/25/3210408  
42. F/ND/25/3210257  
43. F/ND/25/3210047  
44. F/ND/25/3210050  
45. F/ND/25/3210030  
46. F/ND/25/3210025  
47. F/ND/25/3210024  
48. F/ND/25/3210096  
49. F/ND/25/3210405  
50. F/ND/25/3210017  
51. F/ND/25/3210061  
52. F/ND/25/3210214  
53. F/ND/25/3210087  
54. F/ND/25/3210381  
55. F/ND/25/3210235  
56. F/ND/25/3210184  
57. F/ND/25/3210204  
58. F/ND/25/3210344  
59. F/ND/25/3210172  
60. F/ND/25/3210067  
61. F/ND/25/3210287  
62. F/ND/25/3210376  
63. F/ND/25/3210396  
64. F/ND/25/3210068
65. F/ND/25/3210116
