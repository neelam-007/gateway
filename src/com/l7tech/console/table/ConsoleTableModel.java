package com.l7tech.console.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import org.apache.log4j.Category;

/**
 * ConsoleTableModel - implemented as a List of Lists; that is, each row is a List
 */
public class ConsoleTableModel extends AbstractTableModel {
    public ConsoleTableModel(String[] columnNames, Comparator comparator) {
        this.columnNames = columnNames;
        this.comparator = comparator;
    }

    /** Clears the table data */
    public void clear() {
        data.clear();
        fireTableDataChanged();
    }

    /** Returns indication if the given row is contained in the table */
    public boolean contains(List row) {
        return data.contains(row);
    }

    /** Returns the index of the given row or -1 if not found */
    public int indexOf(List row) {
        return data.indexOf(row);
    }

    /** Returns the row at the specified index */
    public List getRow(int rowIndex) {
        return (List)data.get(rowIndex);
    }

    /** Adds the given row to the table */
    public void addRow(List row) {
        int startRow = data.size();
        data.add(row);
        fireTableRowsInserted(startRow, startRow);
    }

    /** Adds the given list of rows to the table */
    public void addRows(List rows) {
        int startRow = data.size();
        data.addAll(rows);
        fireTableRowsInserted(startRow, data.size() - 1);
    }

    /** Removes the specified row */
    public void removeRow(int rowIndex) {
        data.remove(rowIndex);
        fireTableRowsDeleted(rowIndex, rowIndex);
    }

    /** Removes the given array of rowIndices */
    public void removeRows(int[] rowIndices) {
        for (int i = 0; i < rowIndices.length; i++) {
            removeRow(rowIndices[i]);
        }
        fireTableDataChanged();
    }

    /** Removes the given List of rows */
    public void removeRows(List rows) {
        Iterator iter = rows.iterator();
        while (iter.hasNext()) {
            List row = (List)iter.next();
            int index = getRowIndex(row);
            if (-1 == index) {
                data.remove(row); // hope for the best
            } else {
                data.remove(index);
            }
        }
        fireTableDataChanged();
    }

    /** Uses comparator to figure out which row corresponds to the given list;
     returns -1 if row not found */
    private int getRowIndex(List row) {
        if (null == comparator) {
            return -1;
        }

        // For each row
        for (int i = 0; i < data.size(); i++) {
            if (0 == comparator.compare(data.get(i), row)) {
                return i;
            }
        }

        return -1;
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public String getColumnName(int colIndex) {
        return columnNames[colIndex];
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    /*
     * JTable uses this method to determine the default renderer/
     * editor for each cell.
     * If we didn't implement this method,
     * then the last column would contain text ("true"/"false"),
     * rather than a check box for example.
     */
    public Class getColumnClass(int colIndex) {
        return getValueAt(0, colIndex).getClass();
    }

    public int getRowCount() {
        return data.size();
    }

    public boolean isCellEditable(int rowIndex, int colIndex) {
        // This is a non-editable table - always return false
        return false;
    }

    public Object getValueAt(int rowIndex, int colIndex) {
        List row = (List)data.get(rowIndex);
        return row.get(colIndex);
    }

    public void setValueAt(Object value, int rowIndex, int colIndex) {
        List row = (List)data.get(rowIndex);
        row.set(colIndex, value);
        fireTableCellUpdated(rowIndex, colIndex);
    }

    /** Returns the underlying List of rows (Lists) */
    public List asList() {
        return data;
    }

    public Comparator getComparator() {
        return comparator;
    }

    // Data model
    private String[] columnNames;
    private Comparator comparator;
    private final Vector data = new Vector(); // List of rows (Lists) - need to be synchronized
}
