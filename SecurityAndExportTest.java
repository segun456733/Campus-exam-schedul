package com.campus.examscheduler.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Password hashing, CSV and PDF export. */
class SecurityAndExportTest {

    @Test
    void passwordsAreSaltedAndVerified() {
        String salt1 = PasswordUtil.newSalt();
        String salt2 = PasswordUtil.newSalt();
        assertFalse(salt1.equals(salt2));
        String h1 = PasswordUtil.hash("Secret123", salt1, 1000);
        String h2 = PasswordUtil.hash("Secret123", salt2, 1000);
        assertFalse(h1.equals(h2));                                   // same password, different salt -> different hash
        assertFalse(h1.contains("Secret123"));                        // never plaintext
        assertTrue(PasswordUtil.verify("Secret123", salt1, 1000, h1));
        assertFalse(PasswordUtil.verify("secret123", salt1, 1000, h1));
    }

    @Test
    void passwordStrengthRules() {
        assertNotNull(PasswordUtil.checkStrength("short1"));
        assertNotNull(PasswordUtil.checkStrength("onlyletters"));
        assertNotNull(PasswordUtil.checkStrength("12345678"));
        assertNull(PasswordUtil.checkStrength("Admin@123"));
    }

    @Test
    void csvEscapesSpecialCharacters() throws Exception {
        assertEquals("plain", CsvUtil.escape("plain"));
        assertEquals("\"a,b\"", CsvUtil.escape("a,b"));
        assertEquals("\"say \"\"hi\"\"\"", CsvUtil.escape("say \"hi\""));
        Path f = Files.createTempFile("timetable", ".csv");
        CsvUtil.write(f, new String[]{"Date", "Course"}, Collections.singletonList(new String[]{"2026-11-16", "CSC111, CSC112"}));
        String text = Files.readString(f, StandardCharsets.UTF_8);
        assertTrue(text.contains("Date,Course"));
        assertTrue(text.contains("\"CSC111, CSC112\""));
        Files.deleteIfExists(f);
    }

    @Test
    void pdfIsAValidPdfFileWithAllPages() throws Exception {
        java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) rows.add(new String[]{"2026-11-16", "CSC" + i, "Title (x)", "ND1"});
        Path f = Files.createTempFile("timetable", ".pdf");
        SimplePdfWriter.writeTable(f, "Timetable", "test", new String[]{"Date", "Code", "Title", "Level"}, rows);
        String raw = new String(Files.readAllBytes(f), StandardCharsets.ISO_8859_1);
        assertTrue(raw.startsWith("%PDF-1.4"));
        assertTrue(raw.trim().endsWith("%%EOF"));
        assertFalse(raw.contains("/Count 1 "));      // 100 rows need several pages
        assertTrue(raw.contains("CSC99"));
        Files.deleteIfExists(f);
        // an empty report still produces a valid file
        Path e = Files.createTempFile("empty", ".pdf");
        SimplePdfWriter.writeTable(e, "Empty", "none", new String[]{"A"}, List.of());
        assertTrue(Files.size(e) > 200);
        Files.deleteIfExists(e);
    }
}
