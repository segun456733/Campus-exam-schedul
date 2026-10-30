package com.campus.examscheduler.ui;

import com.campus.examscheduler.scheduler.SchedulingRules;
import com.campus.examscheduler.scheduler.TimeRange;
import com.campus.examscheduler.service.SchedulingService;
import com.campus.examscheduler.service.SettingsService;
import com.campus.examscheduler.service.ValidationException;
import com.campus.examscheduler.util.TimeUtil;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Examination period configuration: dates, permitted days, hours, breaks and restrictions. */
public class ExamSettingsPanel extends Page {
    private final JTextField startDate = Ui.textField(10);
    private final JTextField endDate = Ui.textField(10);
    private final JTextField dayStart = Ui.textField(6);
    private final JTextField dayEnd = Ui.textField(6);
    private final JSpinner step = new JSpinner(new SpinnerNumberModel(30, 5, 120, 5));
    private final JSpinner gap = new JSpinner(new SpinnerNumberModel(0, 0, 240, 5));
    private final JSpinner maxSim = new JSpinner(new SpinnerNumberModel(4, 1, 50, 1));
    private final JSpinner maxPerDay = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));
    private final JCheckBox noCrossDept = new JCheckBox("No simultaneous exams across departments");
    private final JCheckBox noCrossLevel = new JCheckBox("No simultaneous exams across levels (ND1/ND2)");
    private final Map<DayOfWeek, JCheckBox> days = new EnumMap<>(DayOfWeek.class);
    private final SimpleTableModel breakModel = new SimpleTableModel("Break", "From", "To");
    private final JTable breakTable = Ui.table(breakModel);
    private final JTextField breakLabel = Ui.textField(10);
    private final JTextField breakFrom = Ui.textField(5);
    private final JTextField breakTo = Ui.textField(5);
    private final List<TimeRange> breaks = new ArrayList<>();

    public ExamSettingsPanel() {
        super("Examination Settings", "The scheduler never places an examination outside these dates, days, hours and breaks");

        JPanel daysRow = new JPanel(new GridLayout(2, 4, 4, 0));
        daysRow.setOpaque(false);
        for (DayOfWeek d : DayOfWeek.values()) {
            JCheckBox cb = new JCheckBox(d.getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            cb.setOpaque(false);
            days.put(d, cb);
            daysRow.add(cb);
        }
        noCrossDept.setOpaque(false);
        noCrossLevel.setOpaque(false);

        Ui.Form period = new Ui.Form();
        period.add("Start date (yyyy-MM-dd)", startDate).add("End date (yyyy-MM-dd)", endDate).add("Permitted days", daysRow)
                .add("Daily start time (HH:mm)", dayStart).add("Daily closing time (HH:mm)", dayEnd)
                .add("Start-time step (minutes)", step);
        RoundedPanel left = new RoundedPanel(new BorderLayout(0, 8), 18);
        left.add(Ui.sectionTitle("Period and daily hours"), BorderLayout.NORTH);
        left.add(period, BorderLayout.CENTER);

        Ui.Form rules = new Ui.Form();
        rules.add("Gap between sessions (min)", gap).add("Max simultaneous exams", maxSim)
                .add("Max exams per group per day", maxPerDay);
        JPanel policies = new JPanel();
        policies.setOpaque(false);
        policies.setLayout(new javax.swing.BoxLayout(policies, javax.swing.BoxLayout.Y_AXIS));
        policies.add(Ui.fieldLabel("Strict policies (optional)"));
        policies.add(noCrossDept);
        policies.add(noCrossLevel);
        RoundedPanel mid = new RoundedPanel(new BorderLayout(0, 8), 18);
        mid.add(Ui.sectionTitle("Restrictions"), BorderLayout.NORTH);
        mid.add(rules, BorderLayout.CENTER);
        mid.add(policies, BorderLayout.SOUTH);

        JPanel breakInputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        breakInputs.setOpaque(false);
        breakInputs.add(Ui.fieldLabel("Name"));
        breakInputs.add(breakLabel);
        breakInputs.add(Ui.fieldLabel("From"));
        breakInputs.add(breakFrom);
        breakInputs.add(Ui.fieldLabel("To"));
        breakInputs.add(breakTo);
        breakInputs.add(Ui.button("Add break", AppButton.Kind.SECONDARY, this::addBreak));
        breakInputs.add(Ui.button("Remove selected", AppButton.Kind.SECONDARY, this::removeBreak));
        RoundedPanel breaksCard = new RoundedPanel(new BorderLayout(0, 8), 18);
        breaksCard.add(Ui.sectionTitle("Daily break periods (no exam may overlap them)"), BorderLayout.NORTH);
        JScrollPane bs = Ui.scroll(breakTable);
        bs.setPreferredSize(new java.awt.Dimension(300, 110));
        breaksCard.add(bs, BorderLayout.CENTER);
        breaksCard.add(breakInputs, BorderLayout.SOUTH);

        JPanel top = new JPanel(new GridLayout(1, 2, 16, 0));
        top.setOpaque(false);
        top.add(left);
        top.add(mid);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(Ui.button("Save settings", AppButton.Kind.PRIMARY, this::save));
        actions.add(Ui.button("Check settings", AppButton.Kind.SECONDARY, this::check));
        actions.add(Ui.button("Discard changes", AppButton.Kind.SECONDARY, this::refresh));

        JPanel body = new WidthTrackingPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        body.add(top, BorderLayout.NORTH);
        JPanel lower = new JPanel(new BorderLayout(0, 14));
        lower.setOpaque(false);
        lower.add(breaksCard, BorderLayout.CENTER);
        lower.add(actions, BorderLayout.SOUTH);
        body.add(lower, BorderLayout.CENTER);
        JScrollPane scroller = new JScrollPane(body);
        scroller.setBorder(null);
        scroller.setOpaque(false);
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.getViewport().setOpaque(false);
        scroller.getVerticalScrollBar().setUnitIncrement(16);
        setBody(scroller);
    }

    /** Panel that always matches the scroll pane's width, so nothing is cut off sideways. */
    private static final class WidthTrackingPanel extends JPanel implements javax.swing.Scrollable {
        WidthTrackingPanel(java.awt.LayoutManager lm) { super(lm); }
        @Override public java.awt.Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(java.awt.Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(java.awt.Rectangle r, int o, int d) { return 80; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    @Override public void refresh() {
        SchedulingRules r = SettingsService.load();
        startDate.setText(r.getStartDate().toString());
        endDate.setText(r.getEndDate().toString());
        dayStart.setText(TimeUtil.formatMinutes(r.getDayStartMin()));
        dayEnd.setText(TimeUtil.formatMinutes(r.getDayEndMin()));
        step.setValue(r.getSlotStepMin());
        gap.setValue(r.getGapMin());
        maxSim.setValue(r.getMaxSimultaneous());
        maxPerDay.setValue(r.getMaxExamsPerGroupPerDay());
        noCrossDept.setSelected(r.isNoCrossDepartment());
        noCrossLevel.setSelected(r.isNoCrossLevel());
        for (Map.Entry<DayOfWeek, JCheckBox> e : days.entrySet()) e.getValue().setSelected(r.getAllowedDays().contains(e.getKey()));
        breaks.clear();
        breaks.addAll(r.getBreaks());
        showBreaks();
    }

    private void showBreaks() {
        List<Object[]> rows = new ArrayList<>();
        for (TimeRange b : breaks) rows.add(new Object[]{b.label(), TimeUtil.formatMinutes(b.start()), TimeUtil.formatMinutes(b.end())});
        breakModel.setRows(rows);
    }

    private void addBreak() {
        String label = breakLabel.getText().trim();
        if (label.isEmpty()) throw new ValidationException("Enter a name for the break (for example Lunch).");
        int from, to;
        try {
            from = TimeUtil.parseMinutes(breakFrom.getText());
            to = TimeUtil.parseMinutes(breakTo.getText());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        }
        if (from >= to) throw new ValidationException("The break must end after it starts.");
        breaks.add(new TimeRange(label, from, to));
        showBreaks();
        breakLabel.setText(""); breakFrom.setText(""); breakTo.setText("");
    }

    private void removeBreak() {
        int row = Ui.selectedModelRow(breakTable);
        if (row < 0) throw new ValidationException("Select a break in the list first.");
        breaks.remove(row);
        showBreaks();
    }

    private SchedulingRules fromForm() {
        SchedulingRules r = new SchedulingRules();
        try {
            r.setStartDate(TimeUtil.parseDate(startDate.getText(), "Start date"));
            r.setEndDate(TimeUtil.parseDate(endDate.getText(), "End date"));
            r.setDayStartMin(TimeUtil.parseMinutes(dayStart.getText()));
            r.setDayEndMin(TimeUtil.parseMinutes(dayEnd.getText()));
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        }
        Set<DayOfWeek> chosen = EnumSet.noneOf(DayOfWeek.class);
        for (Map.Entry<DayOfWeek, JCheckBox> e : days.entrySet()) if (e.getValue().isSelected()) chosen.add(e.getKey());
        r.setAllowedDays(chosen);
        r.setSlotStepMin((Integer) step.getValue());
        r.setGapMin((Integer) gap.getValue());
        r.setMaxSimultaneous((Integer) maxSim.getValue());
        r.setMaxExamsPerGroupPerDay((Integer) maxPerDay.getValue());
        r.setNoCrossDepartment(noCrossDept.isSelected());
        r.setNoCrossLevel(noCrossLevel.isSelected());
        r.setBreaks(new ArrayList<>(breaks));
        return r;
    }

    private void check() {
        List<String> errors = fromForm().validate();
        if (errors.isEmpty()) Ui.info(this, "The examination period is valid. " + fromForm().examDates().size() + " examination day(s) are available.");
        else throw new ValidationException(errors);
    }

    private void save() {
        SettingsService.save(fromForm());
        int conflicts = SchedulingService.validateSaved().size();
        String msg = "Examination settings saved.";
        if (conflicts > 0) {
            Ui.warn(this, msg + "\n\nThe timetable that is already saved now has " + conflicts + " conflict(s) with these settings. "
                    + "Open Generate Timetable to create a new one, or see Reports > Conflict report.");
        } else {
            Ui.info(this, msg);
        }
    }
}
