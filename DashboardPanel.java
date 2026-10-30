package com.campus.examscheduler.ui;

import com.campus.examscheduler.service.DashboardStats;
import com.campus.examscheduler.service.SchedulingService;
import com.campus.examscheduler.service.TimetableRow;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Dashboard: summary cards and an overview of upcoming examinations. All figures come from the database. */
public class DashboardPanel extends Page {
    private final JPanel cardGrid = new JPanel(new GridLayout(2, 4, 16, 16));
    private final SimpleTableModel model = new SimpleTableModel("Date", "Time", "Course", "Level", "Venue");
    private final JLabel tableTitle = Ui.sectionTitle("Upcoming examinations");
    private final JLabel statusLine = new JLabel();
    private final Consumer<String> navigator;

    public DashboardPanel(Consumer<String> navigator) {
        super("Dashboard", "Overview of the examination period, venues and timetable status");
        this.navigator = navigator;
        cardGrid.setOpaque(false);
        JTable table = Ui.table(model);
        Ui.widths(table, 110, 110, 260, 60, 150);

        RoundedPanel overview = new RoundedPanel(new BorderLayout(0, 10), 18);
        overview.add(tableTitle, BorderLayout.NORTH);
        overview.add(Ui.scroll(table), BorderLayout.CENTER);

        RoundedPanel actions = new RoundedPanel(new BorderLayout(0, 10), 18);
        actions.setPreferredSize(new java.awt.Dimension(300, 100));
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(Ui.sectionTitle("Timetable status"));
        box.add(javax.swing.Box.createVerticalStrut(8));
        statusLine.setFont(Theme.plain(12));
        statusLine.setVerticalAlignment(JLabel.TOP);
        box.add(statusLine);
        box.add(javax.swing.Box.createVerticalStrut(14));
        box.add(Ui.sectionTitle("Quick actions"));
        box.add(javax.swing.Box.createVerticalStrut(8));
        for (String[] a : new String[][]{{"Generate timetable", MainFrame.GENERATE}, {"View timetable", MainFrame.TIMETABLE},
                {"Examination settings", MainFrame.SETTINGS}, {"Manage courses", MainFrame.COURSES}}) {
            AppButton b = Ui.button(a[0], a[0].startsWith("Generate") ? AppButton.Kind.PRIMARY : AppButton.Kind.SECONDARY, () -> navigator.accept(a[1]));
            b.setAlignmentX(LEFT_ALIGNMENT);
            b.setMaximumSize(new java.awt.Dimension(240, 34));
            box.add(b);
            box.add(javax.swing.Box.createVerticalStrut(8));
        }
        actions.add(box, BorderLayout.NORTH);

        JPanel lower = new JPanel(new BorderLayout(16, 0));
        lower.setOpaque(false);
        lower.add(overview, BorderLayout.CENTER);
        lower.add(actions, BorderLayout.EAST);

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        cardGrid.setPreferredSize(new java.awt.Dimension(100, 230));
        body.add(cardGrid, BorderLayout.NORTH);
        body.add(lower, BorderLayout.CENTER);
        setBody(body);
    }

    @Override public void refresh() {
        DashboardStats s = SchedulingService.stats();
        cardGrid.removeAll();
        cardGrid.add(card("Departments", s.departments(), "in the School of Science", Theme.ACCENT));
        cardGrid.add(card("Courses", s.courses(), "with examinations", Theme.ACCENT));
        cardGrid.add(card("Venues", s.venues(), s.availableVenues() + " available", Theme.ACCENT));
        cardGrid.add(card("Examination days", s.examDays(), "permitted days in the period", Theme.ACCENT));
        cardGrid.add(card("Total examinations", s.totalExams(), "joint exams counted once", new Color(0x6D5EF5)));
        cardGrid.add(card("Scheduled", s.scheduled(), "saved in the timetable", Theme.SUCCESS));
        cardGrid.add(card("Unscheduled", s.unscheduled(), s.unscheduled() == 0 ? "everything has a slot" : "need attention",
                s.unscheduled() == 0 ? Theme.SUCCESS : Theme.WARNING));
        cardGrid.add(card("Conflicts detected", s.conflicts(), s.conflicts() == 0 ? "saved timetable is clean" : "see Reports",
                s.conflicts() == 0 ? Theme.SUCCESS : Theme.DANGER));
        cardGrid.revalidate();
        cardGrid.repaint();

        List<Object[]> rows = new ArrayList<>();
        for (TimetableRow r : s.upcoming())
            rows.add(new Object[]{r.date().toString() + " " + r.day().substring(0, 3), r.start() + " - " + r.end(),
                    r.codes(), r.levels(), r.venue()});
        model.setRows(rows);
        tableTitle.setText(s.upcomingIsFuture() ? "Upcoming examinations" : (rows.isEmpty() ? "Upcoming examinations" : "Timetable overview (first examinations)"));

        String text;
        if (s.totalExams() == 0) text = "No courses yet. Add courses, then generate a timetable.";
        else if (s.scheduled() == 0) text = "No timetable has been saved yet. Open Generate Timetable.";
        else if (s.unscheduled() > 0) text = s.unscheduled() + " examination(s) are not scheduled. See the Unscheduled report.";
        else if (s.conflicts() > 0) text = "The saved timetable has " + s.conflicts() + " conflict(s) with the current settings.";
        else text = "The timetable is complete and conflict-free.";
        statusLine.setText("<html><body style='width:230px'>" + text + "</body></html>");
    }

    private JPanel card(String title, int value, String caption, Color color) {
        RoundedPanel p = new RoundedPanel(new BorderLayout(0, 4), 16);
        JLabel t = new JLabel(title);
        t.setFont(Theme.bold(12));
        t.setForeground(Theme.MUTED);
        JLabel v = new JLabel(String.valueOf(value));
        v.setFont(Theme.bold(34));
        v.setForeground(color);
        JLabel c = new JLabel(caption);
        c.setFont(Theme.plain(11));
        c.setForeground(Theme.MUTED);
        p.add(t, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        p.add(c, BorderLayout.SOUTH);
        return p;
    }
}
