/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.console.table;

import com.l7tech.uddi.WsdlPortInfo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.JTableHeader;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

public class WsdlPortTable extends JTable {

    enum WsdlPortColumn {

        NAME(0, "wsdl:port", 60) {
            @Override
            Object getValue(WsdlPortInfo portInfo) {
                return portInfo.getWsdlPortName();
            }},

        NAMESPACE(1, "Namespace", 175) {
            @Override
            Object getValue(WsdlPortInfo portInfo) {
                return portInfo.getWsdlPortBindingNamespace();
            }},

        WSDL_URL(2, "WSDL Location", 175) {
            @Override
            Object getValue(WsdlPortInfo portInfo) {
                return portInfo.getWsdlUrl();
            }},

        ENDPOINT(3, "Endpoint", 175) {
            @Override
            Object getValue(WsdlPortInfo portInfo) {
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
        private final int preferredWidth;

        abstract Object getValue(WsdlPortInfo portInfo);

        WsdlPortColumn(int position, String colName, int preferredWidth) {
            this.position = position;
            this.colName = colName;
            this.preferredWidth = preferredWidth;
        }

        public int getPosition() {
            return position;
        }

        public String getColName() {
            return colName;
        }

        public int getPreferredWidth() {
            return preferredWidth;
        }

        public static WsdlPortColumn fromPosition(int position) {
            return positionToEnum.get(position);
        }
    }

    private WsdlPortTableSorter tableSorter = null;

    public WsdlPortTable() {
        setModel(getWsdlTableModel());
        getTableHeader().setReorderingAllowed(false);
        getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        for (WsdlPortColumn col : WsdlPortColumn.values()) {
            getColumnModel().getColumn(col.getPosition()).setPreferredWidth(col.getPreferredWidth());

        }

        addMouseListenerToHeaderInTable();
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

                    ((WsdlPortTableSorter) tableView.getModel()).sortData(WsdlPortColumn.fromPosition(column), true);
                    ((WsdlPortTableSorter) tableView.getModel()).fireTableDataChanged();
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
        private Vector rawdata = new Vector();
        private Object[] sortedData = new Object[0];

        /**
         * Constructor taking <CODE>DefaultTableModel</CODE> as the input parameter.
         *
         * @param model A table model.
         */
        public WsdlPortTableSorter(DefaultTableModel model) {
            setRealModel(model);
        }

        /**
         * Return all data in the model
         *
         * @return The data in the model
         */
        public Vector getAllData() {
            return rawdata;
        }

        /**
         * Set the data object.
         *
         * @param data The list of the WSDLs found in the UDDI Registry.
         */
        public void setData(Vector data) {
            this.rawdata = data;
            sortData(columnToSort, false);
        }

        public void clearData() {
            // an empty list in the rawdata
            this.rawdata = new Vector();

            sortedData = rawdata.toArray();
            getRealModel().setRowCount(sortedData.length);
            fireTableDataChanged();
        }

        /**
         * Add a row to the table model
         *
         * @param rowData The new row to be stored.
         */
        public void addRow(Object rowData) {
            this.rawdata.add(rowData);
            sortData(columnToSort, false);
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
                ascending = ascending ? false : true;
            }

            // always sort in ascending order if the user select a new column
            if (column != columnToSort) {
                ascending = true;
            }
            // save the column index
            columnToSort = column;

            Object[] sorted = rawdata.toArray();
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

            return WsdlPortColumn.fromPosition(col).getValue((WsdlPortInfo) sortedData[row]);
        }

        /**
         * A class for determining the order of two objects by comparing their values.
         */
        public class ColumnSorter implements Comparator {
            private boolean ascending;
            private WsdlPortColumn column;

            /**
             * Constructor
             *
             * @param column    The table column on which the objects are sorted.
             * @param ascending true if the sorting order is ascending, false otherwise.
             */
            ColumnSorter(WsdlPortColumn column, boolean ascending) {
                this.ascending = ascending;
                this.column = column;
            }

            /**
             * Compare the order of the two objects. A negative integer, zero, or a positive integer
             * as the the specified String is greater than, equal to, or less than this String,
             * ignoring case considerations.
             *
             * @param a One of the two objects to be compared.
             * @param b The other one of the two objects to be compared.
             * @return -1 if a > b, 0 if a = b, and 1 if a < b.
             */
            @Override
            public int compare(Object a, Object b) {

                Object elementA = column.getValue((WsdlPortInfo) a);
                Object elementB = column.getValue((WsdlPortInfo) b);

                // Treat empty strains like nulls
                if (elementA instanceof String && ((String) elementA).length() == 0) {
                    elementA = null;
                }
                if (elementB instanceof String && ((String) elementB).length() == 0) {
                    elementB = null;
                }

                // Sort nulls so they appear last, regardless
                // of sort order
                if (elementA == null && elementB == null) {
                    return 0;
                } else if (elementA == null) {
                    return 1;
                } else if (elementB == null) {
                    return -1;
                } else {
                    if (ascending) {
                        return ((String) elementA).compareToIgnoreCase((String) elementB);
                    } else {
                        return ((String) elementB).compareToIgnoreCase((String) elementA);
                    }
                }
            }
        }

    }

}
