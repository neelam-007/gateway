package com.l7tech.console.table;

import javax.swing.table.DefaultTableModel;

/**
 * Created by IntelliJ IDEA.
 * User: fpang
 * Date: Sep 18, 2003
 * Time: 10:21:18 AM
 * To change this template use Options | File Templates.
 */
public class LogTableModel extends DefaultTableModel {

    public LogTableModel(String[][] data, String[] columnNames){
        super(data, columnNames);
    }

    public boolean isCellEditable(int row, int col){
        // the table cells are not editable
        return false;
    }

}
