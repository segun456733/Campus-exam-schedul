package com.campus.examscheduler.ui;

import com.campus.examscheduler.service.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Shared helpers: message dialogs, styled tables, forms. */
public final class Ui {
    private Ui() { }

    // ---------------------------------------------------------------- dialogs

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, wrap(message), "Cannot continue", JOptionPane.ERROR_MESSAGE);
    }

    public static void warn(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, wrap(message), "Warning", JOptionPane.WARNING_MESSAGE);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, wrap(message), "Done", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String title, String message) {
        return JOptionPane.showConfirmDialog(parent, wrap(message), title, JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Shows any exception as a friendly message (validation problems are listed, not stack traces). */
    public static void showException(Component parent, Throwable t) {
        if (t instanceof ValidationException ve && ve.getProblems().size() > 1) {
            StringBuilder sb = new StringBuilder("Please fix the following:\n");
            for (String p : ve.getProblems()) sb.append("\u2022 ").append(p).append('\n');
            error(parent, sb.toString());
        } else {
            error(parent, t.getMessage() == null ? t.toString() : t.getMessage());
        }
        if (!(t instanceof ValidationException)) t.printStackTrace();      // keep technical details in the console log
    }

    private static Object wrap(String message) {
        JTextArea area = new JTextArea(message);
        area.setEditable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Theme.plain(13));
        int lines = Math.min(14, Math.max(2, message.split("\n").length + message.length() / 70));
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(null);
        sp.setPreferredSize(new Dimension(460, 22 * lines + 8));
        return sp;
    }

    // ---------------------------------------------------------------- labels and fields

    public static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.bold(22));
        l.setForeground(Theme.TEXT);
        return l;
    }

    public static JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.bold(15));
        l.setForeground(Theme.TEXT);
        return l;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.plain(12));
        l.setForeground(Theme.MUTED);
        return l;
    }

    public static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.bold(12));
        l.setForeground(Theme.TEXT);
        return l;
    }

    public static JTextField textField(int columns) {
        JTextField f = new JTextField(columns);
        f.setFont(Theme.plain(13));
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER), new EmptyBorder(5, 8, 5, 8)));
        return f;
    }

    public static JPanel buttonRow(Component... buttons) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        for (Component b : buttons) p.add(b);
        return p;
    }

    public static AppButton button(String text, AppButton.Kind kind, Runnable action) {
        AppButton b = new AppButton(text, kind);
        b.addActionListener(e -> {
            try {
                action.run();
            } catch (RuntimeException ex) {
                showException(b, ex);
            }
        });
        return b;
    }

    /** A two-column form (label, field) with consistent spacing. */
    public static class Form extends JPanel {
        private int row = 0;

        public Form() {
            super(new GridBagLayout());
            setOpaque(false);
        }

        public Form add(String label, JComponent field) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0; c.gridy = row; c.anchor = GridBagConstraints.WEST; c.insets = new Insets(6, 0, 6, 14);
            add(fieldLabel(label), c);
            c = new GridBagConstraints();
            c.gridx = 1; c.gridy = row; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(6, 0, 6, 0);
            add(field, c);
            row++;
            return this;
        }
    }

    // ---------------------------------------------------------------- tables

    /** Creates a styled, sortable, single-selection table. */
    public static JTable table(SimpleTableModel model) {
        JTable t = new JTable(model);
        t.setFont(Theme.plain(12));
        t.setRowHeight(32);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setSelectionBackground(Theme.SELECTION);
        t.setSelectionForeground(Theme.TEXT);
        t.setForeground(Theme.TEXT);
        t.setFillsViewportHeight(true);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        t.setRowSorter(new TableRowSorter<>(model));
        t.setDefaultRenderer(Object.class, new CellRenderer());
        t.setDefaultRenderer(Integer.class, new CellRenderer());
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(100, 34));
        h.setDefaultRenderer(new HeaderRenderer());
        return t;
    }

    public static JScrollPane scroll(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    /** Sets preferred widths (pixels) for the first columns of a table. */
    public static void widths(JTable t, int... w) {
        for (int i = 0; i < w.length && i < t.getColumnCount(); i++) t.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
    }

    /** The model row of the selected table row, or -1. */
    public static int selectedModelRow(JTable t) {
        int v = t.getSelectedRow();
        return v < 0 ? -1 : t.convertRowIndexToModel(v);
    }

    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        HeaderRenderer() { setHorizontalAlignment(LEFT); }
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v, false, false, r, c);
            setBackground(Theme.HEADER_BG);
            setForeground(Theme.MUTED);
            setFont(Theme.bold(11));
            setBorder(new EmptyBorder(0, 10, 0, 10));
            setOpaque(true);
            return this;
        }
    }

    private static final class CellRenderer implements TableCellRenderer {
        private final DefaultTableCellRenderer base = new DefaultTableCellRenderer();

        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            JLabel l = (JLabel) base.getTableCellRendererComponent(t, v, sel, false, r, c);
            l.setBorder(new EmptyBorder(0, 10, 0, 10));
            l.setFont(Theme.plain(12));
            l.setBackground(sel ? Theme.SELECTION : (r % 2 == 0 ? Color.WHITE : new Color(0xF9FAFD)));
            l.setForeground(Theme.TEXT);
            String s = v == null ? "" : v.toString();
            if (s.equals("Scheduled") || s.equals("Available") || s.equals("Yes")) { l.setForeground(Theme.SUCCESS); l.setFont(Theme.bold(12)); }
            if (s.equals("Unscheduled") || s.equals("Disabled")) { l.setForeground(Theme.DANGER); l.setFont(Theme.bold(12)); }
            l.setToolTipText(s.length() > 30 ? s : null);
            return l;
        }
    }
}
