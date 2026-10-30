package com.campus.examscheduler.dao;

import com.campus.examscheduler.service.AppException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** SQLite connection handling, schema creation and transaction helpers. */
public final class Db {
    private static String url;

    private Db() { }

    /** Points the application at a database file (created on first use). */
    public static void configure(Path dbFile) {
        url = "jdbc:sqlite:" + dbFile.toAbsolutePath();
    }

    /** A unit of database work. */
    public interface Work<T> { T run(Connection c) throws SQLException; }

    public static Connection open() throws SQLException {
        if (url == null) throw new IllegalStateException("Database has not been configured.");
        Connection c = DriverManager.getConnection(url);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");      // enforce foreign keys on EVERY connection
            st.execute("PRAGMA busy_timeout = 5000");
        }
        return c;
    }

    /** Runs read-only work on a fresh connection. */
    public static <T> T read(Work<T> work) {
        try (Connection c = open()) {
            return work.run(c);
        } catch (SQLException e) {
            throw new AppException(friendly(e), e);
        }
    }

    /** Runs related changes in ONE transaction: all succeed or none are saved. */
    public static <T> T inTransaction(Work<T> work) {
        try (Connection c = open()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new AppException(friendly(e), e);
        }
    }

    /** Creates all tables if they do not exist (safe to run on every launch). */
    public static void createSchema() {
        String script;
        try (InputStream in = Db.class.getResourceAsStream("/schema.sql")) {
            if (in == null) throw new AppException("Internal error: schema.sql is missing from the application.");
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AppException("Could not read the database schema: " + e.getMessage(), e);
        }
        StringBuilder cleaned = new StringBuilder();
        for (String line : script.split("\n")) {
            if (!line.trim().startsWith("--")) cleaned.append(line).append('\n');
        }
        inTransaction(c -> {
            try (Statement st = c.createStatement()) {
                for (String sql : cleaned.toString().split(";")) {
                    if (!sql.isBlank()) st.execute(sql);
                }
            }
            return null;
        });
    }

    public static String friendly(SQLException e) {
        String m = e.getMessage() == null ? "" : e.getMessage();
        if (m.contains("UNIQUE constraint failed")) return "A record with the same unique value already exists.";
        if (m.contains("FOREIGN KEY constraint failed")) return "This record is still being used by other records, so the change was not made.";
        if (m.contains("CHECK constraint failed")) return "One of the values is outside the allowed range.";
        if (m.toLowerCase().contains("locked") || m.toLowerCase().contains("busy"))
            return "The database is busy. Close any other copy of the application and try again.";
        return "Database error: " + m;
    }
}
