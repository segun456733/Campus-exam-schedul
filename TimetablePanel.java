package com.campus.examscheduler.ui;

import com.campus.examscheduler.service.SchedulingService;
import com.campus.examscheduler.service.TimetableRow;
import com.campus.examscheduler.service.ValidationException;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/** Timetable viewer with filters and manual rescheduling. */
public class TimetablePanel extends Page {
    private static final String ALL = "All";

    private final SimpleTableModel model = new SimpleTableModel("Date", "Day", "Start", "End", "Course Code", "Course Title",
            "Department", "Level", "Students", "Venue", "Status", "Note");
    private final JTable table = Ui.table(model);
    private final JComboBox<String> dateFilter = new JComboBox<>();
    private final JComboBox<String> deptFilter = new JComboBox<>();
    private final JComboBox<String> levelFilter = new JComboBox<>(new String[]{ALL, "ND1", "ND2"});
    private final JComboBox<String> venueFilter = new JComboBox<>();
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{ALL, "Scheduled", "Unscheduled"});
    private final JTextField codeFilter = Ui.textField(8);
    private final JLabel count = Ui.muted(" ");
    private List<TimetableRow> all = new ArrayList<>();
    private List<TimetableRow> shown = new ArrayList<>();
    private boolean loading;

    public TimetablePanel() {
        super("Timetable", "Scheduled and unscheduled examinations. Manual changes are checked against every scheduling rule.");
        Ui.widths(table, 85, 80, 50, 50, 110, 200, 150, 50, 60, 130, 85, 250);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(Ui.fieldLabel("Date")); filters.add(dateFilter);
        filters.add(Ui.fieldLabel("Dept")); filters.add(deptFilter);
        filters.add(Ui.fieldLabel("Level")); filters.add(levelFilter);
        filters.add(Ui.fieldLabel("Venue")); filters.add(venueFilter);
        filters.add(Ui.fieldLabel("Status")); filters.add(statusFilter);
        filters.add(Ui.fieldLabel("Course")); filters.add(codeFilter);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(Ui.button("Reschedule / change venue", AppButton.Kind.PRIMARY, this::reschedule));
        actions.add(Ui.button("Remove from timetable", AppButton.Kind.SECONDARY, this::unschedule));
        actions.add(Ui.button("Reset filters", AppButton.Kind.SECONDARY, this::resetFilters));

        JPanel north = new JPanel(new BorderLayout(0, 10));
        north.setOpaque(false);
        north.add(filters, BorderLayout.NORTH);
        north.add(actions, BorderLayout.SOUTH);
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10), 16);
        card.add(north, BorderLayout.NORTH);
        card.add(Ui.scroll(table), BorderLayout.CENTER);
        card.add(count, BorderLayout.SOUTH);
        setBody(card);

        for (JComboBox<String> c : List.of(dateFilter, deptFilter, levelFilter, venueFilter, statusFilter))
            c.addActionListener(e -> applyFilters());
        codeFilter.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilters(); }
            public void removeUpdate(DocumentEvent e) { applyFilters(); }
            public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { if (e.getClickCount() == 2) reschedule(); }
        });
    }

    @Override public void refresh() {
        all = SchedulingService.timetable();
        loading = true;
        fill(dateFilter, distinct(all, 0));
        fill(deptFilter, distinct(all, 1));
        fill(venueFilter, distinct(all, 2));
        loading = false;
        applyFilters();
    }

    private static List<String> distinct(List<TimetableRow> rows, int what) {
        TreeSet<String> set = new TreeSet<>();
        for (TimetableRow r : rows) {
            switch (what) {
                case 0 -> { if (r.date() != null) set.add(r.date().toString()); }
                case 1 -> { for (String d : r.departments().split(", ")) if (!d.isBlank()) set.add(d); }
                default -> { if (!r.venue().isBlank()) set.add(r.venue()); }
            }
        }
        return new ArrayList<>(set);
    }

    private static void fill(JComboBox<String> box, List<String> items) {
        Object keep = box.getSelectedItem();
        box.removeAllItems();
        box.addItem(ALL);
        for (String s : items) box.addItem(s);
        if (keep != null) box.setSelectedItem(keep);
        if (box.getSelectedItem() == null) box.setSelectedIndex(0);
    }

    private void applyFilters() {
        if (loading) return;
        String date = (String) dateFilter.getSelectedItem(), dept = (String) deptFilter.getSelectedItem();
        String level = (String) levelFilter.getSelectedItem(), venue = (String) venueFilter.getSelectedItem();
        String status = (String) statusFilter.getSelectedItem();
        String code = codeFilter.getText().trim().toLowerCase(Locale.ROOT);
        shown = new ArrayList<>();
        List<Object[]> rows = new ArrayList<>();
        for (TimetableRow r : all) {
            if (date != null && !ALL.equals(date) && (r.date() == null || !r.date().toString().equals(date))) continue;
            if (dept != null && !ALL.equals(dept) && !r.departments().contains(dept)) continue;
            if (level != null && !ALL.equals(level) && !r.levels().contains(level)) continue;
            if (venue != null && !ALL.equals(venue) && !r.venue().equals(venue)) continue;
            if (status != null && !ALL.equals(status) && !r.status().equals(status)) continue;
            if (!code.isEmpty() && !r.codes().toLowerCase(Locale.ROOT).contains(code)) continue;
            shown.add(r);
            rows.add(new Object[]{r.date() == null ? "" : r.date().toString(), r.day(), r.start(), r.end(), r.codes(), r.titles(),
                    r.departments(), r.levels(), r.students(), r.venue(), r.status(), r.note()});
        }
        model.setRows(rows);
        long unsched = shown.stream().filter(r -> !r.scheduled()).count();
        count.setText(shown.size() + " of " + all.size() + " examination(s) shown (" + unsched + " unscheduled). "
                + "Double-click a row to reschedule it.");
    }

    private void resetFilters() {
        loading = true;
        for (JComboBox<String> c : List.of(dateFilter, deptFilter, levelFilter, venueFilter, statusFilter)) if (c.getItemCount() > 0) c.setSelectedIndex(0);
        codeFilter.setText("");
        loading = false;
        applyFilters();
    }

    private TimetableRow selected() {
        int row = Ui.selectedModelRow(table);
        if (row < 0) throw new ValidationException("Select an examination in the table first.");
        return shown.get(row);
    }

    private void reschedule() {
        RescheduleDialog d = new RescheduleDialog(this, selected());
        d.setVisible(true);
        if (d.isSaved()) refresh();
    }

    private void unschedule() {
        TimetableRow r = selected();
        if (!r.scheduled()) throw new ValidationException(r.codes() + " is not scheduled.");
        if (!Ui.confirm(this, "Remove from timetable", "Remove " + r.codes() + " from the timetable? It becomes unscheduled.")) return;
        SchedulingService.unschedule(r.eventKey());
        refresh();
    }
}
