package com.l7tech.console.table;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.JTableHeader;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class MimePartsTable extends JTable {

    private MimePartsTableSorter tableSorter = null;

    public MimePartsTable() {

        setModel(getMimePartsTableModel());
        getTableHeader().setReorderingAllowed(false);
        getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        addMouseListenerToHeaderInTable();
    }

    /**
     * Get table sorter which is the table model being used in this table.
     *
     * @return MimePartsTableSorter  The table model with column sorting.
     */
    public MimePartsTableSorter getTableSorter() {
        return tableSorter;
    }

    public void removeAll() {
        super.removeAll();        
        tableSorter.setData(new Vector());
    }

    /**
     * Return MimePartsTableSorter property value
     *
     * @return MimePartsTableSorter
     */
    private MimePartsTableSorter getMimePartsTableModel() {

        if (tableSorter == null) {
            Object[][] rows = new Object[][]{};

            String[] cols = new String[]{
                "Parameter Name", "MIME Part Content Type", "MIME Part Length Max."
            };

            tableSorter = new MimePartsTableSorter(new DefaultTableModel(rows, cols) {
                public boolean isCellEditable(int row, int col) {
                    if(col == MimePartsTableSorter.MIME_PART_TABLE_MAX_LENGTH_COLUMN_INDEX)
                        return true;
                    else
                        return false;
                }
            });
        }

        return tableSorter;
    }

    /**
     * Add a mouse listener to the Table to trigger a table sort
     * when a column heading is clicked in the JTable.
     */
    private void addMouseListenerToHeaderInTable() {

        final JTable tableView = this;
        tableView.setColumnSelectionAllowed(false);
        MouseAdapter listMouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                TableColumnModel columnModel = tableView.getColumnModel();
                int viewColumn = columnModel.getColumnIndexAtX(e.getX());
                int column = tableView.convertColumnIndexToModel(viewColumn);
                if (e.getClickCount() == 1 && column != -1) {

                    ((MimePartsTableSorter) tableView.getModel()).sortData(column, true);
                    ((MimePartsTableSorter) tableView.getModel()).fireTableDataChanged();
                    tableView.getTableHeader().resizeAndRepaint();
                }
            }
        };
        JTableHeader th = tableView.getTableHeader();
        th.addMouseListener(listMouseListener);
    }
}