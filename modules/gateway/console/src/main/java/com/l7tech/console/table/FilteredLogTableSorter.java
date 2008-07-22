/**
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.table;

import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.cluster.GatewayStatus;
import com.l7tech.gateway.common.cluster.LogRequest;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.util.HexUtils;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.LogPanel;
import com.l7tech.console.util.ClusterLogWorker;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.logging.LogMessage;
import com.l7tech.gateway.common.logging.SSGLogRecord;

import javax.crypto.Cipher;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * This class extends the <CODE>FilteredLogTableModel</CODE> class for providing the sorting functionality to the log display.
 */

public class FilteredLogTableSorter extends FilteredLogTableModel {

    /** Validity state of a digital signature. */
    public enum DigitalSignatureState {
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

        DigitalSignatureState(String name, String description, String icon16Path) {
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
    };

    private static Logger logger = Logger.getLogger(StatisticsTableSorter.class.getName());
    private boolean ascending = false;
    private int columnToSort = LogPanel.LOG_TIMESTAMP_COLUMN_INDEX;
    private Object[] sortedData = null;
    private ClusterStatusAdmin clusterStatusAdmin = null;
    private AuditAdmin auditAdmin = null;
    private int logType;
    private boolean canceled;
    private boolean displayingFromFile;
    private boolean truncated;

    /**
     * Constructor taking <CODE>DefaultTableModel</CODE> as the input parameter.
     *
     * @param model  A table model.
     */
    public FilteredLogTableSorter(DefaultTableModel model, int logType) {
        this.logType = logType;
        this.displayingFromFile = false;
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
     * @return true if displayed logs are truncated to {@link #MAX_NUMBER_OF_LOG_MESSGAES}.
     */
    public boolean isTruncated() {
        return truncated;
    }

    /**
     * Add the new logs to head of the cache.
     *
     * @param nodeList  A list of node objects accompanied with their new logs.
     */
    private void addLogs(Map<String, Collection<LogMessage>> nodeList) {
        for (String node : nodeList.keySet()) {
            Collection<LogMessage> logs = nodeList.get(node);
            addLogs(node, logs);
        }
    }

   /**
     * Add the new logs to the cache. The position depends on the input parameter specified.
     *
     * @param nodeId  The Id of the node which is the source of the new logs.
     * @param newLogs  The new logs.
     */
    private void addLogs(String nodeId, Collection<LogMessage> newLogs) {

        // add new logs to the cache
        if (newLogs.size() > 0) {
            Collection<LogMessage> gatewayLogs;
            Collection<LogMessage> cachedLogs;
            if ((cachedLogs = rawLogCache.get(nodeId)) != null) {
                gatewayLogs = cachedLogs;
            } else {
                // create a empty cache for the new node
                gatewayLogs = new TreeSet<LogMessage>();
            }

            if (gatewayLogs.size() < MAX_NUMBER_OF_LOG_MESSGAES) {
                truncated = false;
            }

            // try to remove first to make room
            Iterator setIter = gatewayLogs.iterator();
            int additionalLogCount = newLogs.size() - (MAX_MESSAGE_BLOCK_SIZE/10); // allow for 10% duplicates
            for(int i=0; setIter.hasNext(); i++) {
                // remove the last element
                setIter.next();
                if(i+additionalLogCount>=MAX_NUMBER_OF_LOG_MESSGAES) {
                    setIter.remove();
                }
            }

            // add logs
            gatewayLogs.addAll(newLogs);

            // remove after to ensure correct size
            final int sizeBefore = gatewayLogs.size();
            setIter = gatewayLogs.iterator();
            for(int i=0; setIter.hasNext(); i++) {
                // remove the last element
                setIter.next();
                if(i>=MAX_NUMBER_OF_LOG_MESSGAES) {
                    setIter.remove();
                }
            }
            if (gatewayLogs.size() < sizeBefore) {
                truncated = true;
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
            case LogPanel.LOG_SIGNATURE_COLUMN_INDEX:
                return checkDigitalSignature(msg.getSSGLogRecord());
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
        return filteredLogCache.get(((Integer) sortedData[row]).intValue());
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
        public int compare(Integer a, Integer b) {

            String elementA = "";
            String elementB = "";

            LogMessage logMsgA = filteredLogCache.get(a.intValue());
            LogMessage logMsgB = filteredLogCache.get(b.intValue());

            switch (column) {
                case LogPanel.LOG_SIGNATURE_COLUMN_INDEX:
                    final DigitalSignatureState stateA = checkDigitalSignature(logMsgA.getSSGLogRecord());
                    final DigitalSignatureState stateB = checkDigitalSignature(logMsgB.getSSGLogRecord());
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
        canceled = false;
    }

    /**
     *  Reset variables when the connection with the cluster went down.
     */
    public void onDisconnect() {
        clusterStatusAdmin = null;
        auditAdmin = null;
        canceled = true;
    }

    public int getDelay() {
        int delay = 3;

        if (auditAdmin != null) {
            delay = auditAdmin.getSystemLogRefresh(logType);
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
        rawLogCache = new HashMap<String, Collection<LogMessage>>();
        filteredLogCache = new ArrayList<LogMessage>();
        currentNodeList = new HashMap<String, GatewayStatus>();
        sortedData = new Object[0];
        realModel.setRowCount(sortedData.length);
        realModel.fireTableDataChanged();
        truncated = false;
    }

    /**
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
     * Retreive logs from the cluster for a given duration in the past.
     *
     * @param logPane   The object reference to the LogPanel.
     * @param duration  duration in the past to retrieve (in milliseconds)
     * @param restartTimer  Specifying whether the refresh timer should be restarted.
     * @param nodeId the node to filter requests by (may be null)
     */
    public void refreshLogs(final LogPanel logPane, final long duration, final boolean restartTimer, final String nodeId) {
        Date startDate =  new Date(System.currentTimeMillis() - duration);
        doRefreshLogs(logPane, restartTimer, startDate, null, null, nodeId, 0);
    }

    /**
     * Retreive logs from the cluster for a given time range.
     *
     * @param logPane   The object reference to the LogPanel.
     * @param start The start date for log records.
     * @param end The end date for log records.
     * @param nodeId the node to filter requests by (may be null)
     */
    public void refreshLogs(final LogPanel logPane, final Date start, final Date end, final String nodeId) {
        doRefreshLogs(logPane, false, start, end, null, nodeId, 0);
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
    public void setLogs(final LogPanel logPane, final Map<String, Collection<LogMessage>> logs) {
        // validate input
        int count = 0;
        for (Map.Entry<String, Collection<LogMessage>> me : logs.entrySet()) {
            Collection<LogMessage> logMessages = me.getValue();
            count += logMessages.size();
        }
        logger.info("Importing "+count+" log/audit records.");

        // import
        clearLogCache();
        displayingFromFile = true;
        for (Map.Entry<String, Collection<LogMessage>> entry : logs.entrySet()) {
            String nodeId = entry.getKey();
            Collection<LogMessage> logVector = entry.getValue();
            if (!logVector.isEmpty()) {
                LogMessage logMessage = logVector.iterator().next();
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
     * @param restartTimer  Specifying whether the refresh timer should be restarted.
     * @param start The start date for log records.
     * @param end The end date for log records.
     * @param requests  The list of requests for retrieving logs. One request per node.
     * @param nodeId the node to filter requests by (may be null)
     * @param count Number of records retrieved in the current refresh cycle.
     */
    private void doRefreshLogs(final LogPanel logPane, final boolean restartTimer, final Date start, final Date end, List<LogRequest> requests, final String nodeId, final int count) {

        if(displayingFromFile) {
            displayingFromFile = false;
            clearLogCache();
        }

        // New request or still working on an old request?
        if (count == 0) {
            // create request for each node
            requests = new ArrayList<LogRequest>();
            for (String s : currentNodeList.keySet()) {
                GatewayStatus gatewayStatus = currentNodeList.get(s);

                if (nodeId != null && !nodeId.equals(gatewayStatus.getNodeId()))
                    continue;

                Collection<LogMessage> logCache = rawLogCache.get(gatewayStatus.getNodeId());
                long latest = -1;
                if (logCache != null && logCache.size() > 0) {
                    // remove any cached logs that are outside of our current range.
                    purgeOutOfRange(logCache, start, end);

                    // find limit
                    for (LogMessage lm : logCache) {
                        if (lm.getSSGLogRecord().getMillis() > latest) latest = lm.getSSGLogRecord().getMillis();
                    }
                }

                // add the request for retrieving logs from the node
                Date latestDate = new Date(latest);
                if (latestDate.after(start)) {
                    requests.add(new LogRequest(gatewayStatus.getNodeId(), -1, -1, latestDate, end));
                } else {
                    requests.add(new LogRequest(gatewayStatus.getNodeId(), -1, -1, start, end));
                }
            }
        }

        try {
            // create a worker thread to retrieve the cluster info
            final ClusterLogWorker infoWorker = new ClusterLogWorker(
                    clusterStatusAdmin,
                    auditAdmin,
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
                        logPane.getMsgProgressBar().setVisible(false);
                    } else {
                        // Note: the get() operation is a blocking operation.
                        if (this.get() != null) {

                            Map<String, Collection<LogMessage>> newLogs = getNewLogs();
                            int logCount = countLogs(newLogs);
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
                                filterData(logPane.getMsgFilterLevel(),
                                        logPane.getMsgFilterNodeName(),
                                        logPane.getMsgFilterService(),
                                        logPane.getMsgFilterThreadId(),
                                        logPane.getMsgFilterMessage());

                                // sort the logs
                                sortData(columnToSort, false);

                                // populate the change to the display
                                realModel.fireTableDataChanged();
                                logPane.updateMsgTotal();

                                logPane.setSelectedRow(msgNumSelected);
                            }

                            logPane.updateTimeStamp(getCurrentClusterSystemTime());

                            final List<LogRequest> unfilledRequest = getUnfilledRequest();

                            // if there unfilled requests
                            final int total = count + logCount;
                            if (unfilledRequest.size() > 0 && total < MAX_NUMBER_OF_LOG_MESSGAES) {
                                logPane.getMsgProgressBar().setVisible(true);
                                SwingUtilities.invokeLater(
                                        new Runnable() {
                                            public void run() {
                                                doRefreshLogs(logPane, restartTimer, start, end, unfilledRequest, nodeId, total);
                                            }
                                        });

                            } else {
                                logPane.getMsgProgressBar().setVisible(false);
                                if (restartTimer) {
                                    logPane.getLogsRefreshTimer().start();
                                }
                            }

                        }
                    }
                }

                private int countLogs(Map<String, Collection<LogMessage>> newLogs) {
                    int count = 0;

                    for (Collection<LogMessage> logs : newLogs.values()) {
                        count += logs.size();
                    }

                    return count;
                }
            };

            infoWorker.start();
        }
        catch(IllegalArgumentException iae) {
            //can happen on disconnect when auto refresh is on.
            logPane.getMsgProgressBar().setVisible(false);
        }
    }

    /**
     * Checks the validity of the given record's digital signature.
     *
     * @param record    a log record
     * @return validity of signature
     */
    private DigitalSignatureState checkDigitalSignature(SSGLogRecord record) {
        if (record instanceof AuditRecord) {
            final AuditRecord auditRecord = (AuditRecord) record;

            String signatureToVerify = auditRecord.getSignature();
            if (signatureToVerify == null || signatureToVerify.length() < 1) {
                return DigitalSignatureState.NONE;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-512");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("should not happen", e);
            }
            byte[] digestvalue = null;
            try {
                auditRecord.serializeSignableProperties(baos);
                digestvalue = digest.digest(baos.toByteArray());
            } catch (IOException e) {
                logger.log(Level.WARNING, "could not serialize audit record", e);
            } finally {
                try {
                    baos.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "error closing stream", e);
                }
            }

            // get the cert of the ssg we're connected to
            X509Certificate cert = TopComponents.getInstance().getSsgCert()[0];
            if (cert == null) return DigitalSignatureState.INVALID;
            PublicKey pub = cert.getPublicKey();
            if (pub == null) return DigitalSignatureState.INVALID;
            try {
                byte[] decodedSig = HexUtils.decodeBase64(signatureToVerify);
                Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                rsaCipher.init(Cipher.DECRYPT_MODE, pub);
                byte[] decrypteddata = rsaCipher.doFinal(decodedSig);
                if (Arrays.equals(decrypteddata, digestvalue)) {
                    return DigitalSignatureState.VALID;
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "cannot verify signature", e);
            }
            return DigitalSignatureState.INVALID;
        } else {
            // No digital signature for other record types.
            return DigitalSignatureState.NONE;
        }
    }
}
