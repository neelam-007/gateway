package com.l7tech.console.table;

import com.l7tech.logging.StatisticsRecord;
import com.l7tech.cluster.GatewayStatus;

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
    boolean ascending = true;
    int columnToSort = 1;
    int compares;
    private Vector data;
    private Object[] sortedData = null;

    public ClusterStatusTableSorter() {
    }

    public ClusterStatusTableSorter(DefaultTableModel model) {
        setModel(model);
    }

    public void setModel(DefaultTableModel model) {
        super.setRealModel(model);
    }

    public void setData(Vector data) {
        this.data = data;
        sortData(columnToSort, false);
    }

    public int getSortedColumn(){
        return columnToSort;
    }

    public boolean isAscending(){
        return ascending;
    }

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

    public Object getValueAt(int row, int col) {
        switch (col) {
            case 0:
                return new Integer(((GatewayStatus) sortedData[row]).getStatus());
            case 1:
                return ((GatewayStatus) sortedData[row]).getName();
            case 2:
                return new Integer(((GatewayStatus) sortedData[row]).getLoadSharing());
            case 3:
                return new Integer(((GatewayStatus) sortedData[row]).getRequestFailure());
            case 4:
                return new Double(((GatewayStatus) sortedData[row]).getLoadAvg());
            case 5:
                return new Long(((GatewayStatus) sortedData[row]).getUptime());
            case 6:
                return ((GatewayStatus) sortedData[row]).getIpAddress();
            default:
                throw new IllegalArgumentException("Bad Column");
        }
    }

    public class ColumnSorter implements Comparator {
        private boolean ascending;
        private int column;

        ColumnSorter(int column, boolean ascending) {
            this.ascending = ascending;
            this.column = column;
        }

        public int compare(Object a, Object b) {

            Object elementA = new Object();
            Object elementB = new Object();

            switch (column) {
                case 0:
                    elementA = new Integer(((GatewayStatus) a).getStatus());
                    elementB = new Integer(((GatewayStatus) b).getStatus());
                    break;
                case 1:
                    elementA = ((GatewayStatus) a).getName();
                    elementB = ((GatewayStatus) b).getName();
                    break;
                case 2:
                    elementA = new Long(((GatewayStatus) a).getLoadSharing());
                    elementB = new Long(((GatewayStatus) b).getLoadSharing());
                    break;
                case 3:
                    elementA = new Long(((GatewayStatus) a).getRequestFailure());
                    elementB = new Long(((GatewayStatus) b).getRequestFailure());
                    break;
                case 4:
                    elementA = new Double(((GatewayStatus) a).getLoadAvg());
                    elementB = new Double(((GatewayStatus) b).getLoadAvg());
                    break;
                case 5:
                    elementA = new Long(((GatewayStatus) a).getUptime());
                    elementB = new Long(((GatewayStatus) b).getUptime());
                    break;
                 case 6:
                    elementA = ((GatewayStatus) a).getIpAddress();
                    elementB = ((GatewayStatus) b).getIpAddress();
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


