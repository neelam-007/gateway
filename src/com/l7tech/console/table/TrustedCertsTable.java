package com.l7tech.console.table;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class TrustedCertsTable extends JTable {

    public TrustedCertsTable() {

        setModel(getTrustedCertTableModel());
        getTableHeader().setReorderingAllowed(false);
        getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Hide the cert expiration data column
        getColumnModel().getColumn(TrustedCertTableSorter.CERT_TABLE_CERT_USAGE_COLUMN_INDEX).setMinWidth(0);
        getColumnModel().getColumn(TrustedCertTableSorter.CERT_TABLE_CERT_USAGE_COLUMN_INDEX).setMaxWidth(0);
        getColumnModel().getColumn(TrustedCertTableSorter.CERT_TABLE_CERT_USAGE_COLUMN_INDEX).setPreferredWidth(0);

        addMouseListenerToHeaderInTable();
    }

    /**
       * Return TrustedCertTableSorter property value
       *
       * @return TrustedCertTableSorter
       */
      private TrustedCertTableSorter getTrustedCertTableModel() {

          Object[][] rows = new Object[][]{};

          String[] cols = new String[]{
              "Name", "Issued by", "Expiration Date", "Usage"
          };

          TrustedCertTableSorter trustedCertTableSorter = new TrustedCertTableSorter(new DefaultTableModel(rows, cols) {
              public boolean isCellEditable(int row, int col) {
                  // the table cells are not editable
                  return false;
              }
          });

          return trustedCertTableSorter;

      }

    /**
     *  Add a mouse listener to the Table to trigger a table sort
     *  when a column heading is clicked in the JTable.
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
