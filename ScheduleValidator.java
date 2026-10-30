package com.campus.examscheduler.scheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Validates a complete timetable (used before saving, on the dashboard and in the conflict report). */
public final class ScheduleValidator {
    private ScheduleValidator() { }

    public static List<Violation> validate(List<Placement> placements, SchedulingRules rules) {
        ConstraintChecker checker = new ConstraintChecker(rules);
        Set<String> seenMessages = new LinkedHashSet<>();
        List<Violation> result = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (Placement p : placements) {
            if (!keys.add(p.event().getKey())) {
                add(result, seenMessages, new Violation(Violation.Type.DUPLICATE_EVENT,
                        "Examination " + p.event().codes() + " appears more than once in the timetable."));
            }
            List<Placement> others = new ArrayList<>(placements);
            others.remove(p);
            for (Violation v : checker.check(p.event(), p.date(), p.startMin(), p.venue(), others)) {
                add(result, seenMessages, v);
            }
        }
        return result;
    }

    private static void add(List<Violation> list, Set<String> seen, Violation v) {
        if (seen.add(v.message())) list.add(v);
    }
}
