# What was verified, and what was not

The project was written in a sandbox with **JDK 21, no Maven, no internet and no display**. Be aware of exactly what that means.

## Actually executed and passed (in the sandbox)
| Check | How |
|---|---|
| All main + test sources compile with no errors | `javac` (JDK 21, `--release` semantics of JDK 17 code) |
| 34 automated tests pass: scheduler rules 1-11, 13, 14, 16, generator behaviour, demo data schedules completely (57 events, all constraints), field validation, password hashing, CSV and PDF output | Run with a minimal local stand-in for JUnit 5 (annotations `@Test`/`@BeforeEach` and the assertions used). **They have not yet been run by real JUnit/Maven - `build.bat` does that on your laptop.** |
| `schema.sql` is valid SQLite and all 50 SQL statements in the Java code prepare correctly against it | Python `sqlite3` (SQLite 3.45) with `EXPLAIN` |
| The PDF writer creates a valid multi-page A4 landscape PDF | opened with `pdftotext` and `pypdf` |
| All 9 screens construct and paint without exceptions | headless Swing smoke test (no real window) |

## NOT verified - please check on your laptop (marked unverified)
| Item | Why | What to do |
|---|---|---|
| Maven build, `pom.xml`, shaded runnable JAR, dependency download | no Maven / internet here | run `build.bat` |
| SQLite JDBC at runtime (all DAO code against a real driver) | driver jar unavailable | `DatabaseIntegrationTest` (9 tests) runs during `build.bat`; they cover requirement 15 (persistence after restart), duplicates, manual-move rejection |
| Interactive behaviour of the Swing UI (clicks, dialogs, login flow, SwingWorker progress) | no display | follow `docs/DEMO_GUIDE.md` once |
| Printing | no printer / display | Reports > Print (use *Microsoft Print to PDF*) |
| `jpackage` EXE | not on Windows | optional: `package-exe.bat` |
| Visual match to the Dribbble reference | the link could not be opened | design follows the brief: navy sidebar, light workspace, blue accents |

If `build.bat` reports a failure, the error text tells exactly which test or file; most likely causes are a typo-level compile error
in a UI class or a failing integration test - send the message and it can be fixed quickly.
