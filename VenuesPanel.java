package com.campus.examscheduler.ui;

import com.campus.examscheduler.model.Venue;
import com.campus.examscheduler.service.ValidationException;
import com.campus.examscheduler.service.VenueService;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/** Venue management. */
public class VenuesPanel extends Page {
    private final SimpleTableModel model = new SimpleTableModel("Venue", "Capacity", "Type", "Status", "Notes");
    private final JTable table = Ui.table(model);
    private final JLabel count = Ui.muted(" ");
    private List<Venue> venues = new ArrayList<>();

    public VenuesPanel() {
        super("Examination Venues", "Venues, capacities and availability. Disabled venues are never used by the scheduler.");
        Ui.widths(table, 200, 80, 140, 90, 300);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.add(Ui.button("Add venue", AppButton.Kind.PRIMARY, this::add));
        bar.add(Ui.button("Edit", AppButton.Kind.SECONDARY, this::edit));
        bar.add(Ui.button("Enable / disable", AppButton.Kind.SECONDARY, this::toggle));
        bar.add(Ui.button("Delete", AppButton.Kind.DANGER, this::delete));
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10), 16);
        card.add(bar, BorderLayout.NORTH);
        card.add(Ui.scroll(table), BorderLayout.CENTER);
        card.add(count, BorderLayout.SOUTH);
        setBody(card);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { if (e.getClickCount() == 2) edit(); }
        });
    }

    @Override public void refresh() {
        venues = VenueService.list();
        List<Object[]> rows = new ArrayList<>();
        for (Venue v : venues) rows.add(new Object[]{v.getName(), v.getCapacity(), v.getType(), v.isAvailable() ? "Available" : "Disabled", v.getNotes()});
        model.setRows(rows);
        long ok = venues.stream().filter(Venue::isAvailable).count();
        count.setText(venues.size() + " venue(s), " + ok + " available");
    }

    private Venue selected() {
        int row = Ui.selectedModelRow(table);
        if (row < 0) throw new ValidationException("Select a venue in the table first.");
        return venues.get(row);
    }

    private void add() {
        VenueDialog d = new VenueDialog(this, null);
        d.setVisible(true);
        if (d.isSaved()) refresh();
    }

    private void edit() {
        VenueDialog d = new VenueDialog(this, selected());
        d.setVisible(true);
        if (d.isSaved()) refresh();
    }

    private void toggle() {
        Venue v = selected();
        v.setAvailable(!v.isAvailable());
        VenueService.save(v);
        refresh();
    }

    private void delete() {
        Venue v = selected();
        if (!Ui.confirm(this, "Delete venue", "Delete venue '" + v.getName() + "'?")) return;
        VenueService.delete(v.getId());
        refresh();
    }
}
