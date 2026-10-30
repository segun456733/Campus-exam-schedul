package com.campus.examscheduler.scheduler;

import java.util.ArrayList;
import java.util.List;

/** Outcome of an automatic generation run. */
public class ScheduleResult {
    private final List<Placement> placements = new ArrayList<>();
    private final List<Unscheduled> unscheduled = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private long nodesExplored;
    private long elapsedMs;
    private boolean usedFallback;

    public List<Placement> getPlacements() { return placements; }
    public List<Unscheduled> getUnscheduled() { return unscheduled; }
    public List<String> getWarnings() { return warnings; }
    public List<String> getErrors() { return errors; }
    public long getNodesExplored() { return nodesExplored; }
    public void setNodesExplored(long n) { nodesExplored = n; }
    public long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(long ms) { elapsedMs = ms; }
    public boolean isUsedFallback() { return usedFallback; }
    public void setUsedFallback(boolean b) { usedFallback = b; }

    /** True only when every examination was placed and no error was found. */
    public boolean isComplete() { return unscheduled.isEmpty() && errors.isEmpty(); }
}
