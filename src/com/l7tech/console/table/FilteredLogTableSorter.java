package com.l7tech.console.table;

import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.cluster.GatewayStatus;
import com.l7tech.cluster.LogRequest;
import com.l7tech.common.util.Locator;
import com.l7tech.console.panels.LogPanel;
import com.l7tech.console.util.ClusterLogWorker;
import com.l7tech.logging.GenericLogAdmin;
import com.l7tech.logging.LogAdmin;
import com.l7tech.logging.LogMessage;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.*;
import java.util.logging.Logger;

/*
 * This class extends the <CODE>FilteredLogTableModel</CODE> class for providing the sorting functionality to the log display.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class FilteredLogTableSorter extends FilteredLogTableModel {
    static Logger logger = Logger.getLogger(StatisticsTableSorter.class.getName());
    private boolean ascending = false;
    private int columnToSort = LogPanel.LOG_TIMESTAMP_COLUMN_INDEX;
    private Object[] sortedData = null;
    private ClusterStatusAdmin clusterStatusAdmin = null;
    private GenericLogAdmin logAdmin = null;
    private final Locator logAdminLocator;
    private boolean canceled;
    private LogPanel logPanel;


    /**
     * Constructor taking <CODE>DefaultTableModel</CODE> as the input parameter.
     *
     * @param logPanel The panel of log browser.
     * @param model  A table model.
     * @param logAdminLocator A {@link Locator} that only needs to know how to locate the correct kind of {@link LogAdmin} implementation.
     */
    public FilteredLogTableSorter(LogPanel logPanel, DefaultTableModel model, Locator logAdminLocator) {
        this.logPanel = logPanel;
        this.logAdminLocator = logAdminLocator;
        setModel(model);
    }

    /**
     * Set the table model.
     *
     * @param model  The table model to be set.
     */
    public void setModel(DefaultTableModel model) {
        super.setRealModel(model);
    }

    /**
     * Return the index of the column being sorted.
     *
     * @return int  The index of the column being sorted.
     */
    public int getSortedColumn() {
        return columnToSort;
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
     * Append the new logs to the cache.
     *
     * @param nodeList  A list of node objects accompanied with their new logs.
     */
    private void appendLogs(Hashtable nodeList) {
        for (Iterator i = nodeList.keySet().iterator(); i.hasNext();) {
            String node = (String) i.next();
            Vector logs = (Vector) nodeList.get(node);
            addLogs(node, logs, false);
        }
    }

    /**
     * Add the new logs to head of the cache.
     *
     * @param nodeList  A list of node objects accompanied with their new logs.
     */
    private void addLogs(Hashtable nodeList) {
        for (Iterator i = nodeList.keySet().iterator(); i.hasNext();) {
            String node = (String) i.next();
            Vector logs = (Vector) nodeList.get(node);
            addLogs(node, logs, true);
        }
    }

   /**
     * Add the new logs to the cache. The position depends on the input parameter specified.
     *
     * @param nodeId  The Id of the node which is the source of the new logs.
     * @param newLogs  The new logs.
     */
    private void addLogs(String nodeId, Vector newLogs, boolean front) {

        // add new logs to the cache
        if (newLogs.size() > 0) {

            Object node = null;
            Vector gatewayLogs;
            if ((node = rawLogCache.get(nodeId)) != null) {
                gatewayLogs = (Vector) node;
            } else {
                // create a empty cache for the new node
                gatewayLogs = new Vector();
            }

            while (gatewayLogs.size() + newLogs.size() > MAX_NUMBER_OF_LOG_MESSGAES) {
                // remove the last element
                gatewayLogs.remove(gatewayLogs.size() - 1);
            }

            if (front) {
                // append the old logs to the cache
                newLogs.addAll(gatewayLogs);

                gatewayLogs = newLogs;
            } else {
                gatewayLogs.addAll(newLogs);
            }

            // update the logsCache
            rawLogCache.put(nodeId, gatewayLogs);

        }
    }

    /**
      * Remove the logs of the non-exist node from the cache.
      *
      * @param nodeId  The Id of the node whose logs will be removed from the cache.
      */
     private void removeLogs(String nodeId) {

         if (rawLogCache.get(nodeId) != null) {
                rawLogCache.remove(nodeId);
         }
    }

    /**
     * Apply the filter specified.
     *
     * @param newFilterLevel  The new filter applied
     */
    public void applyNewMsgFilter(int newFilterLevel) {

        filterData(newFilterLevel);
        sortData(columnToSort, false);

        realModel.fireTableDataChanged();
    }

    /**
     * Perform the data sorting.
     *
     * @param column  The index of the table column to be sorted.
     * @param orderToggle  true if the sorting order is toggled, false otherwise.
     */
    public void sortData(int column, boolean orderToggle) {

        if (orderToggle) {
            ascending = ascending ? false : true;
        }

        // always sort in ascending order if the user select a new column
        if (column != columnToSort) {
            ascending = true;
        }

        // save the column index
        columnToSort = column;

        Integer[] sorted = new Integer[filteredLogCache.size()];

        for (int i = 0; i < sorted.length; i++) {
            sorted[i] = new Integer(i);
        }

        Arrays.sort(sorted, new FilteredLogTableSorter.ColumnSorter(columnToSort, ascending));
        sortedData = sorted;

        // update the row count
        realModel.setRowCount(sortedData.length);
    }

    /**
     * Return the value of the table cell specified with its tbale coordinate.
     *
     * @param row  The row index.
     * @param col  The column index.
     * @return Object  The value at the specified table coordinate.
     */
    public Object getValueAt(int row, int col) {
        LogMessage msg = getLogMessageAtRow(row);
        switch (col) {
            case LogPanel.LOG_MSG_NUMBER_COLUMN_INDEX:
                return new Long(msg.getMsgNumber());
            case LogPanel.LOG_NODE_NAME_COLUMN_INDEX:
                return msg.getNodeName();
            case LogPanel.LOG_TIMESTAMP_COLUMN_INDEX:
                return msg.getTime();
            case LogPanel.LOG_SEVERITY_COLUMN_INDEX:
                return msg.getSeverity();
            case LogPanel.LOG_MSG_DETAILS_COLUMN_INDEX:
                return msg.getMsgDetails();
            case LogPanel.LOG_JAVA_CLASS_COLUMN_INDEX:
                return msg.getMsgClass();
            case LogPanel.LOG_JAVA_METHOD_COLUMN_INDEX:
                return msg.getMsgMethod();
            case LogPanel.LOG_REQUEST_ID_COLUMN_INDEX:
                return msg.getReqId();
            case LogPanel.LOG_NODE_ID_COLUMN_INDEX:
                return msg.getNodeId();
            default:
                throw new IllegalArgumentException("Bad Column");
        }
    }

    public LogMessage getLogMessageAtRow(int row) {
        return (LogMessage) filteredLogCache.get(((Integer) sortedData[row]).intValue());
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

            String elementA = new String("");
            String elementB = new String("");

            LogMessage logMsgA = (LogMessage) filteredLogCache.get(((Integer) a).intValue());
            LogMessage logMsgB = (LogMessage) filteredLogCache.get(((Integer) b).intValue());

            switch (column) {
                case LogPanel.LOG_MSG_NUMBER_COLUMN_INDEX:
                    elementA = Long.toString(logMsgA.getMsgNumber());
                    elementB = Long.toString(logMsgB.getMsgNumber());
                    break;
                case LogPanel.LOG_NODE_NAME_COLUMN_INDEX:
                    elementA = logMsgA.getNodeName();
                    elementB = logMsgB.getNodeName();
                    break;
                case LogPanel.LOG_TIMESTAMP_COLUMN_INDEX:
                    elementA = logMsgA.getTime();
                    elementB = logMsgB.getTime();
                    break;
                case LogPanel.LOG_SEVERITY_COLUMN_INDEX:
                    elementA = logMsgA.getSeverity();
                    elementB = logMsgB.getSeverity();
                    break;
                case LogPanel.LOG_MSG_DETAILS_COLUMN_INDEX:
                    elementA = logMsgA.getMsgDetails();
                    elementB = logMsgB.getMsgDetails();
                    break;
                case LogPanel.LOG_JAVA_CLASS_COLUMN_INDEX:
                    elementA = logMsgA.getMsgClass();
                    elementB = logMsgB.getMsgClass();
                    break;
                case LogPanel.LOG_JAVA_METHOD_COLUMN_INDEX:
                    elementA = logMsgA.getMsgMethod();
                    elementB = logMsgB.getMsgMethod();
                    break;
                case LogPanel.LOG_REQUEST_ID_COLUMN_INDEX:
                    elementA = logMsgA.getReqId();
                    elementB = logMsgB.getReqId();
                    break;
                case LogPanel.LOG_NODE_ID_COLUMN_INDEX:
                    elementA = logMsgA.getNodeId();
                    elementB = logMsgB.getNodeId();
                    break;
                default:
                    logger.warning("Bad Statistics Table Column: " + column);
                    break;
            }

            // Treat empty strains like nulls
            if (elementA instanceof String && (elementA).length() == 0) {
                elementA = null;
            }
            if (elementB instanceof String && (elementB).length() == 0) {
                elementB = null;
            }

            // Sort nulls so they appear last, regardless of sort order
            if (elementA == null && elementB == null) {
                return 0;
            } else if (elementA == null) {
                return 1;
            } else if (elementB == null) {
                return -1;
            } else {
                if (ascending) {
                    return (elementA).compareToIgnoreCase(elementB);
                } else {
                    return (elementB).compareToIgnoreCase(elementA);
                }
            }
        }
    }

    /**
     * Initialize the variables when the connection with the cluster is established.
     */
    public void onConnect() {
        logAdmin = (GenericLogAdmin)logAdminLocator.lookup(GenericLogAdmin.class);
        if (logAdmin == null) throw new IllegalStateException("cannot obtain GenericLogAdmin implementation");

        clusterStatusAdmin = (ClusterStatusAdmin) Locator.getDefault().lookup(ClusterStatusAdmin.class);
        if (clusterStatusAdmin == null) throw new RuntimeException("Cannot obtain ClusterStatusAdmin remote reference");

        currentNodeList = new Hashtable();

        clearLogCache();
        canceled = false;
    }

    /**
     *  Reset variables when the connection with the cluster went down.
     */
    public void onDisconnect() {
        clusterStatusAdmin = null;
        logAdmin = null;
        canceled = true;
    }

    /**
     * Return the flag indicating whether the job has been cancelled or not.
     *
     * @return  true if the job is cancelled, false otherwise.
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Clear all caches.
     */
    private void clearLogCache() {
        rawLogCache = new Hashtable();
        filteredLogCache = new Vector();
        currentNodeList = new Hashtable();
        sortedData = new Object[0];
        realModel.setRowCount(sortedData.length);
        realModel.fireTableDataChanged();
    }

    /**
     *  Remove the logs of Non-exist nodes from the cache
     */
    private void removeLogsOfNonExistNodes(Hashtable newNodeList) {
        for (Iterator i = currentNodeList.keySet().iterator();  i.hasNext(); ) {

            Object nodeId = i.next();

            if(newNodeList.get(nodeId) == null) {
                // the node has been removed from the cluster, delete the logs of this node from the cache

                removeLogs((String) nodeId);
            }
        }
    }

    /**
     * Retreive logs from the cluster.
     *
     * @param logPane   The object reference to the LogPanel.
     * @param restartTimer  Specifying whether the refresh timer is restarted after the data retrieval.
     * @param requests  The list of requests for retrieving logs. One request per node.
     * @param newRefresh  Specifying whether this refresh call is a new one or a part of the current refresh cycle.
     */
    public void refreshLogs(final LogPanel logPane, final boolean restartTimer, Vector requests, final boolean newRefresh) {

        //long endMsgNumber = -1;

        if (newRefresh) {

            requests = new Vector();
            for (Iterator i = currentNodeList.keySet().iterator(); i.hasNext();) {
                GatewayStatus gatewayStatus = (GatewayStatus) currentNodeList.get(i.next());

                Object logCache = null;
                if ((logCache = rawLogCache.get(gatewayStatus.getNodeId())) != null) {
                    Vector cachevector = (Vector)logCache;
                    long highest = -1;
                    if (cachevector.size() > 0) {
                        highest = ((LogMessage) cachevector.firstElement()).getMsgNumber();
                        for (Iterator cc = cachevector.iterator(); cc.hasNext();) {
                            LogMessage lm = (LogMessage)cc.next();
                            if (lm.getMsgNumber() > highest) highest = lm.getMsgNumber();
                        }
                        //endMsgNumber = ((LogMessage) cachevector.firstElement()).getMsgNumber();
                    }

                    // add the request for retrieving logs from the node
                    requests.add(new LogRequest(gatewayStatus.getNodeId(), -1, highest));
                }
            }
        }

        // create a worker thread to retrieve the cluster info
        final ClusterLogWorker infoWorker = new ClusterLogWorker(clusterStatusAdmin, logAdmin, currentNodeList, requests) {
            public void finished() {

                if (isCanceled()) {
                    logger.info("Log retrieval is canceled.");
                    logPane.getLogsRefreshTimer().stop();
                } else {
                    // Note: the get() operation is a blocking operation.
                    if (this.get() != null) {

                        if (newRefresh) {
                            removeLogsOfNonExistNodes(getNewNodeList());
                            currentNodeList = getNewNodeList();
                            addLogs(getNewLogs());
                        } else {
                            appendLogs(getNewLogs());
                        }

                        String msgNumSelected = logPane.getSelectedMsgNumber();

                        // filter the logs
                        filterData(logPane.getMsgFilterLevel());

                        // sort the logs
                        sortData(columnToSort, false);

                        // populate the change to the display
                        realModel.fireTableDataChanged();
                        logPane.updateTimeStamp(getCurrentClusterSystemTime());
                        logPane.updateMsgTotal();

                        logPane.setSelectedRow(msgNumSelected);

                        final Vector unfilledRequest = getUnfilledRequest();

                        // if there unfilled requests
                        if (unfilledRequest.size() > 0) {
                            SwingUtilities.invokeLater(
                                    new Runnable() {
                                        public void run() {
                                            refreshLogs(logPane, restartTimer, unfilledRequest, false);
                                        }
                                    });

                        } else {
                            if (restartTimer) {
                                logPane.getLogsRefreshTimer().start();
                            }
                        }

                    } else {
                        if (isRemoteExceptionCaught()) {
                            // the connection to the cluster is down
                            logPanel.onDisconnect();
                            onDisconnect();
                        }
                    }
                }
            }
        };

        infoWorker.start();

    }
}
