package com.l7tech.console.table;

import javax.swing.table.DefaultTableModel;

/*
 * This class encapsulates the table model for log messages.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class LogTableModel extends DefaultTableModel {

    public LogTableModel(Object[][] data, String[] columnNames){
        super(data, columnNames);
    }

    public boolean isCellEditable(int row, int col){
        // the table cells are not editable
        return false;
    }

}
