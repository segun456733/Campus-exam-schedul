package com.campus.examscheduler.ui;

import com.campus.examscheduler.model.Venue;
import com.campus.examscheduler.scheduler.Violation;
import com.campus.examscheduler.service.SchedulingService;
import com.campus.examscheduler.service.TimetableRow;
import com.campus.examscheduler.service.ValidationException;
import com.campus.examscheduler.service.VenueService;
import com.campus.examscheduler.util.TimeUtil;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.List;

/**
 * Manual reschedule / venue change. The proposed change is validated with the SAME rules as the
 * automatic generator and is rejected (with an explanation) when it breaks any of them.
 */
public class RescheduleDialog extends JDialog {
    private final TimetableRow row;
    private final JTextField date = Ui.textField(10);
    private final JTextField start = Ui.textField(6);
    private final JComboBox<Venue> venue = new JComboBox<>();
    private final JTextArea message = new JTextArea(5, 40);
    private boolean saved;

    public RescheduleDialog(Component owner, TimetableRow row) {
        super((JFrame) SwingUtilities.getWindowAncestor(owner), row.scheduled() ? "Reschedule / change venue" : "Schedule manually", true);
        this.row = row;
        List<Venue> venues = VenueService.list();
        for (Venue v : venues) {
            venue.addItem(v);
            if (row.scheduled() && v.getId() == row.venueId()) venue.setSelectedItem(v);
        }
        if (row.scheduled()) { date.setText(row.date().toString()); start.setText(row.start()); }
        else { date.setText(""); start.setText("09:00"); }

        Ui.Form form = new Ui.Form();
        form.add("Date (yyyy-MM-dd)", date).add("Start time (HH:mm)", start).add("Venue", venue);
        message.setEditable(false);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setFont(Theme.plain(12));
        message.setForeground(Theme.DANGER);
        message.setText("Choose a new date, time or venue, then press Check or Save.\nThe change is rejected if it breaks any scheduling rule.");
        message.setForeground(Theme.MUTED);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(Ui.button("Cancel", AppButton.Kind.SECONDARY, this::dispose));
        buttons.add(Ui.button("Check only", AppButton.Kind.SECONDARY, this::check));
        buttons.add(Ui.button("Save change", AppButton.Kind.PRIMARY, this::save));

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBackground(Theme.BG);
        content.setBorder(BorderFactory.createEmptyBorder(18, 22, 16, 22));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Ui.sectionTitle(row.codes()), BorderLayout.NORTH);
        head.add(Ui.muted(row.titles() + "  |  " + row.students() + " students  |  " + row.durationMin() + " min"), BorderLayout.SOUTH);
        JPanel mid = new JPanel(new BorderLayout(0, 8));
        mid.setOpaque(false);
        mid.add(form, BorderLayout.NORTH);
        mid.add(new JScrollPane(message), BorderLayout.CENTER);
        content.add(head, BorderLayout.NORTH);
        content.add(mid, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        setSize(540, 470);
        setLocationRelativeTo(owner);
    }

    private record Proposal(LocalDate date, int start, long venueId) { }

    private Proposal read() {
        Venue v = (Venue) venue.getSelectedItem();
        if (v == null) throw new ValidationException("Select a venue.");
        try {
            return new Proposal(TimeUtil.parseDate(date.getText(), "Date"), TimeUtil.parseMinutes(start.getText()), v.getId());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    private void check() {
        Proposal p = read();
        List<Violation> violations = SchedulingService.checkMove(row.eventKey(), p.date(), p.start(), p.venueId());
        if (violations.isEmpty()) {
            message.setForeground(Theme.SUCCESS);
            message.setText("This change is valid. No rule is broken.");
        } else {
            showViolations(violations);
        }
    }

    private void save() {
        Proposal p = read();
        List<Violation> violations = SchedulingService.checkMove(row.eventKey(), p.date(), p.start(), p.venueId());
        if (!violations.isEmpty()) {
            showViolations(violations);      // rejected: nothing is saved
            return;
        }
        SchedulingService.applyMove(row.eventKey(), p.date(), p.start(), p.venueId());
        saved = true;
        dispose();
    }

    private void showViolations(List<Violation> violations) {
        StringBuilder sb = new StringBuilder("Change rejected. It breaks " + violations.size() + " rule(s):\n");
        for (Violation v : violations) sb.append("\u2022 ").append(v.message()).append('\n');
        message.setForeground(Theme.DANGER);
        message.setText(sb.toString());
        message.setCaretPosition(0);
    }

    public boolean isSaved() { return saved; }
}
