package com.l7tech.console.table;

import com.l7tech.console.util.ArrowIcon;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.*;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class AssociatedLogsTable extends JTable {

    private AssociatedLogsTableSorter associatedLogsTableModel = null;
    private Icon upArrowIcon = new ArrowIcon(0);
    private Icon downArrowIcon = new ArrowIcon(1);

    public AssociatedLogsTable() {
        setModel(getAssociatedLogsTableModel());
        getTableHeader().setReorderingAllowed(false);
        getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setColumnModel(getLogColumnModel());
        addMouseListenerToHeaderInTable();
    }

    /**
     * Get table sorter which is the table model being used in this table.
     *
     * @return AssociatedLogsTableSorter  The table model with column sorting.
     */
    public AssociatedLogsTableSorter getTableSorter() {
        return associatedLogsTableModel;
    }

    /**
     * Return LogColumnModel property value
     * @return  DefaultTableColumnModel
     */
    private DefaultTableColumnModel getLogColumnModel() {

        DefaultTableColumnModel columnModel = new DefaultTableColumnModel();

        columnModel.addColumn(new TableColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_TIMESTAMP_COLUMN_INDEX, 100));
        columnModel.addColumn(new TableColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_SECURITY_COLUMN_INDEX, 50));
        columnModel.addColumn(new TableColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_MSG_COLUMN_INDEX, 500));

        for(int i = 0; i < columnModel.getColumnCount(); i++){
            columnModel.getColumn(i).setHeaderRenderer(iconHeaderRenderer);
            columnModel.getColumn(i).setHeaderValue(getAssociatedLogsTableModel().getColumnName(i));
        }

        return columnModel;
    }


    /**
     * create the table model with log fields
     *
     * @return DefaultTableModel
     *
     */
    private AssociatedLogsTableSorter getAssociatedLogsTableModel() {
        if (associatedLogsTableModel != null) {
            return associatedLogsTableModel;
        }

        String[] cols = {"Time", "Severity", "Message"};
        String[][] rows = new String[][]{};

        associatedLogsTableModel = new AssociatedLogsTableSorter(new DefaultTableModel(rows, cols)) {
            public boolean isCellEditable(int row, int col) {
                // the table cells are not editable
                return false;
            }
        };

        return associatedLogsTableModel;
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

                    ((AssociatedLogsTableSorter) tableView.getModel()).sortData(column, true);
                    ((AssociatedLogsTableSorter) tableView.getModel()).fireTableDataChanged();
                    tableView.getTableHeader().resizeAndRepaint();
                }
            }
        };
        JTableHeader th = tableView.getTableHeader();
        th.addMouseListener(listMouseListener);
    }

    // This customized renderer can render objects of the type TextandIcon
    TableCellRenderer iconHeaderRenderer = new DefaultTableCellRenderer() {
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            // Inherit the colors and font from the header component
            if (table != null) {
                JTableHeader header = table.getTableHeader();
                if (header != null) {
                    setForeground(header.getForeground());
                    setBackground(header.getBackground());
                    setFont(header.getFont());
                    setHorizontalTextPosition(SwingConstants.LEFT);
                }
            }

            setText((String) value);

            if (getTableSorter().getSortedColumn() == column) {

                if (getTableSorter().isAscending()) {
                    setIcon(upArrowIcon);
                } else {
                    setIcon(downArrowIcon);
                }
            }
            else{
                setIcon(null);
            }

            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            setHorizontalAlignment(JLabel.CENTER);
            return this;
        }
    };

}
