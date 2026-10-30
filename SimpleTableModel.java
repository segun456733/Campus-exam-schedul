package com.campus.examscheduler.ui;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/** Read-only table model backed by a list of rows. */
public class SimpleTableModel extends AbstractTableModel {
    private final String[] columns;
    private final List<Object[]> rows = new ArrayList<>();

    public SimpleTableModel(String... columns) { this.columns = columns; }

    public void setRows(List<Object[]> newRows) {
        rows.clear();
        rows.addAll(newRows);
        fireTableDataChanged();
    }

    public Object[] getRow(int modelRow) { return rows.get(modelRow); }
    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return columns.length; }
    @Override public String getColumnName(int c) { return columns[c]; }
    @Override public Object getValueAt(int r, int c) { return rows.get(r)[c]; }
    @Override public boolean isCellEditable(int r, int c) { return false; }

    @Override public Class<?> getColumnClass(int c) {
        for (Object[] r : rows) if (r[c] != null) return r[c].getClass();
        return String.class;
    }
}
