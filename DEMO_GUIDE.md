# Demonstration guide (about 10 minutes)

Login: `admin` / `Admin@123` (change it under Administrator Settings).

1. **Login** - show the error for a wrong password, then sign in. Mention the password is stored as a salted PBKDF2 hash.
2. **Dashboard** - 10 departments, 61 courses, venues, examination days, 0 scheduled. All numbers come from the database.
3. **Departments** - add a department (e.g. `GEO` / Geology), edit its description, then delete it. Try adding `CSC` again: duplicate rejected.
4. **Courses** - search `CSC`, filter ND2, open `MTH112` (shared by 4 cohorts) and `CSC111` (joint examination with PHY114 and SLT114).
   Try saving a course with 0 students or a duplicate code: rejected with a clear message.
5. **Venues** - show capacities; **Old Annex Hall** is disabled (never used). Add a venue, disable it again.
6. **Examination Settings** - dates 16-27 Nov 2026, Mon-Fri, 08:00-17:00, lunch break 12:00-13:00, max 4 simultaneous.
   Press *Check settings*. Optionally enter an end date before the start date to show validation.
7. **Generate Timetable** - *Generate timetable* (progress bar, SwingWorker), review the preview, warnings tab shows no errors,
   *Save timetable*. Show *Regenerate* (a different valid timetable) before saving if desired.
8. **Timetable** - filter by department / level / venue / date. Double-click `CSC113` and try to move it to the same time as `CSC112`
   (same cohort): **rejected with an explanation**. Move it to a free slot: accepted.
9. **Show the unscheduled report** - add a course with 2000 students, generate again: it is reported as unscheduled with the reason
   (exceeds every venue). *Reports > Unscheduled examinations*.
10. **Reports** - show a department timetable, **Export CSV** (open in Excel), **Export PDF**, **Print** (choose "Microsoft Print to PDF" if no printer).
11. **Persistence** - close the window, run `run.bat` again: the data and timetable are still there.
12. **Administrator Settings** - change password; *Reload demonstration data* restores the samples (asks for confirmation).
