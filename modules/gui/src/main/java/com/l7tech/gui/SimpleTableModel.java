package com.l7tech.gui;

import com.l7tech.util.Functions;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A simplified table model backed by a simple list of objects of a certain type {@link RT}, where
 * each row knows how to find a certain field of RT.
 */
public class SimpleTableModel<RT> extends AbstractTableModel {
    private final List<RT> rows = new ArrayList<RT>();
    private final List<SimpleColumn<RT>> columns = new ArrayList<SimpleColumn<RT>>();

    public SimpleTableModel() {
    }

    public void setRows(List<RT> rows) {
        this.rows.clear();
        this.rows.addAll(rows);
        fireTableDataChanged();
    }

    public void setColumns(List<SimpleColumn<RT>> columns) {
        this.columns.clear();
        this.columns.addAll(columns);
        fireTableStructureChanged();
    }

    public int getRowCount() {
        return rows.size();
    }

    public int getColumnCount() {
        return columns.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return columns.get(columnIndex).getValue(rows.get(rowIndex), rowIndex, columnIndex);
    }

    public String getColumnName(int column) {
        return columns.get(column).getName();
    }

    /**
     * Get the backing object for the specified row.
     *
     * @param rowIndex the row index
     * @return the backing object for that row
     */
    public RT getRowObject(int rowIndex) {
        return rows.get(rowIndex);
    }

    /**
     * Find the lowest row index that matches the specified predicate.
     *
     * @param predicate a Unary that, when invoked on an instance of {@link RT}, returns true
     *                  iff. that instance passes the desired test(s).
     * @return the index of the lowest row that matched, or -1 if no row matched.
     */
    public int findFirstRow(Functions.Unary<Boolean, RT> predicate) {
        for (int i = 0; i < rows.size(); i++) {
            RT row = rows.get(i);
            if (predicate.call(row))
                return i;
        }
        return -1;
    }

    /**
     * Find all row indexes that match the specified predicate.
     *
     * @param predicate a Unary that, when invoked on an instance of {@link RT}, returns true
     *                  iff. that instance passes the desired test(s).
     * @return the indexes of the every row that matched.  May be empty if no row matched.
     */
    public List<Integer> findRows(Functions.Unary<Boolean, RT> predicate) {
        List<Integer> ret = new ArrayList<Integer>();
        for (int i = 0; i < rows.size(); i++) {
            RT row = rows.get(i);
            if (predicate.call(row))
                ret.add(i);
        }
        return ret;
    }
    
    /**
     * Provide direct, read-only access to the row list.
     *
     * @return a read-only view of the row list.
     */
    public List<RT> getRows() {
        return Collections.unmodifiableList(rows);
    }
}
