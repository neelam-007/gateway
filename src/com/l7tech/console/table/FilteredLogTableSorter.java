package com.l7tech.console.table;

import com.l7tech.logging.LogMessage;
import com.l7tech.logging.LogAdmin;
import com.l7tech.common.util.Locator;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.cluster.LogRequest;
import com.l7tech.cluster.GatewayStatus;
import com.l7tech.console.panels.LogPanel;
import com.l7tech.console.util.ClusterInfoWorker;

import javax.swing.table.DefaultTableModel;
import java.util.logging.Logger;
import java.util.*;

/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class FilteredLogTableSorter extends FilteredLogTableModel {
    static Logger logger = Logger.getLogger(StatisticsTableSorter.class.getName());
    boolean ascending = true;
    int columnToSort = 0;
    int compares;
    private Object[] sortedData = null;
    private ClusterStatusAdmin clusterStatusAdmin = null;
    private LogAdmin logService = null;
    private Hashtable currentNodeList;

    public FilteredLogTableSorter() {
    }

    public FilteredLogTableSorter(DefaultTableModel model) {
        setModel(model);
    }

    public void setModel(DefaultTableModel model) {
        super.setRealModel(model);
    }

    public int getSortedColumn(){
        return columnToSort;
    }

    public boolean isAscending(){
        return ascending;
    }

    private void addData(Hashtable nodeList, int msgFilterLevel) {
        for (Iterator i = nodeList.keySet().iterator(); i.hasNext();) {
            String node = (String) i.next();
            Vector logs = (Vector) nodeList.get(node);
            addData(node, logs, msgFilterLevel);
        }
    }


    private void addData(String nodeId, Vector newLogs, int msgFilterLevel) {

        // add new logs to the cache
        if (newLogs.size() > 0) {

            Object node = null;
            Vector gatewayLogs;
            if ((node = rawLogCache.get(nodeId)) != null) {
                gatewayLogs = (Vector) node;
            }
            else {
                // create a empty cache for the new node
                gatewayLogs = new Vector();
            }

            while (gatewayLogs.size() + newLogs.size() > MAX_NUMBER_OF_LOG_MESSGAES) {
                // remove the last element
                gatewayLogs.remove(gatewayLogs.size() - 1);
            }

            // append the old logs to the cache
            newLogs.addAll(gatewayLogs);

            gatewayLogs = newLogs;

            // update the logsCache
            rawLogCache.put(nodeId, gatewayLogs);

            // filter the logs
            filterData(nodeId, msgFilterLevel);

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
        realModel.setRowCount((sortedData.length));
        realModel.fireTableDataChanged();
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

        Object[] sorted = filteredLogCache.toArray();
        Arrays.sort(sorted, new FilteredLogTableSorter.ColumnSorter(columnToSort, ascending));
        sortedData = sorted;
    }

    public Object getValueAt(int row, int col) {
        switch (col) {
            case 0:
                return new Long (((LogMessage) sortedData[row]).getMsgNumber());
            case 1:
                return ((LogMessage) sortedData[row]).getTime();
            case 2:
                return ((LogMessage) sortedData[row]).getSeverity();
            case 3:
                return ((LogMessage) sortedData[row]).getMessageDetails();
            case 4:
                return ((LogMessage) sortedData[row]).getMessageClass();
            case 5:
                return ((LogMessage) sortedData[row]).getMessageMethod();
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

            switch (column) {
                case 0:
                    elementA = Long.toString(((LogMessage) a).getMsgNumber());
                    elementB = Long.toString(((LogMessage) b).getMsgNumber());
                    break;
                case 1:
                    elementA = ((LogMessage) a).getTime();
                    elementB = ((LogMessage) b).getTime();
                    break;
                case 2:
                    elementA = ((LogMessage) a).getSeverity();
                    elementB = ((LogMessage) b).getSeverity();
                    break;
                case 3:
                    elementA = ((LogMessage) a).getMessageDetails();
                    elementB = ((LogMessage) b).getMessageDetails();
                    break;
                case 4:
                    elementA = ((LogMessage) a).getMessageClass();
                    elementB = ((LogMessage) b).getMessageClass();
                case 5:
                    elementA = ((LogMessage) a).getMessageMethod();
                    elementB = ((LogMessage) b).getMessageMethod();
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
    }

    public void onDisconnect() {
        clusterStatusAdmin = null;
        logService = null;

    }

    public void refreshLogs(final int msgFilterLevel, final LogPanel logPane, final String msgNumSelected, final boolean restartTimer) {

        long endMsgNumber = -1;
        Vector requests = new Vector();

        //todo: check if this is required anymore
        int index = 0;
        for (Iterator i = currentNodeList.keySet().iterator(); i.hasNext(); index++) {
            GatewayStatus gatewayStatus = (GatewayStatus) currentNodeList.get(i.next());

            Object logCache = null;
            if((logCache = rawLogCache.get(gatewayStatus.getNodeId())) != null) {

                if (((Vector) logCache).size() > 0) {
                   endMsgNumber = ((LogMessage) ((Vector) logCache).firstElement()).getMsgNumber();
                }

                //todo: the following line should be moved out of this block - it is placed here for testing purpose only
                // maybe this doesn't matter as the node must be in the rawLogCache anyway
                requests.add(new LogRequest(gatewayStatus.getNodeId(),  -1, endMsgNumber));
            }

            //requests.add(new LogRequest(gatewayStatus.getNodeId(),  -1, endMsgNumber));
        }

        // create a worker thread to retrieve the Service statistics
/*        final LogsWorker logsWorker = new LogsWorker(logService, startMsgNumber, endMsgNumber) {
            public void finished() {
                updateLogsTable(getNewLogs(), msgFilterLevel);
                logPane.updateMsgTotal();
                logPane.setSelectedRow(msgNumSelected);

                if (restartTimer) {
                    logPane.getLogsRefreshTimer().start();
                }
            }
        };

        logsWorker.start();*/

                // create a worker thread to retrieve the cluster info
        final ClusterInfoWorker infoWorker = new ClusterInfoWorker(clusterStatusAdmin, logService, currentNodeList, requests) {
            public void finished() {

                // Note: the get() operation is a blocking operation.
                if (this.get() != null) {

                    currentNodeList = getNewNodeList();

                    logPane.updateTimeStamp();

                    addData(getNewLogs(), msgFilterLevel);

                    logPane.updateMsgTotal();
                    logPane.setSelectedRow(msgNumSelected);

                    if (restartTimer) {
                        logPane.getLogsRefreshTimer().start();
                    }
                } else {
                    if (isRemoteExceptionCaught()) {
                        // the connection to the cluster is down
                        onDisconnect();
                    }
                }
            }
        };

        infoWorker.start();

    }
}
