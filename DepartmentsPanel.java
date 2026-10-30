package com.campus.examscheduler.ui;

import com.campus.examscheduler.model.Department;
import com.campus.examscheduler.service.DepartmentService;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/** Add, edit and delete departments. */
public class DepartmentsPanel extends Page {
    private final SimpleTableModel model = new SimpleTableModel("Code", "Department", "Description", "Courses");
    private final JTable table = Ui.table(model);
    private final JTextField code = Ui.textField(14);
    private final JTextField name = Ui.textField(14);
    private final JTextField description = Ui.textField(14);
    private final JLabel status = new JLabel(" ");
    private List<Department> current = new ArrayList<>();

    public DepartmentsPanel() {
        super("Departments", "Manage the departments of the School of Science (sample data is editable)");
        Ui.widths(table, 70, 220, 300, 70);

        RoundedPanel left = new RoundedPanel(new BorderLayout(0, 10), 16);
        left.add(Ui.scroll(table), BorderLayout.CENTER);

        RoundedPanel form = new RoundedPanel(new BorderLayout(0, 12), 18);
        form.setPreferredSize(new Dimension(330, 100));
        Ui.Form f = new Ui.Form();
        f.add("Code", code).add("Name", name).add("Description", description);
        JPanel buttons = new JPanel(new java.awt.GridLayout(2, 2, 8, 8));
        buttons.setOpaque(false);
        buttons.add(Ui.button("Add", AppButton.Kind.PRIMARY, this::add));
        buttons.add(Ui.button("Update", AppButton.Kind.SECONDARY, this::update));
        buttons.add(Ui.button("Delete", AppButton.Kind.DANGER, this::delete));
        buttons.add(Ui.button("Clear form", AppButton.Kind.SECONDARY, this::clearForm));
        status.setFont(Theme.plain(12));
        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);
        south.add(buttons, BorderLayout.NORTH);
        south.add(status, BorderLayout.SOUTH);
        form.add(Ui.sectionTitle("Department details"), BorderLayout.NORTH);
        JPanel mid = new JPanel(new BorderLayout());
        mid.setOpaque(false);
        mid.add(f, BorderLayout.NORTH);
        mid.add(south, BorderLayout.CENTER);
        form.add(mid, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);
        body.add(left, BorderLayout.CENTER);
        body.add(form, BorderLayout.EAST);
        setBody(body);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = Ui.selectedModelRow(table);
            if (row >= 0 && row < current.size()) {
                Department d = current.get(row);
                code.setText(d.getCode()); name.setText(d.getName()); description.setText(d.getDescription());
            }
        });
    }

    @Override public void refresh() {
        current = DepartmentService.list();
        List<Object[]> rows = new ArrayList<>();
        for (Department d : current) rows.add(new Object[]{d.getCode(), d.getName(), d.getDescription(), d.getCourseCount()});
        model.setRows(rows);
    }

    private Department fromForm(long id) {
        return new Department(id, code.getText(), name.getText(), description.getText());
    }

    private Department selected() {
        int row = Ui.selectedModelRow(table);
        if (row < 0) throw new com.campus.examscheduler.service.ValidationException("Select a department in the table first.");
        return current.get(row);
    }

    private void ok(String msg) { status.setForeground(Theme.SUCCESS); status.setText(msg); }

    private void add() {
        DepartmentService.save(fromForm(0));
        ok("Department added (ND1 and ND2 student groups were created).");
        refresh();
        clearForm();
    }

    private void update() {
        Department d = selected();
        DepartmentService.save(fromForm(d.getId()));
        ok("Department updated.");
        refresh();
    }

    private void delete() {
        Department d = selected();
        if (!Ui.confirm(this, "Delete department", "Delete '" + d.getName() + "'? This cannot be undone.")) return;
        DepartmentService.delete(d.getId());
        ok("Department deleted.");
        refresh();
        clearForm();
    }

    private void clearForm() {
        table.clearSelection();
        code.setText(""); name.setText(""); description.setText("");
    }
}
