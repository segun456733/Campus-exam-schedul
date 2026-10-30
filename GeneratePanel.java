package com.campus.examscheduler.ui;

import com.campus.examscheduler.scheduler.ConstraintChecker;
import com.campus.examscheduler.scheduler.Placement;
import com.campus.examscheduler.scheduler.ScheduleResult;
import com.campus.examscheduler.scheduler.Unscheduled;
import com.campus.examscheduler.service.SchedulingService;
import com.campus.examscheduler.service.TimetableRow;
import com.campus.examscheduler.service.ValidationException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Generate, preview, regenerate and save the timetable. Generation runs in a SwingWorker so the window stays responsive. */
public class GeneratePanel extends Page {
    private final Consumer<String> navigator;
    private final SimpleTableModel previewModel = new SimpleTableModel("Date", "Day", "Start", "End", "Course", "Department", "Level", "Students", "Venue");
    private final SimpleTableModel unschedModel = new SimpleTableModel("Course", "Department", "Level", "Students", "Reason");
    private final JTextArea log = new JTextArea();
    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel summary = new JLabel(" ");
    private final JLabel savedInfo = Ui.muted(" ");
    private final JProgressBar progress = new JProgressBar();
    private final AppButton generate = Ui.button("Generate timetable", AppButton.Kind.PRIMARY, () -> run(0));
    private final AppButton regenerate = Ui.button("Regenerate", AppButton.Kind.SECONDARY, () -> run(this.variant + 1));
    private final AppButton save = Ui.button("Save timetable", AppButton.Kind.PRIMARY, this::save);
    private final AppButton clear = Ui.button("Clear saved timetable", AppButton.Kind.DANGER, this::clear);
    private ScheduleResult proposed;
    private int variant;

    public GeneratePanel(Consumer<String> navigator) {
        super("Generate Timetable", "Automatic, constraint-based scheduling with a preview before anything is saved");
        this.navigator = navigator;
        progress.setIndeterminate(false);
        progress.setStringPainted(true);
        progress.setString("Ready");
        progress.setPreferredSize(new java.awt.Dimension(220, 22));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.add(generate);
        bar.add(regenerate);
        bar.add(save);
        bar.add(clear);
        bar.add(Ui.button("View unscheduled", AppButton.Kind.SECONDARY, () -> tabs.setSelectedIndex(1)));
        bar.add(Ui.button("Warnings and errors", AppButton.Kind.SECONDARY, () -> tabs.setSelectedIndex(2)));
        bar.add(Ui.button("Open timetable", AppButton.Kind.SECONDARY, () -> navigator.accept(MainFrame.TIMETABLE)));
        save.setEnabled(false);
        regenerate.setEnabled(false);

        summary.setFont(Theme.bold(13));
        summary.setForeground(Theme.TEXT);
        JPanel status = new JPanel(new BorderLayout(12, 0));
        status.setOpaque(false);
        JPanel texts = new JPanel(new BorderLayout());
        texts.setOpaque(false);
        texts.add(summary, BorderLayout.NORTH);
        texts.add(savedInfo, BorderLayout.SOUTH);
        status.add(texts, BorderLayout.CENTER);
        status.add(progress, BorderLayout.EAST);

        javax.swing.JTable preview = Ui.table(previewModel);
        Ui.widths(preview, 90, 80, 55, 55, 220, 160, 55, 65, 140);
        javax.swing.JTable unsched = Ui.table(unschedModel);
        Ui.widths(unsched, 150, 150, 55, 65, 520);
        log.setEditable(false);
        log.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        tabs.addTab("Proposed timetable", Ui.scroll(preview));
        tabs.addTab("Unscheduled examinations", Ui.scroll(unsched));
        tabs.addTab("Warnings and errors", new JScrollPane(log));

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 12), 16);
        JPanel north = new JPanel(new BorderLayout(0, 10));
        north.setOpaque(false);
        north.add(bar, BorderLayout.NORTH);
        north.add(status, BorderLayout.SOUTH);
        card.add(north, BorderLayout.NORTH);
        card.add(tabs, BorderLayout.CENTER);
        setBody(card);
    }

    @Override public void refresh() {
        List<TimetableRow> rows = SchedulingService.timetable();
        long scheduled = rows.stream().filter(TimetableRow::scheduled).count();
        savedInfo.setText(scheduled == 0 ? "No timetable is saved yet."
                : "Saved timetable: " + scheduled + " of " + rows.size() + " examinations scheduled. Saving a new timetable replaces it.");
    }

    // ------------------------------------------------------------------ generate

    private void run(int newVariant) {
        variant = newVariant;
        setBusy(true);
        progress.setIndeterminate(true);
        progress.setString("Generating...");
        SwingWorker<ScheduleResult, String> worker = new SwingWorker<>() {
            @Override protected ScheduleResult doInBackground() {
                return SchedulingService.generate(variant, this::publish);
            }
            @Override protected void process(List<String> chunks) {
                progress.setString(chunks.get(chunks.size() - 1));
            }
            @Override protected void done() {
                progress.setIndeterminate(false);
                setBusy(false);
                try {
                    show(get());
                    progress.setString("Finished");
                } catch (ExecutionException ex) {
                    progress.setString("Failed");
                    Ui.showException(GeneratePanel.this, ex.getCause() == null ? ex : ex.getCause());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        worker.execute();
    }

    private void setBusy(boolean busy) {
        generate.setEnabled(!busy);
        regenerate.setEnabled(!busy && proposed != null);
        save.setEnabled(!busy && proposed != null && !proposed.getPlacements().isEmpty());
        clear.setEnabled(!busy);
    }

    private void show(ScheduleResult r) {
        proposed = r;
        List<Object[]> rows = new ArrayList<>();
        for (Placement p : r.getPlacements()) {
            rows.add(new Object[]{p.date().toString(), p.date().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    ConstraintChecker.time(p.startMin()), ConstraintChecker.time(p.endMin()), p.event().codes(),
                    p.event().departmentsText(), p.event().levelsText(), p.event().getStudents(), p.venue().getName()});
        }
        previewModel.setRows(rows);
        List<Object[]> un = new ArrayList<>();
        for (Unscheduled u : r.getUnscheduled())
            un.add(new Object[]{u.event().codes(), u.event().departmentsText(), u.event().levelsText(), u.event().getStudents(), u.reason()});
        unschedModel.setRows(un);

        int total = r.getPlacements().size() + r.getUnscheduled().size();
        if (r.isComplete()) {
            summary.setForeground(Theme.SUCCESS);
            summary.setText("All " + total + " examinations were scheduled without conflicts (" + r.getElapsedMs() + " ms). Review, then save.");
        } else {
            summary.setForeground(Theme.WARNING);
            summary.setText(r.getPlacements().size() + " of " + total + " scheduled; " + r.getUnscheduled().size()
                    + " could NOT be scheduled. See the Unscheduled tab.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Run ").append(variant == 0 ? "(best timetable)" : "(alternative #" + variant + ")").append(": ")
                .append(r.getNodesExplored()).append(" search steps, ").append(r.getElapsedMs()).append(" ms")
                .append(r.isUsedFallback() ? ", partial-timetable mode" : "").append("\n\n");
        sb.append("ERRORS (").append(r.getErrors().size()).append(")\n");
        for (String e : r.getErrors()) sb.append("  - ").append(e).append('\n');
        if (r.getErrors().isEmpty()) sb.append("  none - the final validation found no rule violations.\n");
        sb.append("\nWARNINGS (").append(r.getWarnings().size()).append(")\n");
        for (String w : r.getWarnings()) sb.append("  - ").append(w).append('\n');
        if (r.getWarnings().isEmpty()) sb.append("  none\n");
        sb.append("\nUNSCHEDULED (").append(r.getUnscheduled().size()).append(")\n");
        for (Unscheduled u : r.getUnscheduled()) sb.append("  - ").append(u.event().codes()).append(": ").append(u.reason()).append('\n');
        log.setText(sb.toString());
        log.setCaretPosition(0);
        tabs.setSelectedIndex(r.getUnscheduled().isEmpty() && r.getErrors().isEmpty() ? 0 : (r.getErrors().isEmpty() ? 1 : 2));
        setBusy(false);
    }

    // ------------------------------------------------------------------ save / clear

    private void save() {
        if (proposed == null) throw new ValidationException("Generate a timetable first.");
        if (!proposed.getUnscheduled().isEmpty() && !Ui.confirm(this, "Save partial timetable",
                proposed.getUnscheduled().size() + " examination(s) are NOT scheduled and will stay unscheduled.\n\n"
                        + "Save the partial timetable anyway?")) return;
        SchedulingService.save(proposed);
        Ui.info(this, "Timetable saved: " + proposed.getPlacements().size() + " examination(s) scheduled.");
        refresh();
        navigator.accept(MainFrame.TIMETABLE);
    }

    private void clear() {
        if (!Ui.confirm(this, "Clear timetable", "Delete the saved timetable and the preview?\n"
                + "Courses, venues and settings are kept. This cannot be undone.")) return;
        SchedulingService.clearSchedule();
        proposed = null;
        previewModel.setRows(new ArrayList<>());
        unschedModel.setRows(new ArrayList<>());
        log.setText("");
        summary.setText(" ");
        save.setEnabled(false);
        regenerate.setEnabled(false);
        refresh();
    }
}
