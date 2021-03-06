package com.l7tech.console.table;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

/**
 * Table for MIME part settings.
 *
 * @author fpang
 */
public class MimePartsTable extends JTable {

    private MimePartsTableSorter tableSorter = null;

    public MimePartsTable(boolean enableSigning) {

        setModel(getMimePartsTableModel());
        getColumnModel().getColumn(MimePartsTableSorter.MIME_PART_TABLE_PARAM_NAME_COLUMN_INDEX).setMinWidth(50);
        getColumnModel().getColumn(MimePartsTableSorter.MIME_PART_TABLE_PARAM_NAME_COLUMN_INDEX).setPreferredWidth(80);
        getColumnModel().getColumn(MimePartsTableSorter.MIME_PART_TABLE_CONTENT_TYPE_COLUMN_INDEX).setMinWidth(100);
        getColumnModel().getColumn(MimePartsTableSorter.MIME_PART_TABLE_CONTENT_TYPE_COLUMN_INDEX).setPreferredWidth(120);
        getColumnModel().getColumn(MimePartsTableSorter.MIME_PART_TABLE_MAX_LENGTH_COLUMN_INDEX).setMinWidth(100);
        getColumnModel().getColumn(MimePartsTableSorter.MIME_PART_TABLE_MAX_LENGTH_COLUMN_INDEX).setPreferredWidth(120);
        if (enableSigning) {
            getColumnModel().getColumn(MimePartsTableSorter.MIME_PART_TABLE_REQUIRE_SIGNATURE_COLUMN_INDEX).setMinWidth(50);
            getColumnModel().getColumn(MimePartsTableSorter.MIME_PART_TABLE_REQUIRE_SIGNATURE_COLUMN_INDEX).setPreferredWidth(70);
        } else {
            getColumnModel().removeColumn(getColumnModel().getColumn(MimePartsTableSorter.MIME_PART_TABLE_REQUIRE_SIGNATURE_COLUMN_INDEX));            
        }
        getTableHeader().setReorderingAllowed(false);
        getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        TableCellEditor  cellEditor = getDefaultEditor(Integer.class);
        if(cellEditor instanceof DefaultCellEditor) {
            ((DefaultCellEditor) cellEditor).setClickCountToStart(1);
        }

        // this will force the cell editor stop editing
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

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

    public void clear() {
        TableCellEditor editor = getCellEditor();
        if (editor != null) editor.cancelCellEditing();
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
                "Parameter Name", "MIME Part Content Type", "MIME Part Length Max. (KB)", "Require Signature"
            };

            tableSorter = new MimePartsTableSorter(new DefaultTableModel(rows, cols)); 
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