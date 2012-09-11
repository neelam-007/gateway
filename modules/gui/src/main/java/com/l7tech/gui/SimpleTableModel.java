package com.l7tech.gui;

import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

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

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return columns.get(columnIndex).getValue(rows.get(rowIndex), rowIndex, columnIndex);
    }

    @Override
    public String getColumnName(int column) {
        return columns.get(column).getName();
    }

    @Override
    public Class<?> getColumnClass( final int columnIndex ) {
        Class<?> columnClass = columns.get(columnIndex).getColumnClass();
        if ( columnClass == null ) {
             columnClass = super.getColumnClass( columnIndex );            
        }
        return columnClass;
    }

    /**
     * Get the backing object for the specified row.
     *
     * @param rowIndex the row index.  If negative, this method returns null.
     * @return the backing object for that row, or null.
     */
    public RT getRowObject(int rowIndex) {
        return rowIndex < 0 ? null : rows.get(rowIndex);
    }

    /**
     * Set the backing object for the specified row.
     *
     * @param rowIndex the row index.  Must be nonnegative.
     * @param newValue the new backing object for the row.  Required.
     */
    public void setRowObject(int rowIndex, @NotNull RT newValue) {
        rows.set(rowIndex, newValue);
        fireTableRowsUpdated(rowIndex, rowIndex);
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

    public void addRow( final RT row ) {
        rows.add( row );
        fireTableRowsInserted( rows.size()-1, rows.size()-1 );
    }

    public void addRow( int index, final RT row ) {
        rows.add(index, row );
        fireTableRowsInserted( index, index );
    }

    /**
     * Get the row index of the specified object.
     *
     * @param row The row item
     * @return The index or -1 if the item is not found.
     */
    public int getRowIndex( final RT row ) {
        return  rows.indexOf( row );
    }

    /**
     * Remove a row from the table.
     * <p/>
     * This method is safe to call with out-of-range or negative row indices.
     *
     * @param modelRowIndex row number to delete.  If this is negative, or greater than or equal to the row count, this method does nothing.
     */
    public void removeRowAt( final int modelRowIndex ) {
        if ( modelRowIndex > -1 && modelRowIndex < rows.size() ) {
            rows.remove( modelRowIndex );
            fireTableRowsDeleted( modelRowIndex, modelRowIndex );
        }
    }

    public void removeRow( final RT row ) {
        removeRowAt( rows.indexOf( row ) );
    }

    /**
     * Swap the position of two rows in the table.
     * <p/>
     * This method is safe to call with out-of-range (or negative) row indices.
     * @param r1 first row index to swap.  If this is negative, or greater than or equal to the row count, this method returns false.
     * @param r2 second row index to swap.  If this is negative, greater than or equal to the row count, or the same as r1, this method returns false.
     * @return true if rows were swapped successfully. Swapping a row with itself is NOT considered successful.
     */
    public boolean swapRows(int r1, int r2) {
        if (r1 < 0 || r2 < 0 || r1 == r2)
            return false;
        
        int rowCount = getRowCount();
        if (r1 >= rowCount || r2 >= rowCount)
            return false;

        RT rv1 = rows.get(r1);
        RT rv2 = rows.get(r2);
        rows.set(r1, rv2);
        rows.set(r2, rv1);
        fireTableRowsUpdated(r1, r1);
        fireTableRowsUpdated(r2, r2);
        return true;
    }
}
