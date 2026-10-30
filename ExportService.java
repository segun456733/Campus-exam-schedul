package com.campus.examscheduler.service;

import com.campus.examscheduler.config.AppConfig;
import com.campus.examscheduler.util.CsvUtil;
import com.campus.examscheduler.util.SimplePdfWriter;

import java.io.IOException;
import java.nio.file.Path;

/** CSV and PDF export of any report. */
public final class ExportService {
    private ExportService() { }

    public static void exportCsv(ReportTable report, Path file) {
        try {
            CsvUtil.write(file, report.columns(), report.rows());
        } catch (IOException e) {
            throw new AppException("Could not write the CSV file: " + e.getMessage(), e);
        }
    }

    public static void exportPdf(ReportTable report, Path file) {
        try {
            SimplePdfWriter.writeTable(file, AppConfig.APP_NAME + " - " + report.title(), report.subtitle(), report.columns(), report.rows());
        } catch (IOException e) {
            throw new AppException("Could not write the PDF file: " + e.getMessage(), e);
        }
    }
}
