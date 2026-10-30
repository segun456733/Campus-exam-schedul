package com.campus.examscheduler.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Tiny dependency-free PDF writer (landscape A4, built-in Helvetica font, tables with page breaks).
 * Written in plain Java so PDF export works fully offline with no external library.
 */
public final class SimplePdfWriter {
    private static final double PAGE_W = 842, PAGE_H = 595, MARGIN = 36, ROW_H = 16, FONT = 8;

    private SimplePdfWriter() { }

    public static void writeTable(Path file, String title, String subtitle, String[] header, List<String[]> rows) throws IOException {
        // Column widths proportional to the longest text in each column (bounded).
        int n = header.length;
        double[] weight = new double[n];
        for (int c = 0; c < n; c++) {
            int max = header[c].length();
            for (String[] r : rows) max = Math.max(max, r[c] == null ? 0 : Math.min(r[c].length(), 40));
            weight[c] = Math.max(6, Math.min(max, 40));
        }
        double total = 0;
        for (double w : weight) total += w;
        double usable = PAGE_W - 2 * MARGIN;
        double[] width = new double[n];
        for (int c = 0; c < n; c++) width[c] = usable * weight[c] / total;

        List<String> pages = new ArrayList<>();
        StringBuilder page = null;
        double y = 0;
        int pageNo = 0;
        for (int i = 0; i < Math.max(rows.size(), 1); i++) {
            if (page == null || y < MARGIN + ROW_H + 10) {          // start a new page (with title and header row)
                if (page != null) pages.add(footer(page, pageNo));
                page = new StringBuilder();
                pageNo++;
                y = PAGE_H - MARGIN;
                text(page, MARGIN, y, 14, true, title);
                y -= 16;
                text(page, MARGIN, y, 9, false, subtitle);
                y -= 22;
                y = row(page, y, width, header, true);
            }
            if (i < rows.size()) y = row(page, y, width, rows.get(i), false);
        }
        if (rows.isEmpty()) text(page, MARGIN, y - 4, 9, false, "(no records)");
        pages.add(footer(page, pageNo));
        Files.write(file, assemble(pages));
    }

    private static double row(StringBuilder p, double y, double[] width, String[] cells, boolean header) {
        double x = MARGIN;
        if (header) {
            p.append("0.13 0.2 0.45 rg ").append(fmt(MARGIN)).append(' ').append(fmt(y - 4)).append(' ')
                    .append(fmt(PAGE_W - 2 * MARGIN)).append(' ').append(fmt(ROW_H)).append(" re f 0 g\n");
        }
        for (int c = 0; c < width.length; c++) {
            String s = cells[c] == null ? "" : cells[c];
            int maxChars = (int) (width[c] / (FONT * 0.5)) - 1;
            if (s.length() > maxChars && maxChars > 3) s = s.substring(0, maxChars - 1) + "~";
            if (header) p.append("1 g\n");
            text(p, x + 2, y, FONT, header, s);
            if (header) p.append("0 g\n");
            x += width[c];
        }
        p.append("0.8 g ").append(fmt(MARGIN)).append(' ').append(fmt(y - 5)).append(' ')
                .append(fmt(PAGE_W - 2 * MARGIN)).append(" 0.4 re f 0 g\n");
        return y - ROW_H;
    }

    private static String footer(StringBuilder page, int no) {
        text(page, PAGE_W - MARGIN - 50, 20, 8, false, "Page " + no);
        return page.toString();
    }

    private static void text(StringBuilder sb, double x, double y, double size, boolean bold, String s) {
        sb.append("BT /").append(bold ? "F2" : "F1").append(' ').append(fmt(size)).append(" Tf ")
                .append(fmt(x)).append(' ').append(fmt(y)).append(" Td (").append(escape(s)).append(") Tj ET\n");
    }

    private static String fmt(double d) { return String.format(java.util.Locale.ROOT, "%.2f", d); }

    /** Keeps printable Latin-1 characters; escapes PDF string delimiters. */
    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (ch == '(' || ch == ')' || ch == '\\') sb.append('\\').append(ch);
            else if (ch >= 32 && ch < 127) sb.append(ch);
            else if (ch >= 160 && ch <= 255) sb.append(String.format("\\%03o", (int) ch));
            else sb.append('?');
        }
        return sb.toString();
    }

    private static byte[] assemble(List<String> pages) throws IOException {
        // Object layout: 1 catalog, 2 pages, 3 font regular, 4 font bold, then (page, content) pairs.
        List<byte[]> objects = new ArrayList<>();
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) kids.append(5 + 2 * i).append(" 0 R ");
        objects.add("<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.ISO_8859_1));
        objects.add(("<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>").getBytes(StandardCharsets.ISO_8859_1));
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>".getBytes(StandardCharsets.ISO_8859_1));
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>".getBytes(StandardCharsets.ISO_8859_1));
        for (int i = 0; i < pages.size(); i++) {
            int contentId = 6 + 2 * i;
            objects.add(("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + (int) PAGE_W + " " + (int) PAGE_H
                    + "] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + contentId + " 0 R >>")
                    .getBytes(StandardCharsets.ISO_8859_1));
            byte[] content = pages.get(i).getBytes(StandardCharsets.ISO_8859_1);
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            o.write(("<< /Length " + content.length + " >>\nstream\n").getBytes(StandardCharsets.ISO_8859_1));
            o.write(content);
            o.write("\nendstream".getBytes(StandardCharsets.ISO_8859_1));
            objects.add(o.toByteArray());
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
        int[] offsets = new int[objects.size()];
        for (int i = 0; i < objects.size(); i++) {
            offsets[i] = out.size();
            out.write(((i + 1) + " 0 obj\n").getBytes(StandardCharsets.ISO_8859_1));
            out.write(objects.get(i));
            out.write("\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        }
        int xref = out.size();
        out.write(("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n").getBytes(StandardCharsets.ISO_8859_1));
        for (int off : offsets) out.write(String.format("%010d 00000 n \n", off).getBytes(StandardCharsets.ISO_8859_1));
        out.write(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n")
                .getBytes(StandardCharsets.ISO_8859_1));
        return out.toByteArray();
    }
}
