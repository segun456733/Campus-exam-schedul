# QA checklist (fill in on your Windows laptop)

Automatic checks (`build.bat`) already cover items marked **auto**. Tick the manual ones while following `docs/DEMO_GUIDE.md`.

| # | Area | How | Automatic check | Result (PASS/FAIL) |
|---|---|---|---|---|
| 1 | Install / start | `build.bat`, `run.bat` | `--version`, `--selftest`, `MainClassPackagingTest` | |
| 2 | Login / wrong password / logout / password change | Login screen, Administrator Settings | `--selftest`, `DatabaseIntegrationTest` | |
| 3 | Dashboard numbers and navigation | open every sidebar item | `DatabaseIntegrationTest` (stats) | |
| 4 | Departments add / edit / delete | Departments screen | `--selftest` | |
| 5 | Courses add / edit / delete / validation | Courses screen + dialog | `ValidationRulesTest`, `--selftest` | |
| 6 | Student groups and joint exams | course MTH112 (shared), CSC111 (joint) | `ConstraintCheckerTest`, `DemoDataSchedulingTest` | |
| 7 | Venues add / edit / capacity / availability | Venues screen | `--selftest` | |
| 8 | Examination settings and dates | Examination Settings, use *Check settings* | `ConstraintCheckerTest`, `TimetableGeneratorTest` | |
| 9 | Automatic generation | Generate Timetable | `TimetableGeneratorTest`, `DemoDataSchedulingTest`, `--selftest` | |
| 10 | Student-group clash prevention | Timetable > move CSC113 onto CSC112 | `ConstraintCheckerTest`, `--selftest` | |
| 11 | Venue clash prevention | move an exam into an occupied venue/time | `ConstraintCheckerTest` | |
| 12 | Manual rescheduling | Timetable > Reschedule | `DatabaseIntegrationTest`, `--selftest` | |
| 13 | Unscheduled reporting | add a 2000-student course, generate | `TimetableGeneratorTest`, `--selftest` | |
| 14 | CSV / PDF export, printing | Reports | `SecurityAndExportTest`, `--selftest` (printing: manual only) | |
| 15 | Persistence after restart | close and reopen the app | `DatabaseIntegrationTest`, `--selftest` | |
| 16 | Demo data load / reset | Administrator Settings | `--selftest` | |
| 17 | Error handling / invalid input | submit empty or wrong values in any form | `ValidationRulesTest` | |
| 18 | All buttons, links, dialogs, resizing | click every button; resize the window to 1100x680 | manual only | |
