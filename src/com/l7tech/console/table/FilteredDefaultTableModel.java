package com.l7tech.console.table;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.TableModelListener;

/*
 * This class encapsulates the table model with filtering support.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
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
    public void addTableModelListener(TableModelListener l){
        realModel.addTableModelListener(l);
    }

    public TableModelListener getTableModelListener(){
        return getTableModelListener();
    }

    public void fireTableDataChanged(){
        realModel.fireTableDataChanged();
    }

    public boolean isCellEditable(int rowIndex, int columnIndex){
        return realModel.isCellEditable(rowIndex, columnIndex);
    }

    public Object getValueAt(int rowIndex, int columnIndex){
        return realModel.getValueAt(rowIndex, columnIndex);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex){
        realModel.setValueAt(aValue, rowIndex, columnIndex);
    }

    public int getRowCount(){
         return realModel.getRowCount();
     }

    public int getColumnCount(){
        return realModel.getColumnCount();
    }

    public String getColumnName(int columnIndex){
        return realModel.getColumnName(columnIndex);
    }

}
