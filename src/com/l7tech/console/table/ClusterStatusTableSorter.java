package com.l7tech.console.table;

import com.l7tech.cluster.GatewayStatus;
import com.l7tech.console.ClusterStatusWindow;

import javax.swing.table.DefaultTableModel;
import java.util.logging.Logger;
import java.util.Vector;
import java.util.Arrays;
import java.util.Comparator;

/*
 * This class encapsulates the data model for the cluster status table with column sorting capability.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class ClusterStatusTableSorter extends FilteredDefaultTableModel {
    static Logger logger = Logger.getLogger(ClusterStatusTableSorter.class.getName());
    private boolean ascending = true;
    private int columnToSort = 1;
    private Vector data = new Vector();
    private Object[] sortedData = null;

    /**
     * Constructor taking <CODE>DefaultTableModel</CODE> as the input parameter.
     *
     * @param model  A table model.
     */
    public ClusterStatusTableSorter(DefaultTableModel model) {
        setRealModel(model);
    }

    /**
     * Set the data object.
     *
     * @param data  The list of the node status of every gateways in the cluster (unsorted).
     */
    public void setData(Vector data) {
        this.data = data;
        sortData(columnToSort, false);
    }

    /**
     * Return the column index of the sorted column.
     *
     * @return int  The column index of the sorted column.
     */
    public int getSortedColumn(){
        return columnToSort;
    }

    /**
     * The sorting order.
     *
     * @return boolean  true if the sorting is in ascending order, false otherwise.
     */
    public boolean isAscending(){
        return ascending;
    }

    /**
     * Perform the data sorting.
     *
     * @param column  The index of the table column to be sorted.
     * @param orderToggle  true if the sorting order is toggled, false otherwise.
     */
    public void sortData(int column, boolean orderToggle) {

        if(orderToggle){
            ascending = ascending ? false : true;
        }

        // always sort in ascending order if the user select a new column
        if(column != columnToSort){
            ascending = true;
        }
        // save the column index
        columnToSort = column;

        Object[] sorted = data.toArray();
        Arrays.sort(sorted, new ClusterStatusTableSorter.ColumnSorter(columnToSort, ascending));
        sortedData = sorted;
    }

    /**
     * Return the value of the table cell specified with its tbale coordinate.
     *
     * @param row  The row index.
     * @param col  The column index.
     * @return Object  The value at the specified table coordinate.
     */
    public Object getValueAt(int row, int col) {
        switch (col) {
            case ClusterStatusWindow.STATUS_TABLE_NODE_STATUS_COLUMN_INDEX:
                return new Integer(((GatewayStatus) sortedData[row]).getStatus());
            case ClusterStatusWindow.STATUS_TABLE_NODE_NAME_COLUMN_INDEX:
                return ((GatewayStatus) sortedData[row]).getName();
            case ClusterStatusWindow.STATUS_TABLE_LOAD_SHARING_COLUMN_INDEX:
                return new Integer(((GatewayStatus) sortedData[row]).getLoadSharing());
            case ClusterStatusWindow.STATUS_TABLE_REQUEST_ROUTED_COLUMN_INDEX:
                return new Integer(((GatewayStatus) sortedData[row]).getRequestRouted());
            case ClusterStatusWindow.STATUS_TABLE_LOAD_AVERAGE_COLUMN_INDEX:
                return new Double(((GatewayStatus) sortedData[row]).getAvgLoad());
            case ClusterStatusWindow.STATUS_TABLE_SERVER_UPTIME_COLUMN_INDEX:
                // if the node is down, the uptime retrived from the node is outdated and it should be set to zero
                if(((GatewayStatus) sortedData[row]).getStatus() == 0 ) {
                    return new Long(0);
                }
                else{
                    return new Long(((GatewayStatus) sortedData[row]).getUptime());
                }
            case ClusterStatusWindow.STATUS_TABLE_IP_ADDDRESS_COLUMN_INDEX:
                return ((GatewayStatus) sortedData[row]).getAddress();
            case ClusterStatusWindow.STATUS_TABLE_NODE_ID_COLUMN_INDEX:
                return ((GatewayStatus) sortedData[row]).getNodeId();
            default:
                throw new IllegalArgumentException("Bad Column");
        }
    }

    /**
     * A class for determining the order of two objects by comparing their values.
     */
    public class ColumnSorter implements Comparator {
        private boolean ascending;
        private int column;

        /**
         * Constructor
         *
         * @param column  The index of the table column on which the objects are sorted.
         * @param ascending  true if the sorting order is ascending, false otherwise.
         */
        ColumnSorter(int column, boolean ascending) {
            this.ascending = ascending;
            this.column = column;
        }

        /**
         * Compare the order of the two objects. A negative integer, zero, or a positive integer
         * as the the specified String is greater than, equal to, or less than this String,
         * ignoring case considerations.
         *
         * @param a  One of the two objects to be compared.
         * @param b  The other one of the two objects to be compared.
         * @return   -1 if a > b, 0 if a = b, and 1 if a < b.
         */
        public int compare(Object a, Object b) {

            Object elementA = new Object();
            Object elementB = new Object();

            switch (column) {
                case ClusterStatusWindow.STATUS_TABLE_NODE_STATUS_COLUMN_INDEX:
                    elementA = new Integer(((GatewayStatus) a).getStatus());
                    elementB = new Integer(((GatewayStatus) b).getStatus());
                    break;
                case ClusterStatusWindow.STATUS_TABLE_NODE_NAME_COLUMN_INDEX:
                    elementA = ((GatewayStatus) a).getName();
                    elementB = ((GatewayStatus) b).getName();
                    break;
                case ClusterStatusWindow.STATUS_TABLE_LOAD_SHARING_COLUMN_INDEX:
                    elementA = new Long(((GatewayStatus) a).getLoadSharing());
                    elementB = new Long(((GatewayStatus) b).getLoadSharing());
                    break;
                case ClusterStatusWindow.STATUS_TABLE_REQUEST_ROUTED_COLUMN_INDEX:
                    elementA = new Long(((GatewayStatus) a).getRequestRouted());
                    elementB = new Long(((GatewayStatus) b).getRequestRouted());
                    break;
                case ClusterStatusWindow.STATUS_TABLE_LOAD_AVERAGE_COLUMN_INDEX:
                    elementA = new Double(((GatewayStatus) a).getAvgLoad());
                    elementB = new Double(((GatewayStatus) b).getAvgLoad());
                    break;
                case ClusterStatusWindow.STATUS_TABLE_SERVER_UPTIME_COLUMN_INDEX:
                    elementA = new Long(((GatewayStatus) a).getUptime());
                    elementB = new Long(((GatewayStatus) b).getUptime());
                    break;
                case ClusterStatusWindow.STATUS_TABLE_IP_ADDDRESS_COLUMN_INDEX:
                    elementA = ((GatewayStatus) a).getAddress();
                    elementB = ((GatewayStatus) b).getAddress();
                    break;
                case ClusterStatusWindow.STATUS_TABLE_NODE_ID_COLUMN_INDEX:
                    elementA = ((GatewayStatus) a).getNodeId();
                    elementB = ((GatewayStatus) b).getNodeId();
                    break;
                default:
                    logger.warning("Bad Cluster Status Table Column: " + column);
                    break;
            }

            // Treat empty strains like nulls
            if (elementA instanceof String && ((String)elementA).length() == 0) {
                elementA = null;
            }
            if (elementB instanceof String && ((String)elementB).length() == 0) {
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

                    if(elementA instanceof Long)
                        return ((Long) elementA).longValue() > ((Long) elementB).longValue()?1:0;
                    else if(elementA instanceof Double)
                        return ((Double) elementA).doubleValue() > ((Double) elementB).doubleValue()?1:0;
                    else
                        return ((String)elementA).compareToIgnoreCase((String)elementB);
                } else {
                    if (elementA instanceof Long)
                        return ((Long) elementB).longValue() > ((Long) elementA).longValue()?1:0;
                    else if(elementA instanceof Double)
                        return ((Double) elementB).doubleValue() > ((Double) elementA).doubleValue()?1:0;
                    else
                        return ((String)elementB).compareToIgnoreCase((String)elementA);

                }
            }
        }
    }

}


