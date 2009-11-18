package com.l7tech.console.table;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.JTableHeader;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * <p> Copyright (C) 2005 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class WsdlTable extends JTable {
    private WsdlTableSorter tableSorter = null;
    private boolean showWsdlURL;

    public WsdlTable(final boolean showWsdlURL) {
        this.showWsdlURL = showWsdlURL;
        setModel(getWsdlTableModel());
        getTableHeader().setReorderingAllowed(false);
        getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getColumnModel().getColumn(WsdlTableSorter.WSDL_TABLE_SERVICE_NAME_COLUMN_INDEX).setPreferredWidth(200);

        setShowWsdlURL(showWsdlURL);
        addMouseListenerToHeaderInTable();
    }

    public void setShowWsdlURL(boolean show){
        this.showWsdlURL = show;
        setModel(getWsdlTableModel());
        if (showWsdlURL) {
            getColumnModel().getColumn(WsdlTableSorter.WSDL_TABLE_WSDL_COLUMN_INDEX).setPreferredWidth(450);
        }
    }

    /**
     * Get table sorter which is the table model being used in this table.
     *
     * @return TrustedCertTableSorter  The table model with column sorting.
     */
    public WsdlTableSorter getTableSorter() {
        return tableSorter;
    }

    /**
     * Return WsdlTableSorter property value
     *
     * @return WsdlTableSorter
     */
    private WsdlTableSorter getWsdlTableModel() {

        if (tableSorter == null) {
            Object[][] rows = new Object[][]{};

            final String [] cols = new String[]{ "Business Service Name", "WSDL Location"};
            
            tableSorter = new WsdlTableSorter(new DefaultTableModel(rows, cols) {
                @Override
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
            @Override
            public void mouseClicked(MouseEvent e) {
                TableColumnModel columnModel = tableView.getColumnModel();
                int viewColumn = columnModel.getColumnIndexAtX(e.getX());
                int column = tableView.convertColumnIndexToModel(viewColumn);
                if (e.getClickCount() == 1 && column != -1) {

                    ((WsdlTableSorter) tableView.getModel()).sortData(column, true);
                    ((WsdlTableSorter) tableView.getModel()).fireTableDataChanged();
                    tableView.getTableHeader().resizeAndRepaint();
                }
            }
        };
        JTableHeader th = tableView.getTableHeader();
        th.addMouseListener(listMouseListener);
    }
}
