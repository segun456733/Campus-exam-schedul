package com.campus.examscheduler.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Application-wide configuration: names and the writable data folder that holds the SQLite database. */
public final class AppConfig {
    public static final String APP_NAME = "Campus Examination Scheduler";
    public static final String APP_VERSION = "1.0.0";
    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";

    private AppConfig() { }

    /**
     * Writable data directory: %APPDATA%\CampusExamScheduler on Windows (never the install folder).
     * Can be overridden with -Dexamscheduler.home=PATH (used by the automated tests).
     */
    public static Path dataDir() {
        try {
            Path dir;
            String override = System.getProperty("examscheduler.home");
            String appData = System.getenv("APPDATA");
            if (override != null && !override.isBlank()) dir = Paths.get(override);
            else if (appData != null && !appData.isBlank()) dir = Paths.get(appData, "CampusExamScheduler");
            else dir = Paths.get(System.getProperty("user.home"), ".campus-exam-scheduler");
            Files.createDirectories(dir);
            return dir;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot create the application data folder: " + e.getMessage(), e);
        }
    }

    public static Path dbFile() { return dataDir().resolve("exam_scheduler.db"); }
}
