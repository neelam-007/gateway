package com.l7tech.console.table;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class TrustedCertsTable extends JTable {

    private TrustedCertTableSorter tableSorter = null;

    public TrustedCertsTable() {

        setModel(getTrustedCertTableModel());
        getTableHeader().setReorderingAllowed(false);
        getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        addMouseListenerToHeaderInTable();
    }

    /**
     * Hide the colunm
     *
     * @param columnIndex The index of the column being hiden.
     */
    public void hideColumn(int columnIndex) {
        if (columnIndex <= getColumnCount()) {

            getColumnModel().getColumn(columnIndex).setMinWidth(0);
            getColumnModel().getColumn(columnIndex).setMaxWidth(0);
            getColumnModel().getColumn(columnIndex).setPreferredWidth(0);
        }
    }

    /**
     * Get table sorter which is the table model being used in this table.
     *
     * @return TrustedCertTableSorter  The table model with column sorting.
     */
    public TrustedCertTableSorter getTableSorter() {
        return tableSorter;
    }

    /**
     * Return TrustedCertTableSorter property value
     *
     * @return TrustedCertTableSorter
     */
    private TrustedCertTableSorter getTrustedCertTableModel() {

        if (tableSorter == null) {
            Object[][] rows = new Object[][]{};

            String[] cols = new String[]{
                "Name", "Issued By", "Expiration Date", "Usage"
            };

            tableSorter = new TrustedCertTableSorter(new DefaultTableModel(rows, cols) {
                public boolean isCellEditable(int row, int col) {
                    // the table cells are not editable
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

                    ((TrustedCertTableSorter) tableView.getModel()).sortData(column, true);
                    ((TrustedCertTableSorter) tableView.getModel()).fireTableDataChanged();
                    tableView.getTableHeader().resizeAndRepaint();
                }
            }
        };
        JTableHeader th = tableView.getTableHeader();
        th.addMouseListener(listMouseListener);
    }

}
