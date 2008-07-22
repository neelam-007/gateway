package com.l7tech.console.util;

import javax.swing.table.AbstractTableModel;
import java.util.*;

/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 *
 */
public class SortedSingleColumnTableModel extends AbstractTableModel{

    /** SortedMap that backs the list */
    SortedSet model;
    String columnName = "";
    Comparator comparator;
    int numberOfColumn = 0;

    /** default constructor */
    public SortedSingleColumnTableModel(Comparator c) {
        model = new TreeSet(c);
        comparator =  c;
    }

    public Object getValueAt(int row, int col) {

        if(col == 0) {
            return model.toArray()[row];
        }
        else {
            return null;
        }
    }

    public int getColumnCount() {
        return numberOfColumn;
    }

    public int getRowCount() {
        if(model == null) {
            return 0;
        }
        return model.size();
    }

    public void addColumn(String columnName) {
        // only allow one column in this model
        if(numberOfColumn < 1) {
            this.columnName = columnName;
            numberOfColumn++;
        }
    }

    public String getColumnName(int col){
        if(col == 0) {
            return columnName;
        }
        else{
            return null;
        }
    }

    public Object[] getDataSet(){
        return model.toArray();
    }

    public Iterator getDataIterator(){
        return model.iterator();
    }

    public void clearDataSet(){
        model = new TreeSet(comparator);
    }

    public void addRow(Object rowData){
        model.add(rowData);
    }

    public void addRows(Object[] rowsData) {

        model.addAll(toCollection(rowsData));
    }

    public void removeRows(Object[] rowsData){

        model.removeAll(toCollection(rowsData));
    }

    private Collection toCollection(Object[] array){

        Collection list = new ArrayList();
        for (int i = 0; i < array.length; i++) {
            list.add(array[i]);
        }
        return list;
    }

}
