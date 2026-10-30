package com.campus.examscheduler.service;

import java.util.List;

/** Thrown when user input or a timetable change breaks one or more rules. */
public class ValidationException extends AppException {
    private final List<String> problems;

    public ValidationException(List<String> problems) {
        super(String.join("\n", problems));
        this.problems = problems;
    }
    public ValidationException(String problem) { this(List.of(problem)); }
    public List<String> getProblems() { return problems; }
}
