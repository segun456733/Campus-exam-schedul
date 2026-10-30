package com.campus.examscheduler.ui;

import com.campus.examscheduler.model.Department;
import com.campus.examscheduler.model.Venue;
import com.campus.examscheduler.service.DepartmentService;
import com.campus.examscheduler.service.ExportService;
import com.campus.examscheduler.service.ReportService;
import com.campus.examscheduler.service.ReportTable;
import com.campus.examscheduler.service.ValidationException;
import com.campus.examscheduler.service.VenueService;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.print.PrinterException;
import java.io.File;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/** Reports: view, export to CSV/PDF, print. */
public class ReportsPanel extends Page {
    private final JComboBox<ReportService.Type> type = new JComboBox<>(ReportService.Type.values());
    private final JComboBox<Object> parameter = new JComboBox<>();
    private final JLabel info = Ui.muted("Choose a report and press Show report.");
    private final JScrollPane scroll = new JScrollPane();
    private JTable table;
    private ReportTable current;

    public ReportsPanel() {
        super("Reports", "View, export (CSV, PDF) and print timetables and validation reports");
        scroll.setBorder(javax.swing.BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(java.awt.Color.WHITE);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.add(Ui.fieldLabel("Report"));
        bar.add(type);
        bar.add(parameter);
        bar.add(Ui.button("Show report", AppButton.Kind.PRIMARY, this::showReport));
        JPanel bar2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar2.setOpaque(false);
        bar2.add(Ui.button("Export CSV", AppButton.Kind.SECONDARY, () -> export(true)));
        bar2.add(Ui.button("Export PDF", AppButton.Kind.SECONDARY, () -> export(false)));
        bar2.add(Ui.button("Print", AppButton.Kind.SECONDARY, this::print));

        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.setOpaque(false);
        north.add(bar, BorderLayout.NORTH);
        north.add(bar2, BorderLayout.SOUTH);
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10), 16);
        card.add(north, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(info, BorderLayout.SOUTH);
        setBody(card);

        type.addActionListener(e -> updateParameter());
        updateParameter();
    }

    @Override public void refresh() {
        updateParameter();
        if (current != null) showReport();
    }

    private void updateParameter() {
        ReportService.Type t = (ReportService.Type) type.getSelectedItem();
        parameter.removeAllItems();
        parameter.setVisible(t == ReportService.Type.DEPARTMENT || t == ReportService.Type.VENUE);
        if (t == ReportService.Type.DEPARTMENT) for (Department d : DepartmentService.list()) parameter.addItem(d.getName());
        if (t == ReportService.Type.VENUE) for (Venue v : VenueService.list()) parameter.addItem(v.getName());
    }

    private void showReport() {
        ReportService.Type t = (ReportService.Type) type.getSelectedItem();
        String p = (String) parameter.getSelectedItem();
        if ((t == ReportService.Type.DEPARTMENT || t == ReportService.Type.VENUE) && p == null)
            throw new ValidationException("Choose a " + (t == ReportService.Type.DEPARTMENT ? "department" : "venue") + " first.");
        current = ReportService.build(t, p);
        SimpleTableModel model = new SimpleTableModel(current.columns());
        List<Object[]> rows = new ArrayList<>();
        for (String[] r : current.rows()) rows.add(r.clone());
        model.setRows(rows);
        table = Ui.table(model);
        scroll.setViewportView(table);
        info.setText(current.title() + " - " + current.subtitle());
    }

    private ReportTable requireReport() {
        if (current == null) throw new ValidationException("Press 'Show report' first.");
        return current;
    }

    private void export(boolean csv) {
        ReportTable r = requireReport();
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(csv ? "Export CSV" : "Export PDF");
        String ext = csv ? "csv" : "pdf";
        fc.setFileFilter(new FileNameExtensionFilter(csv ? "CSV files (*.csv)" : "PDF files (*.pdf)", ext));
        fc.setSelectedFile(new File(r.title().replaceAll("[^A-Za-z0-9]+", "_") + "." + ext));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith("." + ext)) f = new File(f.getParentFile(), f.getName() + "." + ext);
        if (f.exists() && !Ui.confirm(this, "Overwrite file", f.getName() + " already exists. Replace it?")) return;
        Path path = f.toPath();
        if (csv) ExportService.exportCsv(r, path); else ExportService.exportPdf(r, path);
        Ui.info(this, "Saved " + path);
    }

    private void print() {
        ReportTable r = requireReport();
        HashPrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
        attrs.add(OrientationRequested.LANDSCAPE);
        try {
            table.print(JTable.PrintMode.FIT_WIDTH, new MessageFormat(r.title()), new MessageFormat("Page {0}"), true, attrs, true);
        } catch (PrinterException e) {
            throw new ValidationException("Printing failed: " + e.getMessage());
        }
    }
}
