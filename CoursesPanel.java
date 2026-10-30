package com.campus.examscheduler.ui;

import com.campus.examscheduler.model.Course;
import com.campus.examscheduler.model.Department;
import com.campus.examscheduler.service.CourseService;
import com.campus.examscheduler.service.DepartmentService;
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

/** Course management with search and filters. */
public class CoursesPanel extends Page {
    private static final String ALL_DEPTS = "All departments";
    private static final String ALL_LEVELS = "All levels";

    private final SimpleTableModel model = new SimpleTableModel("Code", "Title", "Department", "Level", "Students",
            "Duration (min)", "Student group", "Shared with", "Joint exam");
    private final JTable table = Ui.table(model);
    private final JTextField search = Ui.textField(16);
    private final JComboBox<Object> deptFilter = new JComboBox<>();
    private final JComboBox<String> levelFilter = new JComboBox<>(new String[]{ALL_LEVELS, "ND1", "ND2"});
    private final JLabel count = Ui.muted(" ");
    private List<Course> shown = new ArrayList<>();
    private boolean loading;

    public CoursesPanel() {
        super("Courses", "Add, edit, search and filter the courses that need examinations");
        Ui.widths(table, 70, 200, 150, 50, 60, 80, 90, 150, 140);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.add(Ui.fieldLabel("Search"));
        bar.add(search);
        bar.add(deptFilter);
        bar.add(levelFilter);
        bar.add(Ui.button("Add course", AppButton.Kind.PRIMARY, this::add));
        bar.add(Ui.button("Edit", AppButton.Kind.SECONDARY, this::edit));
        bar.add(Ui.button("Delete", AppButton.Kind.DANGER, this::delete));

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10), 16);
        card.add(bar, BorderLayout.NORTH);
        card.add(Ui.scroll(table), BorderLayout.CENTER);
        card.add(count, BorderLayout.SOUTH);
        setBody(card);

        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { reload(); }
            public void removeUpdate(DocumentEvent e) { reload(); }
            public void changedUpdate(DocumentEvent e) { reload(); }
        });
        deptFilter.addActionListener(e -> reload());
        levelFilter.addActionListener(e -> reload());
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { if (e.getClickCount() == 2) edit(); }
        });
    }

    @Override public void refresh() {
        loading = true;
        Object selected = deptFilter.getSelectedItem();
        deptFilter.removeAllItems();
        deptFilter.addItem(ALL_DEPTS);
        for (Department d : DepartmentService.list()) deptFilter.addItem(d);
        if (selected != null) deptFilter.setSelectedItem(selected);
        loading = false;
        reload();
    }

    private void reload() {
        if (loading) return;
        Object d = deptFilter.getSelectedItem();
        Long deptId = d instanceof Department dep ? dep.getId() : null;
        String lv = (String) levelFilter.getSelectedItem();
        shown = CourseService.search(search.getText(), deptId, ALL_LEVELS.equals(lv) ? null : lv);
        List<Object[]> rows = new ArrayList<>();
        for (Course c : shown) {
            rows.add(new Object[]{c.getCode(), c.getTitle(), c.getDepartmentName(), c.getLevel(), c.getStudentCount(),
                    c.getDurationMinutes(), c.getGroupName(), String.join(", ", c.getExtraGroupNames()), c.getJointName()});
        }
        model.setRows(rows);
        count.setText(shown.size() + " course(s) shown");
    }

    private Course selected() {
        int row = Ui.selectedModelRow(table);
        if (row < 0) throw new ValidationException("Select a course in the table first.");
        return shown.get(row);
    }

    private void add() {
        CourseDialog dlg = new CourseDialog(this, null);
        dlg.setVisible(true);
        if (dlg.isSaved()) reload();
    }

    private void edit() {
        CourseDialog dlg = new CourseDialog(this, selected());
        dlg.setVisible(true);
        if (dlg.isSaved()) reload();
    }

    private void delete() {
        Course c = selected();
        if (!Ui.confirm(this, "Delete course", "Delete course " + c.getCode() + " - " + c.getTitle() + "?\n"
                + "Its saved timetable entry (if any) is removed too.")) return;
        CourseService.delete(c.getId());
        reload();
    }
}
