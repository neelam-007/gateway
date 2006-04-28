package com.l7tech.console.table;

import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.cluster.GatewayStatus;
import com.l7tech.cluster.LogRequest;
import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.console.panels.LogPanel;
import com.l7tech.console.util.ClusterLogWorker;
import com.l7tech.console.util.Registry;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.logging.GenericLogAdmin;
import com.l7tech.logging.LogMessage;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.LinkedHashSet;

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
    private int logType;
    private boolean canceled;
    private LogPanel logPanel;
    private boolean displayingFromFile;
    private long timeOffset = 1000L*60L*60L*3L;


    /**
     * Constructor taking <CODE>DefaultTableModel</CODE> as the input parameter.
     *
     * @param logPanel The panel of log browser.
     * @param model  A table model.
     */
    public FilteredLogTableSorter(LogPanel logPanel, DefaultTableModel model, int logType) {
        this.logPanel = logPanel;
        this.logType = logType;
        this.displayingFromFile = false;
        setModel(model);
    }

    /**
     * Set the time offset.
     *
     * <p>This is used to filter data when a date is not explicitly passed.</p>
     *
     * @param offset the offet to use
     */
    public void setTimeOffset(long offset) {
        timeOffset = offset;
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
    private void appendLogs(Map nodeList) {
        for (Iterator i = nodeList.keySet().iterator(); i.hasNext();) {
            String node = (String) i.next();
            Collection logs = (Collection) nodeList.get(node);
            addLogs(node, logs, false);
        }
    }

    /**
     * Add the new logs to head of the cache.
     *
     * @param nodeList  A list of node objects accompanied with their new logs.
     */
    private void addLogs(Map nodeList) {
        for (Iterator i = nodeList.keySet().iterator(); i.hasNext();) {
            String node = (String) i.next();
            Collection logs = (Collection) nodeList.get(node);
            addLogs(node, logs, true);
        }
    }

   /**
     * Add the new logs to the cache. The position depends on the input parameter specified.
     *
     * @param nodeId  The Id of the node which is the source of the new logs.
     * @param newLogs  The new logs.
     */
    private void addLogs(String nodeId, Collection newLogs, boolean front) {

        // add new logs to the cache
        if (newLogs.size() > 0) {

            Object node = null;
            Collection gatewayLogs;
            if ((node = rawLogCache.get(nodeId)) != null) {
                gatewayLogs = (Collection) node;
            } else {
                // create a empty cache for the new node
                gatewayLogs = new LinkedHashSet();
            }

            Iterator setIter = gatewayLogs.iterator();
            for(int i=0; i<gatewayLogs.size(); i++) {
                // remove the last element
                setIter.next();
                if(i+newLogs.size()>MAX_NUMBER_OF_LOG_MESSGAES) {
                    setIter.remove();
                }
            }

            if (front) {
                // append the old logs to the cache
                newLogs.addAll(gatewayLogs);

                if(newLogs instanceof LinkedHashSet) {
                    gatewayLogs = newLogs;
                }
                else {
                    gatewayLogs = new LinkedHashSet(newLogs);
                }
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
    public void applyNewMsgFilter(int filterLevel,
                                  String filterNodeName,
                                  String filterService,
                                  String filterThreadId,
                                  String filterMessage) {

        filterData(filterLevel, filterNodeName, filterService, filterThreadId, filterMessage);
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
     * Return the value of the table cell specified with its table coordinate.
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
            case LogPanel.LOG_SERVICE_COLUMN_INDEX:
                return getServiceName(msg);
            case LogPanel.LOG_THREAD_COLUMN_INDEX:
                return Integer.toString(msg.getSSGLogRecord().getThreadID());
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
                case LogPanel.LOG_SERVICE_COLUMN_INDEX:
                    elementA = getServiceName(logMsgA);
                    elementB = getServiceName(logMsgB);
                    break;
                case LogPanel.LOG_THREAD_COLUMN_INDEX:
                    elementA = Integer.toString(logMsgA.getSSGLogRecord().getThreadID());
                    elementB = Integer.toString(logMsgB.getSSGLogRecord().getThreadID());
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
        clusterStatusAdmin = Registry.getDefault().getClusterStatusAdmin();
        logAdmin = Registry.getDefault().getAuditAdmin();
        currentNodeList = new HashMap();

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

    public int getDelay() {
        int delay = 3;

        if(logAdmin!=null) {
            delay = logAdmin.getSystemLogRefresh(logType);
        }

        return delay;
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
    public void clearLogCache() {
        rawLogCache = new HashMap();
        filteredLogCache = new ArrayList();
        currentNodeList = new HashMap();
        sortedData = new Object[0];
        realModel.setRowCount(sortedData.length);
        realModel.fireTableDataChanged();
    }

    /**
     *  Remove the logs of Non-exist nodes from the cache
     */
    private void removeLogsOfNonExistNodes(Map newNodeList) {
        for (Iterator i = currentNodeList.keySet().iterator();  i.hasNext(); ) {

            Object nodeId = i.next();

            if(newNodeList.get(nodeId) == null) {
                // the node has been removed from the cluster, delete the logs of this node from the cache

                removeLogs((String) nodeId);
            }
        }
    }

    /**
     * Remove any logs that are outside the selected date range
     */
    private void purgeOutOfRange(Collection logMessageCollection, Date start, Date end) {
        if(logMessageCollection!=null && (start!=null || end!=null)) {
            for (Iterator iterator = logMessageCollection.iterator(); iterator.hasNext();) {
                LogMessage logMessage = (LogMessage) iterator.next();
                if(start!=null && logMessage.getSSGLogRecord().getMillis() < start.getTime()) {
                    iterator.remove();
                }
                else if(end!=null && logMessage.getSSGLogRecord().getMillis() >= end.getTime()) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Retreive logs from the cluster.
     *
     * @param logPane   The object reference to the LogPanel.
     * @param restartTimer  Specifying whether the refresh timer is restarted after the data retrieval.
     * @param nodeId the node to filter requests by (may be null)
     * @param newRefresh  Specifying whether this refresh call is a new one or a part of the current refresh cycle.
     */
    public void refreshLogs(final LogPanel logPane, final boolean restartTimer, final String nodeId) {
        // Load the last 3 hours initially
        Date startDate =  new Date(System.currentTimeMillis()-timeOffset);
        doRefreshLogs(logPane, restartTimer, startDate, null, null, nodeId, true);
    }

    /**
     * Retreive logs from the cluster.
     *
     * @param logPane   The object reference to the LogPanel.
     * @param start The start date for log records.
     * @param end The end date for log records.
     * @param nodeId the node to filter requests by (may be null)
     * @param newRefresh  Specifying whether this refresh call is a new one or a part of the current refresh cycle.
     */
    public void refreshLogs(final LogPanel logPane, final Date start, final Date end, final String nodeId) {
        doRefreshLogs(logPane, false, start, end, null, nodeId, true);
    }

    /**
     * Set the log/audit data to be displayed.
     *
     * <p>The logs Map is a map of nodeId (Strings) to Collection of log
     * messages (LogMessage).</p>
     *
     * @param logPane   The object reference to the LogPanel.
     * @param logs the data hashtable.
     */
    public void setLogs(final LogPanel logPane, final Map logs) {
        // validate input
        int count = 0;
        for(Iterator logEntryIter=logs.entrySet().iterator(); logEntryIter.hasNext();){
            Map.Entry me = (Map.Entry) logEntryIter.next();
            Object key = me.getKey();
            Object value = me.getValue();
            if(!(key instanceof String) ||
               !(value instanceof Collection)) {
                return;
            }
            Collection logMessages = (Collection) value;
            count += logMessages.size();
            for (Iterator iterator = logMessages.iterator(); iterator.hasNext();) {
                Object o = iterator.next();
                if(!(o instanceof LogMessage)) {
                    return;
                }
            }
        }
        logger.info("Importing "+count+" log/audit records.");

        // import
        clearLogCache();
        displayingFromFile = true;
        for(Iterator logEntryIter=logs.entrySet().iterator(); logEntryIter.hasNext();){
            Map.Entry me = (Map.Entry) logEntryIter.next();
            String nodeId = (String) me.getKey();
            Collection logVector = (Collection) me.getValue();
            if(!logVector.isEmpty()) {
                LogMessage logMessage = (LogMessage) logVector.iterator().next();
                ClusterNodeInfo cni = new ClusterNodeInfo();
                cni.setName(logMessage.getNodeName());
                cni.setMac(logMessage.getNodeId());
                currentNodeList.put(nodeId, new GatewayStatus(cni));
            }
        }
        addLogs(logs);

        // filter the logs
        filterData(logPane.getMsgFilterLevel(),
                logPane.getMsgFilterNodeName(),
                logPane.getMsgFilterService(),
                logPane.getMsgFilterThreadId(),
                logPane.getMsgFilterMessage());

        // sort the logs
        sortData(columnToSort, false);

        // populate the change to the display
        realModel.fireTableDataChanged();
        logPane.updateTimeStamp(new Date());
        logPane.updateMsgTotal();
    }

    /**
     * Retreive logs from the cluster.
     *
     * @param logPane   The object reference to the LogPanel.
     * @param restartTimer  Specifying whether the refresh timer is restarted after the data retrieval.
     * @param start The start date for log records.
     * @param end The end date for log records.
     * @param requests  The list of requests for retrieving logs. One request per node.
     * @param nodeId the node to filter requests by (may be null)
     * @param newRefresh  Specifying whether this refresh call is a new one or a part of the current refresh cycle.
     */
    private void doRefreshLogs(final LogPanel logPane, final boolean restartTimer, final Date start, final Date end, List requests, final String nodeId, final boolean newRefresh) {

        if(displayingFromFile) {
            displayingFromFile = false;
            clearLogCache();
        }

        // New request or still working on an old request?
        if (newRefresh) {
            // create request for each node
            requests = new ArrayList();
            for (Iterator i = currentNodeList.keySet().iterator(); i.hasNext();) {
                GatewayStatus gatewayStatus = (GatewayStatus) currentNodeList.get(i.next());

                Object logCache = null;
                if ((logCache = rawLogCache.get(gatewayStatus.getNodeId())) != null) {
                    Collection cachevector = (Collection)logCache;
                    long highest = -1;
                    if (cachevector.size() > 0) {
                        // remove any cached logs that are outside of our current range.
                        purgeOutOfRange(cachevector, start, end);

                        // find limit
                        for (Iterator cc = cachevector.iterator(); cc.hasNext();) {
                            LogMessage lm = (LogMessage)cc.next();
                            if (lm.getMsgNumber() > highest) highest = lm.getMsgNumber();
                        }
                    }

                    // add the request for retrieving logs from the node
                    requests.add(new LogRequest(gatewayStatus.getNodeId(), -1, highest, start, end));
                }
            }
        }

        try {
            // create a worker thread to retrieve the cluster info
            final ClusterLogWorker infoWorker = new ClusterLogWorker(
                    clusterStatusAdmin,
                    logAdmin,
                    logType,
                    nodeId,
                    start,
                    end,
                    currentNodeList,
                    requests) {
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
                            filterData(logPane.getMsgFilterLevel(),
                                    logPane.getMsgFilterNodeName(),
                                    logPane.getMsgFilterService(),
                                    logPane.getMsgFilterThreadId(),
                                    logPane.getMsgFilterMessage());

                            // sort the logs
                            sortData(columnToSort, false);

                            // populate the change to the display
                            realModel.fireTableDataChanged();
                            logPane.updateTimeStamp(getCurrentClusterSystemTime());
                            logPane.updateMsgTotal();

                            logPane.setSelectedRow(msgNumSelected);

                            final List unfilledRequest = getUnfilledRequest();

                            // if there unfilled requests
                            if (unfilledRequest.size() > 0) {
                                SwingUtilities.invokeLater(
                                        new Runnable() {
                                            public void run() {
                                                doRefreshLogs(logPane, restartTimer, start, end, unfilledRequest, nodeId, false);
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
                                ErrorManager.getDefault().notify(Level.WARNING, getRemoteException(), "Could not get data from gateway.");
                            }
                        }
                    }
                }
            };

            infoWorker.start();
        }
        catch(IllegalArgumentException iae) {
            //can happen on disconnect when auto refresh is on.
        }
    }
}
