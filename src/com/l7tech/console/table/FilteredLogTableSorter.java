package com.l7tech.console.table;

import com.l7tech.logging.LogMessage;
import com.l7tech.logging.LogAdmin;
import com.l7tech.common.util.Locator;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.cluster.LogRequest;
import com.l7tech.cluster.GatewayStatus;
import com.l7tech.console.panels.LogPanel;
import com.l7tech.console.util.ClusterLogWorker;

import javax.swing.table.DefaultTableModel;
import javax.swing.*;
import java.util.logging.Logger;
import java.util.*;

/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class FilteredLogTableSorter extends FilteredLogTableModel {
    static Logger logger = Logger.getLogger(StatisticsTableSorter.class.getName());
    boolean ascending = false;
    int columnToSort = LogPanel.LOG_TIMESTAMP_COLUMN_INDEX;
    int compares;
    private Object[] sortedData = null;
    private ClusterStatusAdmin clusterStatusAdmin = null;
    private LogAdmin logService = null;
    private boolean canceled;

    public FilteredLogTableSorter() {
    }

    public FilteredLogTableSorter(DefaultTableModel model) {
        setModel(model);
    }

    public void setModel(DefaultTableModel model) {
        super.setRealModel(model);
    }

    public int getSortedColumn() {
        return columnToSort;
    }

    public boolean isAscending() {
        return ascending;
    }

    private void appendData(Hashtable nodeList, int msgFilterLevel) {
        for (Iterator i = nodeList.keySet().iterator(); i.hasNext();) {
            String node = (String) i.next();
            Vector logs = (Vector) nodeList.get(node);
            addData(node, logs, msgFilterLevel, false);
        }
    }

    private void addData(Hashtable nodeList, int msgFilterLevel) {
        for (Iterator i = nodeList.keySet().iterator(); i.hasNext();) {
            String node = (String) i.next();
            Vector logs = (Vector) nodeList.get(node);
            addData(node, logs, msgFilterLevel, true);
        }
    }


    private void addData(String nodeId, Vector newLogs, int msgFilterLevel, boolean front) {

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

            // filter the logs
            filterData(msgFilterLevel);

            // sort the logs
            sortData(columnToSort, false);

            // populate the change to the display
            realModel.fireTableDataChanged();
        }
    }

    public void applyNewMsgFilter(int newFilterLevel) {
        filterData(newFilterLevel);

        sortData(columnToSort, false);
//        System.out.println("Number of rows in sortedData array: " + sortedData.length);

        realModel.fireTableDataChanged();
    }

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

        //Object[] sorted = filteredLogCache.toArray();

        Integer[] sorted = new Integer[filteredLogCache.size()];

        for (int i = 0; i < sorted.length; i++) {
            sorted[i] = new Integer(i);
        }

        Arrays.sort(sorted, new FilteredLogTableSorter.ColumnSorter(columnToSort, ascending));
        sortedData = sorted;

        // update the row count
        realModel.setRowCount(sortedData.length);
    }

    public Object getValueAt(int row, int col) {

        LogMessage msg = (LogMessage) filteredLogCache.get(((Integer) sortedData[row]).intValue());

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
                    return (elementA).compareToIgnoreCase(elementB);
                } else {
                    return (elementB).compareToIgnoreCase(elementA);
                }
            }
        }
    }

    public void onConnect() {
        logService = (LogAdmin) Locator.getDefault().lookup(LogAdmin.class);
        if (logService == null) throw new IllegalStateException("cannot obtain LogAdmin remote reference");

        clusterStatusAdmin = (ClusterStatusAdmin) Locator.getDefault().lookup(ClusterStatusAdmin.class);
        if (clusterStatusAdmin == null) throw new RuntimeException("Cannot obtain ClusterStatusAdmin remote reference");

        currentNodeList = new Hashtable();

        clearLogCache();
        canceled = false;
    }

    public void onDisconnect() {
        clusterStatusAdmin = null;
        logService = null;
        canceled = true;

    }

    public boolean isCanceled() {
        return canceled;
    }

    private void clearLogCache() {
        rawLogCache = new Hashtable();
        filteredLogCache = new Vector();
        currentNodeList = new Hashtable();
        sortedData = new Object[0];
        realModel.setRowCount(sortedData.length);
        realModel.fireTableDataChanged();
    }

    public void refreshLogs(final int msgFilterLevel, final LogPanel logPane, final String msgNumSelected, final boolean restartTimer, Vector requests, final boolean newRefresh) {

        long endMsgNumber = -1;
        //Vector requests = new Vector();
        if (newRefresh) {
            //todo: check if this is required anymore
            requests = new Vector();
            for (Iterator i = currentNodeList.keySet().iterator(); i.hasNext();) {
                GatewayStatus gatewayStatus = (GatewayStatus) currentNodeList.get(i.next());

                Object logCache = null;
                if ((logCache = rawLogCache.get(gatewayStatus.getNodeId())) != null) {

                    if (((Vector) logCache).size() > 0) {
                        endMsgNumber = ((LogMessage) ((Vector) logCache).firstElement()).getMsgNumber();
                    }

                    //todo: the following line should be moved out of this block - it is placed here for testing purpose only
                    // maybe this doesn't matter as the node must be in the rawLogCache anyway
                    requests.add(new LogRequest(gatewayStatus.getNodeId(), -1, endMsgNumber));
                }

                //requests.add(new LogRequest(gatewayStatus.getNodeId(),  -1, endMsgNumber));
            }
        }
        // create a worker thread to retrieve the cluster info
        final ClusterLogWorker infoWorker = new ClusterLogWorker(clusterStatusAdmin, logService, currentNodeList, requests) {
            public void finished() {

                if (isCanceled()) {
                    logger.info("Log retrieval is canceled.");
                    logPane.getLogsRefreshTimer().stop();
                } else {
                    // Note: the get() operation is a blocking operation.
                    if (this.get() != null) {

                        if (newRefresh) {
                            currentNodeList = getNewNodeList();
                            addData(getNewLogs(), msgFilterLevel);
                        } else {
                            appendData(getNewLogs(), msgFilterLevel);
                        }

                        logPane.updateTimeStamp();


                        logPane.updateMsgTotal();
                        logPane.setSelectedRow(msgNumSelected);

                        final Vector unfilledRequest = getUnfilledRequest();


                        // if there unfilled requests
                        if (unfilledRequest.size() > 0) {
                            SwingUtilities.invokeLater(
                                    new Runnable() {
                                        public void run() {
                                            refreshLogs(msgFilterLevel, logPane, msgNumSelected, restartTimer, unfilledRequest, false);
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
                            onDisconnect();
                        }
                    }
                }
            }
        };

        infoWorker.start();

    }
}
