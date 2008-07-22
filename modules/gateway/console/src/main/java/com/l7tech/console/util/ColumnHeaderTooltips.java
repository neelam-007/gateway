package com.l7tech.console.util;

import javax.swing.table.TableColumn;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.*;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.HashMap;

/**
 * This class provides the support of tooltip for the table column header.
 *
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p/>
 * $Id$
 */

public class ColumnHeaderTooltips extends MouseMotionAdapter {
    // Current column whose tooltip is being displayed.
    // This variable is used to minimize the calls to setToolTipText().
    TableColumn currentCol;

    // Maps TableColumn objects to tooltips
    Map tips = new HashMap();

    // If tooltip is null, removes any tooltip text.
    public void setToolTip(TableColumn col, String tooltip) {
        if (tooltip == null) {
            tips.remove(col);
        } else {
            tips.put(col, tooltip);
        }
    }

    public void mouseMoved(MouseEvent evt) {
        TableColumn col = null;
        JTableHeader header = (JTableHeader) evt.getSource();
        JTable table = header.getTable();
        TableColumnModel colModel = table.getColumnModel();
        int vColIndex = colModel.getColumnIndexAtX(evt.getX());

        // Return if not clicked on any column header
        if (vColIndex >= 0) {
            col = colModel.getColumn(vColIndex);
        }

        if (col != currentCol) {
            header.setToolTipText((String) tips.get(col));
            currentCol = col;
        }
    }
}

