package com.campus.examscheduler.ui;

import com.campus.examscheduler.model.Venue;
import com.campus.examscheduler.service.VenueService;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

/** Add / edit one venue. */
public class VenueDialog extends JDialog {
    private final Venue existing;
    private final JTextField name = Ui.textField(20);
    private final JSpinner capacity = new JSpinner(new SpinnerNumberModel(100, 1, 10000, 5));
    private final JTextField type = Ui.textField(20);
    private final JCheckBox available = new JCheckBox("Available for examinations", true);
    private final JTextField notes = Ui.textField(20);
    private boolean saved;

    public VenueDialog(Component owner, Venue existing) {
        super((JFrame) SwingUtilities.getWindowAncestor(owner), existing == null ? "Add venue" : "Edit venue", true);
        this.existing = existing;
        available.setOpaque(false);
        if (existing != null) {
            name.setText(existing.getName());
            capacity.setValue(existing.getCapacity());
            type.setText(existing.getType());
            available.setSelected(existing.isAvailable());
            notes.setText(existing.getNotes());
        }
        Ui.Form form = new Ui.Form();
        form.add("Venue name", name).add("Capacity (seats)", capacity).add("Type / description", type)
                .add("Status", available).add("Notes", notes);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(Ui.button("Cancel", AppButton.Kind.SECONDARY, this::dispose));
        buttons.add(Ui.button("Save venue", AppButton.Kind.PRIMARY, this::save));
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBackground(Theme.BG);
        content.setBorder(BorderFactory.createEmptyBorder(18, 22, 16, 22));
        content.add(Ui.sectionTitle(existing == null ? "New venue" : "Edit venue"), BorderLayout.NORTH);
        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        setSize(480, 380);
        setLocationRelativeTo(owner);
    }

    private void save() {
        Venue v = new Venue();
        v.setId(existing == null ? 0 : existing.getId());
        v.setName(name.getText());
        v.setCapacity((Integer) capacity.getValue());
        v.setType(type.getText());
        v.setAvailable(available.isSelected());
        v.setNotes(notes.getText());
        VenueService.save(v);
        saved = true;
        dispose();
    }

    public boolean isSaved() { return saved; }
}
