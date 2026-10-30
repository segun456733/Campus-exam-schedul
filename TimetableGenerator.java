package com.campus.examscheduler.scheduler;

import com.campus.examscheduler.model.Venue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Constraint-based timetable generator.
 *
 * Strategy:
 *  1. Pre-check every examination on its own (venue capacity, duration vs. daily window).
 *     Impossible ones are reported as unscheduled WITH a reason, never silently dropped.
 *  2. Backtracking search with the "most constrained first" (MRV) heuristic and forward
 *     checking: at every step the examination with the fewest remaining (slot, venue)
 *     options is placed next; if any examination has zero options the branch is abandoned.
 *  3. Candidate options are tried best-first: spread a student group's exams over different
 *     days, then earliest date/time, then smallest suitable venue.
 *  4. If no complete timetable exists (or the search limit is hit) a greedy pass places as many
 *     examinations as possible and explains every examination it could not place.
 * Every rule check goes through {@link ConstraintChecker}, the same code used for manual edits.
 */
public class TimetableGenerator {

    /** Tuning options. */
    public static class Options {
        public int variant = 0;               // 0 = best timetable; >0 = alternative timetable (regenerate)
        public long maxNodes = 30_000;
        public long timeLimitMs = 20_000;
        public Consumer<String> progress = s -> { };
    }

    private record Slot(LocalDate date, int dateIndex, int start) { }
    private record Option(Slot slot, Venue venue, long cost) { }

    private final SchedulingRules rules;
    private final ConstraintChecker checker;
    private final Map<LocalDate, List<Placement>> byDate = new HashMap<>();
    private final Map<Integer, List<Slot>> slotCache = new HashMap<>();
    private final Map<String, List<Venue>> eligible = new HashMap<>();
    private Options opts = new Options();
    private Random rnd;
    private long nodes;
    private long deadline;
    private boolean aborted;

    public TimetableGenerator(SchedulingRules rules) {
        this.rules = rules;
        this.checker = new ConstraintChecker(rules);
    }

    // ------------------------------------------------------------------ public API

    /**
     * Checks one examination on its own. Returns a reason if it can never be scheduled, otherwise null.
     */
    public static String precheck(ExamEvent e, List<Venue> venues, SchedulingRules rules) {
        List<String> settingErrors = rules.validate();
        if (!settingErrors.isEmpty()) return "Examination settings are invalid: " + settingErrors.get(0);
        int maxCap = 0;
        boolean anyAvailable = false;
        for (Venue v : venues) {
            if (v.isAvailable()) { anyAvailable = true; maxCap = Math.max(maxCap, v.getCapacity()); }
        }
        if (!anyAvailable) return "No examination venue is available (all venues are disabled or none exist).";
        if (e.getStudents() > maxCap)
            return "Student count (" + e.getStudents() + ") exceeds the capacity of every available venue (largest available venue seats "
                    + maxCap + ").";
        if (buildSlots(rules, new ConstraintChecker(rules), e.getDurationMin()).isEmpty())
            return "Duration of " + e.getDurationMin() + " minutes does not fit into any permitted day/time window "
                    + "(check examination dates, permitted days, daily hours and breaks).";
        return null;
    }

    public ScheduleResult generate(List<ExamEvent> events, List<Venue> venues, Options options) {
        long t0 = System.currentTimeMillis();
        this.opts = options == null ? new Options() : options;
        this.rnd = new Random(1000L + this.opts.variant);
        this.deadline = System.nanoTime() + this.opts.timeLimitMs * 1_000_000L;
        ScheduleResult result = new ScheduleResult();

        List<String> settingErrors = rules.validate();
        if (!settingErrors.isEmpty()) {
            result.getErrors().addAll(settingErrors);
            for (ExamEvent e : events) result.getUnscheduled().add(new Unscheduled(e, "Examination settings are invalid: " + settingErrors.get(0)));
            result.setElapsedMs(System.currentTimeMillis() - t0);
            return result;
        }

        // Step 1: individual pre-checks.
        List<ExamEvent> schedulable = new ArrayList<>();
        for (ExamEvent e : events) {
            String reason = precheck(e, venues, rules);
            if (reason != null) {
                result.getUnscheduled().add(new Unscheduled(e, reason));
            } else {
                schedulable.add(e);
                List<Venue> suitable = new ArrayList<>();
                for (Venue v : venues) if (v.isAvailable() && v.getCapacity() >= e.getStudents()) suitable.add(v);
                suitable.sort(Comparator.comparingInt(Venue::getCapacity).thenComparing(Venue::getName));
                eligible.put(e.getKey(), suitable);
            }
        }

        // Step 2: backtracking search.
        opts.progress.accept("Searching for a conflict-free timetable for " + schedulable.size() + " examinations...");
        List<ExamEvent> remaining = new ArrayList<>(schedulable);
        boolean solved = search(remaining);
        if (!solved) {
            // Step 3: greedy fallback that places as many examinations as possible.
            result.setUsedFallback(true);
            byDate.clear();
            result.getWarnings().add(aborted
                    ? "The search limit was reached before a complete timetable was found. A partial timetable is shown."
                    : "No complete timetable exists under the current restrictions. A partial timetable is shown.");
            opts.progress.accept("Placing as many examinations as possible...");
            greedy(schedulable, result);
        }

        for (List<Placement> day : byDate.values()) result.getPlacements().addAll(day);
        result.getPlacements().sort(Comparator.comparing(Placement::date).thenComparingInt(Placement::startMin)
                .thenComparing(p -> p.venue().getName()));

        // Rule 12: validate the final timetable before returning it.
        for (Violation v : ScheduleValidator.validate(result.getPlacements(), rules)) result.getErrors().add(v.message());
        result.setNodesExplored(nodes);
        result.setElapsedMs(System.currentTimeMillis() - t0);
        return result;
    }

    // ------------------------------------------------------------------ search

    private boolean search(List<ExamEvent> remaining) {
        if (remaining.isEmpty()) return true;
        if (++nodes > opts.maxNodes || System.nanoTime() > deadline) { aborted = true; return false; }

        // Most constrained examination first (fewest options), ties: more students, then longer.
        ExamEvent best = null;
        int bestCount = Integer.MAX_VALUE;
        for (ExamEvent e : remaining) {
            int c = options(e, 40, false, null).size();
            if (c == 0) return false;                                  // forward check: dead end
            if (c < bestCount || (c == bestCount && moreDifficult(e, best))) { best = e; bestCount = c; }
        }
        List<Option> candidates = options(best, Integer.MAX_VALUE, true, null);
        remaining.remove(best);
        int tried = 0;
        for (Option o : candidates) {
            place(best, o);
            if (search(remaining)) return true;
            unplace(best, o);
            if (aborted) break;
            if (++tried >= 12) break;                                  // bounded branching keeps the search fast
        }
        remaining.add(best);
        return false;
    }

    private static boolean moreDifficult(ExamEvent a, ExamEvent b) {
        if (b == null) return true;
        if (a.getStudents() != b.getStudents()) return a.getStudents() > b.getStudents();
        return a.getDurationMin() > b.getDurationMin();
    }

    private void greedy(List<ExamEvent> all, ScheduleResult result) {
        List<ExamEvent> remaining = new ArrayList<>(all);
        while (!remaining.isEmpty()) {
            ExamEvent best = null;
            int bestCount = Integer.MAX_VALUE;
            List<ExamEvent> dead = new ArrayList<>();
            for (ExamEvent e : remaining) {
                int c = options(e, 40, false, null).size();
                if (c == 0) dead.add(e);
                else if (c < bestCount || (c == bestCount && moreDifficult(e, best))) { best = e; bestCount = c; }
            }
            for (ExamEvent e : dead) {
                result.getUnscheduled().add(new Unscheduled(e, diagnose(e)));
                remaining.remove(e);
            }
            if (best == null) break;
            remaining.remove(best);
            place(best, options(best, Integer.MAX_VALUE, true, null).get(0));
        }
    }

    // ------------------------------------------------------------------ options & diagnostics

    private List<Option> options(ExamEvent e, int cap, boolean sorted, Map<Violation.Type, Integer> diag) {
        List<Option> out = new ArrayList<>();
        List<Venue> venues = eligible.get(e.getKey());
        int dur = e.getDurationMin();
        for (Slot s : slots(dur)) {
            List<Placement> day = byDate.getOrDefault(s.date(), List.of());
            Violation sv = checker.studentViolation(e, s.date(), s.start(), dur, day);
            if (sv != null) { count(diag, sv.type()); continue; }
            long load = sorted ? groupLoad(e, day) : 0;
            for (int vi = 0; vi < venues.size(); vi++) {
                Venue v = venues.get(vi);
                if (checker.venueClash(v, s.date(), s.start(), dur, day) != null) { count(diag, Violation.Type.VENUE_CLASH); continue; }
                long cost = load * 1_000_000_000L + s.dateIndex() * 100_000L + s.start() * 10L + vi;
                if (sorted && opts.variant > 0) cost += rnd.nextInt(6) * 100_000L;   // alternative timetables on regenerate
                out.add(new Option(s, v, cost));
                if (!sorted && out.size() >= cap) return out;
            }
        }
        if (sorted) out.sort(Comparator.comparingLong(Option::cost));
        return out;
    }

    private long groupLoad(ExamEvent e, List<Placement> day) {
        int max = 0;
        for (long g : e.getGroupIds()) {
            int c = 0;
            for (Placement q : day) if (q.event().getGroupIds().contains(g)) c++;
            max = Math.max(max, c);
        }
        return max;
    }

    private String diagnose(ExamEvent e) {
        Map<Violation.Type, Integer> diag = new EnumMap<>(Violation.Type.class);
        options(e, 1, false, diag);
        int totalSlots = slots(e.getDurationMin()).size();
        List<Map.Entry<Violation.Type, Integer>> entries = new ArrayList<>(diag.entrySet());
        entries.sort((a, b) -> b.getValue() - a.getValue());
        StringBuilder sb = new StringBuilder("No valid slot remains (" + totalSlots + " possible time slots checked). Blocked mainly by ");
        for (int i = 0; i < Math.min(2, entries.size()); i++) {
            if (i > 0) sb.append(" and ");
            sb.append(phrase(entries.get(i).getKey())).append(" (").append(entries.get(i).getValue()).append("x)");
        }
        return sb.append('.').toString();
    }

    private static String phrase(Violation.Type t) {
        return switch (t) {
            case GROUP_CLASH -> "clashes with exams of the same student group";
            case VENUE_CLASH -> "no free venue of sufficient capacity";
            case MAX_SIMULTANEOUS -> "the simultaneous-examination limit";
            case POLICY_DEPARTMENT -> "the no-simultaneous-exams-across-departments policy";
            case POLICY_LEVEL -> "the no-simultaneous-exams-across-levels policy";
            case DAILY_LIMIT -> "the daily exam limit per student group";
            default -> t.name().toLowerCase();
        };
    }

    private static void count(Map<Violation.Type, Integer> diag, Violation.Type t) {
        if (diag != null) diag.merge(t, 1, Integer::sum);
    }

    // ------------------------------------------------------------------ state & slots

    private void place(ExamEvent e, Option o) {
        byDate.computeIfAbsent(o.slot().date(), d -> new ArrayList<>())
                .add(new Placement(e, o.slot().date(), o.slot().start(), o.venue()));
    }

    private void unplace(ExamEvent e, Option o) {
        List<Placement> day = byDate.get(o.slot().date());
        day.removeIf(p -> p.event().getKey().equals(e.getKey()));
    }

    private List<Slot> slots(int duration) {
        return slotCache.computeIfAbsent(duration, d -> buildSlots(rules, checker, d));
    }

    /** Every (date, start time) where an examination of this duration obeys the static rules. */
    private static List<Slot> buildSlots(SchedulingRules rules, ConstraintChecker checker, int duration) {
        List<Slot> slots = new ArrayList<>();
        List<LocalDate> dates = rules.examDates();
        for (int di = 0; di < dates.size(); di++) {
            for (int s = rules.getDayStartMin(); s + duration <= rules.getDayEndMin(); s += rules.getSlotStepMin()) {
                if (checker.slotViolation(dates.get(di), s, duration) == null) slots.add(new Slot(dates.get(di), di, s));
            }
        }
        return slots;
    }
}
