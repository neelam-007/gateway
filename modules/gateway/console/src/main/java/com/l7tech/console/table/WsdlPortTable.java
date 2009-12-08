/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.console.table;

import com.l7tech.uddi.WsdlPortInfo;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.*;
import java.util.*;
import java.util.List;

public class WsdlPortTable extends JTable {

    enum WsdlPortColumn {

        NAME(0, "wsdl:port", MAX_COLUMN_SIZE) {
            @Override
            String getValue(WsdlPortInfo portInfo) {
                return portInfo.getWsdlPortName();
            }},

        NAMESPACE(1, "Namespace", MAX_COLUMN_SIZE) {
            @Override
            String getValue(WsdlPortInfo portInfo) {
                return portInfo.getWsdlPortBindingNamespace();
            }},

        WSDL_URL(2, "WSDL Location", MAX_COLUMN_SIZE) {
            @Override
            String getValue(WsdlPortInfo portInfo) {
                return portInfo.getWsdlUrl();
            }},

        ENDPOINT(3, "Endpoint", MAX_COLUMN_SIZE) {
            @Override
            String getValue(WsdlPortInfo portInfo) {
                return portInfo.getAccessPointURL();
            }};

        private static final Map<Integer,WsdlPortColumn> positionToEnum = new HashMap<Integer, WsdlPortColumn>();
        private static final List<String> colNames = new ArrayList<String>();

        static {
            for (WsdlPortColumn col : values()) {
                positionToEnum.put(col.getPosition(), col);
                colNames.add(col.getColName());
            }
        }

        private final int position;
        private final String colName;
        private final int maxWidth;

        abstract String getValue(WsdlPortInfo portInfo);

        WsdlPortColumn(int position, String colName, int preferredWidth) {
            this.position = position;
            this.colName = colName;
            this.maxWidth = preferredWidth;
        }

        public int getPosition() {
            return position;
        }

        public String getColName() {
            return colName;
        }

        public int getMaxWidth() {
            return maxWidth;
        }

        public int getValueLength(WsdlPortInfo portInfo) {
            String value = getValue(portInfo);
            return value == null || value.isEmpty() ? 0 : value.length();
        }

        public static WsdlPortColumn fromPosition(int position) {
            return positionToEnum.get(position);
        }
    }

    private static final int MAX_COLUMN_SIZE = 175;
    private WsdlPortColumn displayFullColumn = WsdlPortColumn.NAME;
    private int displayFullColumnExtraSpace = 10;
    private final Map<WsdlPortColumn, Integer> preferredSizes = new HashMap<WsdlPortColumn, Integer>();
    private final int totalScalingColumnsSize;

    private WsdlPortTableSorter tableSorter = null;

    public WsdlPortTable(WsdlPortInfo[] data) {
        setModel(getWsdlTableModel());
        getTableHeader().setReorderingAllowed(false);
        getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        getTableSorter().setData(data);

        // calculate preferred sizes
        final TableCellRenderer renderer = getTableHeader().getDefaultRenderer();
        int maxPreferredSize, preferredSize, totalRemainingSize = 0;
        for (WsdlPortColumn col : WsdlPortColumn.values()) {
            maxPreferredSize = 0;
            for (WsdlPortInfo dataRow : data) {
                preferredSize = renderer.getTableCellRendererComponent(this, col.getValue(dataRow), false, false, 0, col.getPosition() ).getPreferredSize().width;
                if (maxPreferredSize < preferredSize)
                    maxPreferredSize = preferredSize;
            }
            preferredSizes.put(col, maxPreferredSize);
            if (col != displayFullColumn)
                totalRemainingSize += maxPreferredSize;
        }
        totalScalingColumnsSize = totalRemainingSize;

        addMouseListenerToHeaderInTable();
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        final Component prepareRenderer = super.prepareRenderer(renderer, row, column);
        if (getTableHeader().getResizingColumn() != null) return prepareRenderer;
        
        final TableColumn tableColumn = getColumnModel().getColumn(column);

        int preferredWidth;
        if (WsdlPortColumn.fromPosition(column) == displayFullColumn) {
            preferredWidth = preferredSizes.get(displayFullColumn) + displayFullColumnExtraSpace;
            tableColumn.setMinWidth(preferredWidth);
        } else {
            double ratio = (double)(getTableHeader().getWidth() - preferredSizes.get(displayFullColumn) - displayFullColumnExtraSpace) / totalScalingColumnsSize;
            preferredWidth = (int)Math.round(ratio * preferredSizes.get(WsdlPortColumn.fromPosition(column)));
        }

        tableColumn.setPreferredWidth(Math.min(preferredWidth, WsdlPortColumn.fromPosition(column).getMaxWidth()));
        return prepareRenderer;
    }

    /**
     * Get table sorter which is the table model being used in this table.
     *
     * @return TrustedCertTableSorter  The table model with column sorting.
     */
    public WsdlPortTableSorter getTableSorter() {
        return tableSorter;
    }

    /**
     * Return WsdlTableSorter property value
     *
     * @return WsdlTableSorter
     */
    private WsdlPortTableSorter getWsdlTableModel() {

        if (tableSorter == null) {
            Object[][] rows = new Object[][]{};

            tableSorter = new WsdlPortTableSorter(new DefaultTableModel(rows, WsdlPortColumn.colNames.toArray()) {
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
                    WsdlPortTableSorter tableModel = (WsdlPortTableSorter) tableView.getModel();
                    tableModel.sortData(WsdlPortColumn.fromPosition(column), true);
                    tableModel.fireTableDataChanged();
                    tableView.getTableHeader().resizeAndRepaint();
                }
            }
        };
        JTableHeader th = tableView.getTableHeader();
        th.addMouseListener(listMouseListener);
    }

    public static class WsdlPortTableSorter extends FilteredDefaultTableModel {

        private boolean ascending = true;
        private WsdlPortColumn columnToSort = WsdlPortColumn.NAME;
        private WsdlPortInfo[] rawdata = new WsdlPortInfo[0];
        private WsdlPortInfo[] sortedData = new WsdlPortInfo[0];

        /**
         * Constructor taking <CODE>DefaultTableModel</CODE> as the input parameter.
         *
         * @param model A table model.
         */
        public WsdlPortTableSorter(DefaultTableModel model) {
            setRealModel(model);
        }

        /**
         * Set the data object.
         *
         * @param data The list of the WSDLs found in the UDDI Registry.
         */
        public void setData(WsdlPortInfo[] data) {
            this.rawdata = Arrays.copyOf(data, data.length);
            sortData(columnToSort, false);
        }

        public void clearData() {
            // an empty list in the rawdata
            this.rawdata = new WsdlPortInfo[0];
            this.sortedData = new WsdlPortInfo[0];
            getRealModel().setRowCount(0);
            fireTableDataChanged();
        }

        /**
         * Return the column index of the sorted column.
         *
         * @return int  The column index of the sorted column.
         */
        public WsdlPortColumn getSortedColumn() {
            return columnToSort;
        }

        public Object getData(int index) {
            if (index <= sortedData.length) {
                return sortedData[index];
            } else
                return null;
        }

        /**
         * The sorting order.
         *
         * @return boolean  true if the sorting is in ascending order, false otherwise.
         */
        public boolean isAscending() {
            return ascending;
        }

        /**
         * Perform the data sorting.
         *
         * @param column      The index of the table column to be sorted.
         * @param orderToggle true if the sorting order is toggled, false otherwise.
         */
        public void sortData(WsdlPortColumn column, boolean orderToggle) {

            if (orderToggle) {
                ascending = !ascending;
            }

            // always sort in ascending order if the user select a new column
            if (column != columnToSort) {
                ascending = true;
            }
            // save the column index
            columnToSort = column;

            WsdlPortInfo[] sorted = Arrays.copyOf(rawdata, rawdata.length);
            Arrays.sort(sorted, new WsdlPortTableSorter.ColumnSorter(columnToSort, ascending));
            sortedData = sorted;
            getRealModel().setRowCount(sortedData.length);
            fireTableDataChanged();
        }

        /**
         * Return the value of the table cell specified with its tbale coordinate.
         *
         * @param row The row index.
         * @param col The column index.
         * @return Object  The value at the specified table coordinate.
         */
        @Override
        public Object getValueAt(int row, int col) {

            if (row >= sortedData.length) {
                throw new IllegalArgumentException("Bad Row");
            }

            return WsdlPortColumn.fromPosition(col).getValue(sortedData[row]);
        }

        /**
         * A class for determining the order of two objects by comparing their values.
         */
        public class ColumnSorter implements Comparator<WsdlPortInfo> {
            private int ascending;
            private WsdlPortColumn column;

            /**
             * Constructor
             *
             * @param column    The table column on which the objects are sorted.
             * @param ascending true if the sorting order is ascending, false otherwise.
             */
            ColumnSorter(WsdlPortColumn column, boolean ascending) {
                this.ascending = ascending ? 1 : -1;
                this.column = column;
            }

            /**
             * Compares its two arguments for order.  Returns a negative integer,
             * zero, or a positive integer as the first argument is less than, equal
             * to, or greater than the second.
             *
             * @param a One of the two objects to be compared.
             * @param b The other one of the two objects to be compared.
             * @return -1 if a > b, 0 if a = b, and 1 if a < b.
             */
            @Override
            public int compare(WsdlPortInfo a, WsdlPortInfo b) {
                int elementAlength = column.getValueLength(a);
                int elementBlength = column.getValueLength(b);

                // Sort nulls and empty strings so they appear last, regardless of sort order
                return elementAlength == 0 && elementBlength == 0 ? 0:
                       elementAlength == 0 ? 1 :
                       elementBlength == 0  ? -1 :
                       ascending * column.getValue(a).compareToIgnoreCase(column.getValue(b));

            }
        }
    }
}
