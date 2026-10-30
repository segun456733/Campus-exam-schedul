package com.campus.examscheduler.ui;

import com.campus.examscheduler.model.Course;
import com.campus.examscheduler.model.Department;
import com.campus.examscheduler.model.StudentGroup;
import com.campus.examscheduler.service.CourseService;
import com.campus.examscheduler.service.DepartmentService;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.List;

/** Add / edit one course. */
public class CourseDialog extends JDialog {
    private final Course existing;
    private final JTextField code = Ui.textField(20);
    private final JTextField title = Ui.textField(20);
    private final JComboBox<Department> department = new JComboBox<>();
    private final JComboBox<String> level = new JComboBox<>(new String[]{"ND1", "ND2"});
    private final JSpinner students = new JSpinner(new SpinnerNumberModel(50, 1, 5000, 1));
    private final JSpinner duration = new JSpinner(new SpinnerNumberModel(120, 15, 480, 5));
    private final JComboBox<StudentGroup> cohort = new JComboBox<>();
    private final JTextField extraGroups = Ui.textField(20);
    private final JComboBox<String> joint = new JComboBox<>();
    private final List<StudentGroup> groups;
    private boolean saved;

    public CourseDialog(Component owner, Course existing) {
        super((JFrame) SwingUtilities.getWindowAncestor(owner), existing == null ? "Add course" : "Edit course", true);
        this.existing = existing;
        for (Department d : DepartmentService.list()) department.addItem(d);
        groups = CourseService.groups();
        for (StudentGroup g : groups) cohort.addItem(g);
        joint.setEditable(true);
        joint.addItem("");
        for (String j : CourseService.jointNames()) joint.addItem(j);
        extraGroups.setToolTipText("Other student groups that also sit this course, e.g. CSC-ND1, STA-ND1");

        Ui.Form form = new Ui.Form();
        form.add("Course code", code).add("Course title", title).add("Department", department).add("Student level", level)
                .add("Number of students", students).add("Exam duration (minutes)", duration)
                .add("Student group (cohort)", cohort).add("Also shared with groups", extraGroups)
                .add("Joint examination name", joint);
        JLabel hint = Ui.muted("<html>Shared groups: comma-separated, e.g. CSC-ND1, STA-ND1. Courses with the same joint examination name "
                + "are sat together as ONE examination (students are added up).</html>");

        department.addActionListener(e -> syncCohort());
        level.addActionListener(e -> syncCohort());

        if (existing != null) {
            code.setText(existing.getCode());
            title.setText(existing.getTitle());
            for (int i = 0; i < department.getItemCount(); i++) if (department.getItemAt(i).getId() == existing.getDepartmentId()) department.setSelectedIndex(i);
            level.setSelectedItem(existing.getLevel());
            students.setValue(existing.getStudentCount());
            duration.setValue(existing.getDurationMinutes());
            for (int i = 0; i < cohort.getItemCount(); i++) if (cohort.getItemAt(i).getId() == existing.getGroupId()) cohort.setSelectedIndex(i);
            extraGroups.setText(String.join(", ", existing.getExtraGroupNames()));
            joint.setSelectedItem(existing.getJointName());
        } else {
            syncCohort();
        }

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(Ui.button("Cancel", AppButton.Kind.SECONDARY, this::dispose));
        buttons.add(Ui.button("Save course", AppButton.Kind.PRIMARY, this::save));

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBackground(Theme.BG);
        content.setBorder(BorderFactory.createEmptyBorder(18, 22, 16, 22));
        content.add(Ui.sectionTitle(existing == null ? "New course" : "Edit course"), BorderLayout.NORTH);
        JPanel mid = new JPanel(new BorderLayout(0, 6));
        mid.setOpaque(false);
        mid.add(form, BorderLayout.NORTH);
        mid.add(hint, BorderLayout.CENTER);
        content.add(mid, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        setSize(560, 600);
        setLocationRelativeTo(owner);
    }

    /** Picks the cohort that matches the chosen department and level (the user may still override it). */
    private void syncCohort() {
        Department d = (Department) department.getSelectedItem();
        String lv = (String) level.getSelectedItem();
        if (d == null || lv == null) return;
        for (int i = 0; i < cohort.getItemCount(); i++) {
            StudentGroup g = cohort.getItemAt(i);
            if (g.getDepartmentId() == d.getId() && g.getLevel().equals(lv)) { cohort.setSelectedIndex(i); return; }
        }
    }

    private void save() {
        Course c = new Course();
        c.setId(existing == null ? 0 : existing.getId());
        c.setCode(code.getText());
        c.setTitle(title.getText());
        Department d = (Department) department.getSelectedItem();
        c.setDepartmentId(d == null ? 0 : d.getId());
        c.setLevel((String) level.getSelectedItem());
        c.setStudentCount((Integer) students.getValue());
        c.setDurationMinutes((Integer) duration.getValue());
        StudentGroup g = (StudentGroup) cohort.getSelectedItem();
        c.setGroupId(g == null ? 0 : g.getId());
        c.setExtraGroupIds(CourseService.resolveGroups(extraGroups.getText()));
        Object j = joint.getEditor().getItem();
        c.setJointName(j == null ? "" : j.toString().trim());
        CourseService.save(c);
        saved = true;
        dispose();
    }

    public boolean isSaved() { return saved; }
}
