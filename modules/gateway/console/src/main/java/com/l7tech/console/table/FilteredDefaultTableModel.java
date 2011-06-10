package com.l7tech.console.table;

import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;

/*
 * This class encapsulates the table model with filtering support.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * Warning: This class does not completely delegate to realModel for all model methods. This may or may not have been
 * for a specific reason. e.g. only methods required were delegated.
 *
 * $Id$
 */


public abstract class FilteredDefaultTableModel extends AbstractTableModel {

    // Storage of reference to model being filtered
    protected DefaultTableModel realModel = null;

    public DefaultTableModel getRealModel(){
        return realModel;
    }

    public void setRealModel(DefaultTableModel tableModel){
        this.realModel = tableModel;
    }
    @Override
    public void addTableModelListener(TableModelListener l){
        realModel.addTableModelListener(l);
    }

    @Override
    public void fireTableDataChanged(){
        realModel.fireTableDataChanged();
    }

    @Override
    public void fireTableRowsUpdated(int firstRow, int lastRow) {
        realModel.fireTableRowsUpdated(firstRow, lastRow);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex){
        return realModel.isCellEditable(rowIndex, columnIndex);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex){
        return realModel.getValueAt(rowIndex, columnIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex){
        realModel.setValueAt(aValue, rowIndex, columnIndex);
    }

    @Override
    public int getRowCount(){
         return realModel.getRowCount();
     }

    @Override
    public int getColumnCount(){
        return realModel.getColumnCount();
    }

    @Override
    public String getColumnName(int columnIndex){
        return realModel.getColumnName(columnIndex);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return realModel.getColumnClass(columnIndex);
    }
}
