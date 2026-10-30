package com.campus.examscheduler.service;

import com.campus.examscheduler.dao.CourseDao;
import com.campus.examscheduler.dao.Db;
import com.campus.examscheduler.dao.DepartmentDao;
import com.campus.examscheduler.dao.ScheduleDao;
import com.campus.examscheduler.dao.SettingsDao;
import com.campus.examscheduler.dao.VenueDao;
import com.campus.examscheduler.model.Course;
import com.campus.examscheduler.model.Venue;
import com.campus.examscheduler.scheduler.ConstraintChecker;
import com.campus.examscheduler.scheduler.ExamEvent;
import com.campus.examscheduler.scheduler.ExamEventBuilder;
import com.campus.examscheduler.scheduler.Placement;
import com.campus.examscheduler.scheduler.ScheduleResult;
import com.campus.examscheduler.scheduler.ScheduleValidator;
import com.campus.examscheduler.scheduler.SchedulingRules;
import com.campus.examscheduler.scheduler.TimetableGenerator;
import com.campus.examscheduler.scheduler.Unscheduled;
import com.campus.examscheduler.scheduler.Violation;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** Connects the database to the scheduling engine: generate, save, view, manually change and validate timetables. */
public final class SchedulingService {
    private SchedulingService() { }

    /** The last automatic generation (kept in memory so the "Unscheduled" report can show the reasons). */
    private static volatile ScheduleResult lastGeneration;

    public static ScheduleResult getLastGeneration() { return lastGeneration; }
    public static void forgetLastGeneration() { lastGeneration = null; }

    /** Everything the engine needs, read from the database at one moment. */
    public static final class Context {
        public SchedulingRules rules;
        public List<Venue> venues;
        public List<Course> courses;
        public List<ExamEvent> events;
        public List<String> warnings = new ArrayList<>();
        public Map<String, ExamEvent> eventByKey = new LinkedHashMap<>();
        public Map<Long, Venue> venueById = new HashMap<>();
    }

    public static Context load() {
        return Db.read(c -> {
            Context ctx = new Context();
            ctx.rules = SettingsDao.load(c);
            if (ctx.rules == null) ctx.rules = SettingsService.load();
            ctx.venues = VenueDao.findAll(c);
            ctx.courses = CourseDao.findAll(c);
            ctx.events = ExamEventBuilder.build(ctx.courses, ctx.warnings);
            for (ExamEvent e : ctx.events) ctx.eventByKey.put(e.getKey(), e);
            for (Venue v : ctx.venues) ctx.venueById.put(v.getId(), v);
            return ctx;
        });
    }

    /** Saved timetable joined with current events and venues. Rows whose course/venue no longer exists are purged. */
    public static List<Placement> savedPlacements(Context ctx) {
        Map<String, ScheduleDao.Row> rows = Db.read(ScheduleDao::findAll);
        List<Placement> placements = new ArrayList<>();
        List<String> orphans = new ArrayList<>();
        for (ScheduleDao.Row r : rows.values()) {
            ExamEvent e = ctx.eventByKey.get(r.eventKey());
            Venue v = ctx.venueById.get(r.venueId());
            if (e == null || v == null) { orphans.add(r.eventKey()); continue; }
            placements.add(new Placement(e, r.date(), r.startMin(), v));
        }
        if (!orphans.isEmpty()) Db.inTransaction(c -> { for (String k : orphans) ScheduleDao.delete(c, k); return null; });
        return placements;
    }

    // ------------------------------------------------------------------ generation

    /** Runs the scheduling engine (does NOT save). Call from a background thread for large data sets. */
    public static ScheduleResult generate(int variant, Consumer<String> progress) {
        Context ctx = load();
        if (ctx.events.isEmpty()) throw new ValidationException("There are no courses to schedule. Add courses first.");
        if (ctx.venues.isEmpty()) throw new ValidationException("There are no examination venues. Add venues first.");
        TimetableGenerator.Options options = new TimetableGenerator.Options();
        options.variant = variant;
        if (progress != null) options.progress = progress;
        ScheduleResult result = new TimetableGenerator(ctx.rules).generate(ctx.events, ctx.venues, options);
        result.getWarnings().addAll(0, ctx.warnings);
        lastGeneration = result;
        return result;
    }

    /** Validates the proposed timetable again and replaces the saved timetable with it (one transaction). */
    public static void save(ScheduleResult result) {
        Context ctx = load();
        List<String> problems = new ArrayList<>();
        for (Placement p : result.getPlacements()) {
            ExamEvent current = ctx.eventByKey.get(p.event().getKey());
            if (current == null || current.getStudents() != p.event().getStudents() || current.getDurationMin() != p.event().getDurationMin()
                    || !current.getGroupIds().equals(p.event().getGroupIds()))
                problems.add("Course data changed after this timetable was generated (" + p.event().codes() + "). Generate it again.");
            Venue v = ctx.venueById.get(p.venue().getId());
            if (v == null) problems.add("Venue '" + p.venue().getName() + "' no longer exists. Generate the timetable again.");
        }
        if (!problems.isEmpty()) throw new ValidationException(problems);
        List<Violation> violations = ScheduleValidator.validate(result.getPlacements(), ctx.rules);
        if (!violations.isEmpty()) {
            List<String> messages = new ArrayList<>();
            for (Violation v : violations) messages.add(v.message());
            throw new ValidationException(messages);
        }
        Db.inTransaction(c -> {
            ScheduleDao.deleteAll(c);
            for (Placement p : result.getPlacements())
                ScheduleDao.upsert(c, p.event().getKey(), p.date(), p.startMin(), p.endMin(), p.venue().getId());
            return null;
        });
    }

    public static void clearSchedule() {
        Db.inTransaction(c -> { ScheduleDao.deleteAll(c); return null; });
    }

    // ------------------------------------------------------------------ manual editing

    /** Checks a proposed manual change with the SAME rules as the automatic generator. */
    public static List<Violation> checkMove(String eventKey, LocalDate date, int startMin, long venueId) {
        Context ctx = load();
        ExamEvent event = ctx.eventByKey.get(eventKey);
        if (event == null) throw new ValidationException("That examination no longer exists.");
        Venue venue = ctx.venueById.get(venueId);
        if (venue == null) throw new ValidationException("Select a venue.");
        List<Placement> others = savedPlacements(ctx);
        return new ConstraintChecker(ctx.rules).check(event, date, startMin, venue, others);
    }

    /** Saves a manual reschedule / venue change, but only if it breaks no rule. */
    public static void applyMove(String eventKey, LocalDate date, int startMin, long venueId) {
        List<Violation> violations = checkMove(eventKey, date, startMin, venueId);
        if (!violations.isEmpty()) {
            List<String> messages = new ArrayList<>();
            for (Violation v : violations) messages.add(v.message());
            throw new ValidationException(messages);
        }
        Context ctx = load();
        ExamEvent event = ctx.eventByKey.get(eventKey);
        Db.inTransaction(c -> {
            ScheduleDao.upsert(c, eventKey, date, startMin, startMin + event.getDurationMin(), venueId);
            return null;
        });
    }

    public static void unschedule(String eventKey) {
        Db.inTransaction(c -> { ScheduleDao.delete(c, eventKey); return null; });
    }

    // ------------------------------------------------------------------ views

    public static List<TimetableRow> timetable() {
        Context ctx = load();
        return timetable(ctx, savedPlacements(ctx));
    }

    private static List<TimetableRow> timetable(Context ctx, List<Placement> placements) {
        Map<String, Placement> byKey = new HashMap<>();
        for (Placement p : placements) byKey.put(p.event().getKey(), p);
        ScheduleResult last = lastGeneration;
        List<TimetableRow> rows = new ArrayList<>();
        for (ExamEvent e : ctx.events) {
            Placement p = byKey.get(e.getKey());
            if (p != null) {
                rows.add(new TimetableRow(e.getKey(), p.date(), p.date().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                        ConstraintChecker.time(p.startMin()), ConstraintChecker.time(p.endMin()), e.codes(), e.titlesText(),
                        e.departmentsText(), e.levelsText(), e.getStudents(), e.getDurationMin(), p.venue().getId(),
                        p.venue().getName(), true, e.isJoint() ? "Joint examination" : ""));
            } else {
                rows.add(new TimetableRow(e.getKey(), null, "", "", "", e.codes(), e.titlesText(), e.departmentsText(),
                        e.levelsText(), e.getStudents(), e.getDurationMin(), 0, "", false, reasonFor(e, ctx, last)));
            }
        }
        rows.sort(Comparator.comparing((TimetableRow r) -> !r.scheduled())
                .thenComparing(r -> r.date() == null ? LocalDate.MAX : r.date())
                .thenComparing(TimetableRow::start).thenComparing(TimetableRow::venue).thenComparing(TimetableRow::codes));
        return rows;
    }

    private static String reasonFor(ExamEvent e, Context ctx, ScheduleResult last) {
        if (last != null) for (Unscheduled u : last.getUnscheduled()) if (u.event().getKey().equals(e.getKey())) return u.reason();
        String pre = TimetableGenerator.precheck(e, ctx.venues, ctx.rules);
        return pre != null ? pre : "Not scheduled yet. Generate the timetable or place it manually.";
    }

    /** Re-validates the saved timetable against the CURRENT settings, venues and courses. */
    public static List<Violation> validateSaved() {
        Context ctx = load();
        return ScheduleValidator.validate(savedPlacements(ctx), ctx.rules);
    }

    public static DashboardStats stats() {
        Context ctx = load();
        List<Placement> placed = savedPlacements(ctx);
        List<TimetableRow> rows = timetable(ctx, placed);
        int departments = Db.read(DepartmentDao::findAll).size();
        int scheduled = (int) rows.stream().filter(TimetableRow::scheduled).count();
        int available = (int) ctx.venues.stream().filter(Venue::isAvailable).count();
        int conflicts = ScheduleValidator.validate(placed, ctx.rules).size();
        LocalDate today = LocalDate.now();
        List<TimetableRow> future = new ArrayList<>();
        List<TimetableRow> all = new ArrayList<>();
        for (TimetableRow r : rows) {
            if (!r.scheduled()) continue;
            all.add(r);
            if (!r.date().isBefore(today)) future.add(r);
        }
        boolean useFuture = !future.isEmpty();
        List<TimetableRow> source = useFuture ? future : all;
        List<TimetableRow> upcoming = new ArrayList<>(source.subList(0, Math.min(8, source.size())));
        return new DashboardStats(departments, ctx.courses.size(), ctx.venues.size(), available, rows.size(), scheduled,
                rows.size() - scheduled, ctx.rules.examDates().size(), conflicts, upcoming, useFuture);
    }
}
