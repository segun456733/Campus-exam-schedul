package com.campus.examscheduler.util;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Minimal RFC-4180 CSV writer (UTF-8 with BOM so Excel opens accents correctly). */
public final class CsvUtil {
    private CsvUtil() { }

    public static String escape(String value) {
        String v = value == null ? "" : value;
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r"))
            return "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }

    public static void write(Path file, String[] header, List<String[]> rows) throws IOException {
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write('\uFEFF');
            w.write(line(header));
            for (String[] r : rows) w.write(line(r));
        }
    }

    private static String line(String[] cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(cells[i]));
        }
        return sb.append("\r\n").toString();
    }
}
