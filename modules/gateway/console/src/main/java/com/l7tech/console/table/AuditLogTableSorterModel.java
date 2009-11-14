/**
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.table;

import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.LogPanel;
import com.l7tech.console.util.ClusterLogWorker;
import com.l7tech.console.util.LogMessage;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.cluster.GatewayStatus;
import com.l7tech.gateway.common.cluster.LogRequest;
import com.l7tech.gateway.common.logging.GenericLogAdmin;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.security.cert.KeyUsageActivity;
import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.util.HexUtils;

import javax.crypto.Cipher;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.IOException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECKey;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * This class extends the <CODE>FilteredLogTableModel</CODE> class for providing the sorting functionality to the log display.
 */

public class AuditLogTableSorterModel extends FilteredLogTableModel {

    /** Validity state of a digital signature. */
    public enum DigitalSignatureUIState {
        // NOTE: elements are ordered for display sorting (i.e., from bad to good).
        /** Has signature but is invalid. */
        INVALID("invalid", "digital signature is invalid", MainWindow.RESOURCE_PATH + "/DigitalSignatureStateInvalid16.png"),

        /** No signature at all. */
        NONE("missing", "no digital signature", MainWindow.RESOURCE_PATH + "/DigitalSignatureStateNone16.png"),

        /** Has signature and is valid. */
        VALID("verified", "digital signature is verified", MainWindow.RESOURCE_PATH + "/DigitalSignatureStateValid16.png");

        private final String name;
        private final String description;
        private final String icon16Path;
        private Icon icon16;

        DigitalSignatureUIState(String name, String description, String icon16Path) {
            this.name = name;
            this.description = description;
            this.icon16Path = icon16Path;
        }

        /** @return a word suitable for message parameter substitution (e.g., Digital signature is BLAH) */
        public String getName() {
            return name;
        }

        /** @return an uncapitalized short phrase suitable for things like tooltip or exception message */
        public String getDescription() {
            return description;
        }

        /** @return 16 by 16 pixel icon */
        public synchronized Icon getIcon16() {
            if (icon16 == null) {
                // Can't do this in constructor because testpackage complains.
                icon16 = new ImageIcon(ImageCache.getInstance().getIcon(icon16Path));
            }
            return icon16;
        }
    }

    private static Logger logger = Logger.getLogger(AuditLogTableSorterModel.class.getName());
    private boolean ascending = false;
    private int columnToSort = LogPanel.LOG_TIMESTAMP_COLUMN_INDEX;
    private Object[] sortedData = null;
    private ClusterStatusAdmin clusterStatusAdmin = null;
    private AuditAdmin auditAdmin = null;
    private int logType;
    private boolean displayingFromFile;
    private boolean truncated;
    private AtomicReference<ClusterLogWorker> workerReference = new AtomicReference<ClusterLogWorker>();

    /**
     * Constructor taking <CODE>DefaultTableModel</CODE> as the input parameter.
     *
     * @param model  A table model.
     */
    public AuditLogTableSorterModel(DefaultTableModel model, int logType) {
        this.logType = logType;
        this.displayingFromFile = false;

        setModel(model);
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        // the table cells are not editable
        return false;
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
     * @return true if displayed logs are truncated to {@link #MAX_NUMBER_OF_LOG_MESSAGES}.
     */
    public boolean isTruncated() {
        return truncated;
    }

   /**
     * Add the new logs to the cache. The position depends on the input parameter specified.
     *
     * @param newLogs  The new logs.
     */
    private void addLogs(Map<Long, ? extends LogMessage> newLogs) {

        // add new logs to the cache
        if (newLogs.size() > 0) {
            rawLogCache.putAll(newLogs);

            filteredLogCache.clear();
            filteredLogCache.addAll(rawLogCache.values());
        }
    }

    /**
      * Remove the logs of the non-exist node from the cache.
      *
      * @param nodeId  The Id of the node whose logs will be removed from the cache.
      */
     private void removeLogs(String nodeId) {
        Collection<LogMessage> rawLogCacheCollection = rawLogCache.values();
        for(LogMessage logMessage : rawLogCacheCollection){
            if(logMessage.getNodeId().equals(nodeId)){
                rawLogCache.remove(logMessage.getMsgNumber());
            }
        }
    }

    /**
     * Apply the filter specified.
     */
    public void applyNewMsgFilter(LogPanel.LogLevelOption filterLevel,
                                  String filterThreadId,
                                  String filterMessage) {

        filterData(filterLevel, filterThreadId, filterMessage);
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
            ascending = !ascending;
        }

        // always sort in ascending order if the user select a new column
        if (column != columnToSort) {
            ascending = true;
        }

        // save the column index
        columnToSort = column;

        Integer[] sorted = new Integer[filteredLogCache.size()];

        for (int i = 0; i < sorted.length; i++) {
            sorted[i] = i;
        }

        Arrays.sort(sorted, new AuditLogTableSorterModel.ColumnSorter(columnToSort, ascending));
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
    @Override
    public Object getValueAt(int row, int col) {
        LogMessage msg = getLogMessageAtRow(row);
        switch (col) {
            case LogPanel.LOG_SIGNATURE_COLUMN_INDEX:
                return getUISignatureState(msg);
            case LogPanel.LOG_MSG_NUMBER_COLUMN_INDEX:
                return msg.getMsgNumber();
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
                return msg.getServiceName();
            case LogPanel.LOG_THREAD_COLUMN_INDEX:
                return Integer.toString(msg.getThreadID());
            default:
                throw new IllegalArgumentException("Bad Column");
        }
    }

    public LogMessage getLogMessageAtRow(int row) {
        return filteredLogCache.get((Integer)sortedData[row]);
    }

    private DigitalSignatureUIState getUISignatureState(LogMessage msg) {
        try {
            return compareSignatureDigests( msg.getSignature(), msg.getSignatureDigest() );
        } catch ( IOException e ) {
            logger.log(Level.WARNING, "could not serialize audit record", e);
            return DigitalSignatureUIState.INVALID;
        }
    }

    /**
     * A class for determining the order of two objects by comparing their values.
     */
    public class ColumnSorter implements Comparator<Integer> {
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
        @Override
        public int compare(Integer a, Integer b) {

            String elementA = "";
            String elementB = "";

            LogMessage logMsgA = filteredLogCache.get(a);
            LogMessage logMsgB = filteredLogCache.get(b);

            switch (column) {
                case LogPanel.LOG_SIGNATURE_COLUMN_INDEX:
                    final DigitalSignatureUIState stateA = getUISignatureState(logMsgA);
                    final DigitalSignatureUIState stateB = getUISignatureState(logMsgB);
                    return (ascending ? 1 : -1) * stateA.compareTo(stateB);
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
                    elementA = logMsgA.getServiceName();
                    elementB = logMsgB.getServiceName();
                    break;
                case LogPanel.LOG_THREAD_COLUMN_INDEX:
                    elementA = Integer.toString(logMsgA.getThreadID());
                    elementB = Integer.toString(logMsgB.getThreadID());
                    break;
                default:
                    logger.warning("Bad Statistics Table Column: " + column);
                    break;
            }

            // Treat empty strains like nulls
            if (elementA != null && (elementA).length() == 0) {
                elementA = null;
            }
            if (elementB != null && (elementB).length() == 0) {
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
        auditAdmin = Registry.getDefault().getAuditAdmin();
        currentNodeList = new HashMap<String, GatewayStatus>();

        clearLogCache();
    }

    /**
     *  Reset variables when the connection with the cluster went down.
     */
    public void onDisconnect() {
        clearLogCache();

        clusterStatusAdmin = null;
        auditAdmin = null;
    }

    public int getDelay() {
        int delay = 3;

        if (auditAdmin != null) {
            delay = auditAdmin.getSystemLogRefresh(logType);
        }

        return delay;
    }

    /**
     * Clear all caches.
     */
    public void clearLogCache() {
        cancelWorker();
        rawLogCache = new HashMap<Long,LogMessage>();
        filteredLogCache = new ArrayList<LogMessage>();
        currentNodeList = new HashMap<String, GatewayStatus>();
        sortedData = new Object[0];
        realModel.setRowCount(sortedData.length);
        realModel.fireTableDataChanged();
        truncated = false;
    }

    private void cancelWorker() {
        final ClusterLogWorker worker = workerReference.getAndSet(null);
        if ( worker != null ) {
            worker.cancel();
        }
    }

    /*
     *  Remove the logs of Non-exist nodes from the cache
     */
    private void removeLogsOfNonExistNodes(Map newNodeList) {
        for (String nodeId : currentNodeList.keySet()) {
            if (newNodeList.get(nodeId) == null) {
                // the node has been removed from the cluster, delete the logs of this node from the cache
                removeLogs(nodeId);
            }
        }
    }

    /**
     * Retrieve audits/logs from the cluster.
     *
     * @param logPane The object reference to the LogPanel.
     * @param logRequest The LogRequest object containing the search criteria.
     * @param restartTimer Specifying whether or not to restart the refresh timer.
     */
    public void refreshLogs(final LogPanel logPane, LogRequest logRequest, final boolean restartTimer) {
        //if auto-refresh is enabled, we only want the latest logs, we dont want to re-load every record.
        if (restartTimer) {
            //...so must set the start message number
            //TODO there is likely a faster way to do this
            long highest = -1;
            Collection<LogMessage> rawLogCacheCollection = rawLogCache.values();
            for (LogMessage logMessage : rawLogCacheCollection) {
                if (logMessage.getMsgNumber() > highest) {
                    highest = logMessage.getMsgNumber();
                }
            }
            logRequest.setStartMsgNumber(highest);
        }
        doRefreshLogs(logPane, logRequest, restartTimer, 0);
    }

    /**
     * Set the log/audit data to be displayed.
     *
     * @param logPane   The object reference to the LogPanel.
     * @param logs the data list.
     */
    public void setLogs(final LogPanel logPane, final Map<Long, ? extends LogMessage> logs) {
        logger.info("Importing "+/*count*/logs.size()+" log/audit records.");

        // import
        clearLogCache();
        displayingFromFile = true;
        addLogs(logs);

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
     * @param logRequest Request parameters.
     * @param restartTimer  Specifying whether the refresh timer should be restarted.
     * @param count Number of records retrieved in the current refresh cycle.
     */
    private void doRefreshLogs(final LogPanel logPane, LogRequest logRequest, final boolean restartTimer, final int count) {

        if(displayingFromFile) {
            displayingFromFile = false;
            clearLogCache();
        }

        try {            
            // create a worker thread to retrieve the cluster info
            final ClusterLogWorker infoWorker = new ClusterLogWorker(
                    clusterStatusAdmin,
                    auditAdmin,
                    logType,
                    //currentNodeList,
                    logRequest) {
                @Override
                public void finished() {
                    if ( !isCancelled() ) {
                        // Note: the get() operation is a blocking operation.
                        if (this.get() != null) {
                            Map<Long, LogMessage> newLogs = getNewLogs();
                            int logCount = newLogs.size();
                            boolean updated = logCount > 0;

                            if (count==0) {
                                Map<String, GatewayStatus> newNodeList = getNewNodeList();
                                removeLogsOfNonExistNodes(newNodeList);
                                updated = updated || currentNodeList==null || !currentNodeList.keySet().equals(newNodeList.keySet());
                                currentNodeList = newNodeList;
                            }

                            addLogs(newLogs);

                            if (updated) {

                                String msgNumSelected = logPane.getSelectedMsgNumber();

                                // filter the logs
                                if(logType == GenericLogAdmin.TYPE_LOG){
                                    filterData(logPane.getMsgFilterLevel(),
                                        logPane.getMsgFilterThreadId(),
                                        logPane.getMsgFilterMessage());
                                }

                                // sort the logs
                                sortData(columnToSort, false);

                                // populate the change to the display
                                realModel.fireTableDataChanged();
                                logPane.updateMsgTotal();
                                logPane.setSelectedRow(msgNumSelected);
                            }

                            logPane.updateTimeStamp(getCurrentClusterSystemTime());

                            final LogRequest unfilledRequest = getUnfilledRequest();

                            // if there unfilled requests
                            final int total = count + logCount;
                            if (unfilledRequest != null && total < MAX_NUMBER_OF_LOG_MESSAGES) {
                                logPane.getMsgProgressBar().setVisible(true);
                                SwingUtilities.invokeLater(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                doRefreshLogs(logPane, unfilledRequest, restartTimer, total);
                                            }
                                        });

                            } else {
                                logPane.getMsgProgressBar().setVisible(false);
                                if (restartTimer) {
                                    logPane.getLogsRefreshTimer().start();
                                }
                            }

                        } else {
                            logPane.getMsgProgressBar().setVisible(false);
                            if (restartTimer) {
                                logPane.getLogsRefreshTimer().start();
                            }
                        }
                    }
                }
            };

            workerReference.set( infoWorker );
            infoWorker.start();
        }
        catch(IllegalArgumentException iae) {
            //can happen on disconnect when auto refresh is on.
            logPane.getMsgProgressBar().setVisible(false);
        }
    }

    private DigitalSignatureUIState compareSignatureDigests( String signatureToVerify, byte[] digestValue ) {
        if (signatureToVerify == null || signatureToVerify.length() < 1 || digestValue == null) {
            return DigitalSignatureUIState.NONE;
        }

        // get the cert of the ssg we're connected to
        X509Certificate cert = TopComponents.getInstance().getSsgCert()[0];
        if (cert == null) return DigitalSignatureUIState.INVALID;
        PublicKey pub = cert.getPublicKey();
        if (pub == null) return DigitalSignatureUIState.INVALID;
        try {
            KeyUsageChecker.requireActivity(KeyUsageActivity.verifyXml, cert);
            byte[] decodedSig = HexUtils.decodeBase64(signatureToVerify);
            boolean isEcc = pub instanceof ECKey || "EC".equals(pub.getAlgorithm());
            Signature sig = Signature.getInstance(isEcc ? "NONEwithECDSA" : "NONEwithRSA");
            sig.initVerify(pub);
            sig.update(digestValue);
            sig.verify(decodedSig);
            return DigitalSignatureUIState.VALID;

        } catch (Exception e) {
            logger.log(Level.WARNING, "cannot verify signature", e);
        }
        return DigitalSignatureUIState.INVALID;
    }
}
