package com.campus.examscheduler.service;

import java.util.List;

/** A report ready to display, export or print. */
public record ReportTable(String title, String subtitle, String[] columns, List<String[]> rows) { }
