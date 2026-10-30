# What was verified, and what was not (honest status)

The project was built in a sandbox with **JDK 21, no Maven, no internet, no SQLite JDBC jar, no real JUnit jar and a headless JDK (no windows)**.
Because of that, **`mvn clean verify` was never run by the author of this package.** `build.bat` does it on your laptop.

## Actually executed here and passed
| Check | How |
|---|---|
| All main + test sources compile (JDK 21 compiler); every `package` line matches its folder (0 mismatches) | `javac` + script |
| 36 tests pass: scheduler rules, generator behaviour, demo data schedules completely (57 events), field validation, password hashing, CSV, PDF, main-class packaging | executed with a **minimal local stand-in for JUnit 5** (not the real engine) |
| `schema.sql` is valid SQLite and all 50 SQL statements prepare against it | Python `sqlite3` (SQLite 3.45) `EXPLAIN` |
| PDF writer output is a valid multi-page PDF | `pdftotext`, `pypdf` |
| All 9 screens construct and paint without exceptions | headless Swing smoke test (no real window); two layout bugs found and fixed |
| A hand-built JAR with `Main-Class: com.campus.examscheduler.Main` starts: `--version` works, `--selftest` correctly reports the missing SQLite driver | `java -jar` |

## NOT verified here (needs your laptop)
| Item | Where it is checked for you |
|---|---|
| Maven build, shade plugin, dependency download, **real JUnit 5 run** | `build.bat` (`mvn clean package` runs surefire) |
| Everything touching SQLite JDBC (all DAO code at runtime) | `DatabaseIntegrationTest` (9 tests) and `--selftest` (13 checks), both run by `build.bat` |
| That the shaded JAR contains `Main.class` and the SQLite driver | `build.bat` runs `java -jar ... --version` and `--selftest` and stops on failure |
| Interactive UI: clicks, dialogs, login flow, SwingWorker progress, resizing, printing | `docs/QA_CHECKLIST.md` (manual) |
| `jpackage` EXE | `package-exe.bat` (Windows only) |
| The Dribbble reference design | the link could not be opened; look follows the brief |

## About the "Could not find or load main class" error
A BUILD SUCCESS followed by `ClassNotFoundException: com.campus.examscheduler.Main` means the finished JAR has no `Main.class`, i.e. `Main.java`
was not part of the compiled sources (missing from `src\main\java\com\campus\examscheduler\`). Maven cannot notice this because no other class
refers to `Main`. The cause in your earlier copy could not be inspected (no project ZIP reached the sandbox; only individual files, and `Main.java`
was not among them). Protections now in place: test `MainClassPackagingTest` (fails the build), `build.bat` runs the JAR before declaring success,
and `run.bat` keeps errors visible. The ZIP delivered with this report contains `Main.java` (verified).
