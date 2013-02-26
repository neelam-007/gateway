package com.l7tech.console.table;

import com.l7tech.console.util.Registry;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.TimeUnit;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.*;
import java.util.Date;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class TrustedCertsTable extends JTable {

    private static Date clusterDate = null;
    private static long clusterDateRefresh = 0;

    private TrustedCertTableSorter tableSorter = null;

    public TrustedCertsTable() {

        setModel(getTrustedCertTableModel());
        getTableHeader().setReorderingAllowed(false);

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
                "Name", "Issued By", "Expiration Date", "SHA-1 Thumbprint", "Usage", "Subject DN"
            };

            tableSorter = new TrustedCertTableSorter(new DefaultTableModel(rows, cols) {
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

                    ((TrustedCertTableSorter) tableView.getModel()).sortData(column, true);
                    ((TrustedCertTableSorter) tableView.getModel()).fireTableDataChanged();
                    tableView.getTableHeader().resizeAndRepaint();
                }
            }
        };
        JTableHeader th = tableView.getTableHeader();
        th.addMouseListener(listMouseListener);
    }

    /**
     * Find all expired certificates and highlight them using red color.
     */
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component cell = super.prepareRenderer(renderer, row, column);
        TrustedCert trustedCert = (TrustedCert) tableSorter.getData(row);
        Date today = getClusterDate();

        if (trustedCert.getCertificate().getNotAfter().before(today)) {
            cell.setBackground(Color.RED);
        } else if (isCellSelected(row, column)) {
            cell.setBackground(selectionBackground);
        } else {
            cell.setBackground(Color.WHITE);
        }
        return cell;
    }

    private static Date getClusterDate() {
        Date date = clusterDate;
        if ( date == null || (System.currentTimeMillis() > (clusterDateRefresh + TimeUnit.MINUTES.toMillis(2)))) {
            date = Registry.getDefault().getClusterStatusAdmin().getCurrentClusterSystemTime();
            clusterDate = date;
            clusterDateRefresh = System.currentTimeMillis();
        }
        return date;
    }
}
