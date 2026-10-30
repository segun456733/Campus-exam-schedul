package com.campus.examscheduler.service;

import com.campus.examscheduler.config.AppConfig;
import com.campus.examscheduler.dao.AdminDao;
import com.campus.examscheduler.dao.Db;
import com.campus.examscheduler.dao.DemoData;
import com.campus.examscheduler.util.PasswordUtil;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** Start-up: opens the database, creates the schema and seeds demonstration data on FIRST launch only. */
public final class AppInit {
    private AppInit() { }

    public static void start() { start(AppConfig.dbFile()); }

    public static void start(Path dbFile) {
        Db.configure(dbFile);
        Db.createSchema();
        Db.inTransaction(c -> {
            if (AdminDao.count(c) == 0) {
                String salt = PasswordUtil.newSalt();
                AdminDao.insert(c, AppConfig.DEFAULT_ADMIN_USERNAME,
                        PasswordUtil.hash(AppConfig.DEFAULT_ADMIN_PASSWORD, salt, PasswordUtil.ITERATIONS), salt, PasswordUtil.ITERATIONS);
            }
            boolean seeded;
            try (PreparedStatement ps = c.prepareStatement("SELECT value FROM app_meta WHERE key = 'demo_seeded'");
                 ResultSet rs = ps.executeQuery()) {
                seeded = rs.next();
            }
            if (!seeded) {
                DemoData.seed(c);
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO app_meta (key, value) VALUES ('demo_seeded', '1')")) {
                    ps.executeUpdate();
                }
            }
            return null;
        });
    }

    /** Deletes ALL institution data and reloads the demonstration data. Administrator accounts are kept. */
    public static void resetDemoData() {
        Db.inTransaction(c -> {
            DemoData.wipe(c);
            DemoData.seed(c);
            return null;
        });
        SchedulingService.forgetLastGeneration();
    }

    /** Deletes ALL institution data and leaves the database empty (for a real institution to enter its own data). */
    public static void clearAllData() {
        Db.inTransaction(c -> { DemoData.wipe(c); return null; });
        SchedulingService.forgetLastGeneration();
    }
}
