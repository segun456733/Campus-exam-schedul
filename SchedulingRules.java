package com.campus.examscheduler.scheduler;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** All configurable examination-period restrictions. Pure data + validation (no database). */
public class SchedulingRules {
    private LocalDate startDate;
    private LocalDate endDate;
    private Set<DayOfWeek> allowedDays = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
    private int dayStartMin = 8 * 60;
    private int dayEndMin = 17 * 60;
    private int slotStepMin = 30;
    private int gapMin = 0;
    private int maxSimultaneous = 4;
    private boolean noCrossDepartment = false;
    private boolean noCrossLevel = false;
    private int maxExamsPerGroupPerDay = 2;
    private List<TimeRange> breaks = new ArrayList<>();

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate d) { this.startDate = d; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate d) { this.endDate = d; }
    public Set<DayOfWeek> getAllowedDays() { return allowedDays; }
    public void setAllowedDays(Set<DayOfWeek> days) { this.allowedDays = days; }
    public int getDayStartMin() { return dayStartMin; }
    public void setDayStartMin(int m) { this.dayStartMin = m; }
    public int getDayEndMin() { return dayEndMin; }
    public void setDayEndMin(int m) { this.dayEndMin = m; }
    public int getSlotStepMin() { return slotStepMin; }
    public void setSlotStepMin(int m) { this.slotStepMin = m; }
    public int getGapMin() { return gapMin; }
    public void setGapMin(int m) { this.gapMin = m; }
    public int getMaxSimultaneous() { return maxSimultaneous; }
    public void setMaxSimultaneous(int m) { this.maxSimultaneous = m; }
    public boolean isNoCrossDepartment() { return noCrossDepartment; }
    public void setNoCrossDepartment(boolean b) { this.noCrossDepartment = b; }
    public boolean isNoCrossLevel() { return noCrossLevel; }
    public void setNoCrossLevel(boolean b) { this.noCrossLevel = b; }
    public int getMaxExamsPerGroupPerDay() { return maxExamsPerGroupPerDay; }
    public void setMaxExamsPerGroupPerDay(int m) { this.maxExamsPerGroupPerDay = m; }
    public List<TimeRange> getBreaks() { return breaks; }
    public void setBreaks(List<TimeRange> breaks) { this.breaks = breaks; }

    /** Every permitted examination date (inside the range and on an allowed weekday), in order. */
    public List<LocalDate> examDates() {
        List<LocalDate> dates = new ArrayList<>();
        if (startDate == null || endDate == null) return dates;
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            if (allowedDays.contains(d.getDayOfWeek())) dates.add(d);
        }
        return dates;
    }

    /** Validates the period before timetable generation. Returns an empty list when everything is fine. */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (startDate == null) errors.add("Examination start date is missing.");
        if (endDate == null) errors.add("Examination end date is missing.");
        if (startDate != null && endDate != null && endDate.isBefore(startDate))
            errors.add("End date must not be before the start date.");
        if (allowedDays == null || allowedDays.isEmpty()) errors.add("Select at least one permitted examination day.");
        if (startDate != null && endDate != null && !endDate.isBefore(startDate)
                && allowedDays != null && !allowedDays.isEmpty() && examDates().isEmpty())
            errors.add("No permitted weekday falls inside the chosen date range.");
        if (dayStartMin < 0 || dayEndMin > 24 * 60 || dayStartMin >= dayEndMin)
            errors.add("Daily closing time must be later than the daily starting time.");
        if (slotStepMin < 5 || slotStepMin > 120) errors.add("Slot step must be between 5 and 120 minutes.");
        if (gapMin < 0 || gapMin > 240) errors.add("Gap between sessions must be between 0 and 240 minutes.");
        if (maxSimultaneous < 1) errors.add("Maximum simultaneous examinations must be at least 1.");
        if (maxExamsPerGroupPerDay < 1) errors.add("Maximum exams per student group per day must be at least 1.");
        for (TimeRange b : breaks) {
            if (b.start() >= b.end()) errors.add("Break '" + b.label() + "' must end after it starts.");
            else if (b.start() < dayStartMin || b.end() > dayEndMin)
                errors.add("Break '" + b.label() + "' lies outside the daily operating hours.");
        }
        return errors;
    }
}
