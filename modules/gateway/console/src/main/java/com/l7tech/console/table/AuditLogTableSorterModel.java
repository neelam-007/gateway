package com.l7tech.console.table;

import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.LogPanel;
import com.l7tech.console.util.*;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gateway.common.audit.AuditRecordVerifier;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.cluster.GatewayStatus;
import com.l7tech.gateway.common.cluster.LogRequest;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * This class extends the <CODE>FilteredLogTableModel</CODE> class for providing the sorting functionality to the log display.
 */

public class AuditLogTableSorterModel extends FilteredDefaultTableModel {
    public static final int MAX_MESSAGE_BLOCK_SIZE = 1024;
    public static final int MAX_NUMBER_OF_LOG_MESSAGES = 131072;//2^17

    /** Validity state of a digital signature. */
    public enum DigitalSignatureUIState {
        // NOTE: elements are ordered for display sorting (i.e., from bad to good).
        /** Has signature but is invalid. */
        INVALID("invalid", "digital signature is invalid", MainWindow.RESOURCE_PATH + "/DigitalSignatureStateInvalid16.png"),

        /** No signature at all. */
        NONE("missing", "no digital signature", MainWindow.RESOURCE_PATH + "/DigitalSignatureStateNone16.png"),

        NOT_YET_VALIDATED("pending", "not yet validated", null),

        MANUAL_DOWNLOAD("manual download", "click to validate", MainWindow.RESOURCE_PATH + "/DigitalSignatureStateManualDownloaded16.png"),

        /** Has signature and is valid. */
        VALID("verified", "digital signature is verified", MainWindow.RESOURCE_PATH + "/DigitalSignatureStateValid16.png");

        private final String name;
        private final String description;
        private final Icon icon16;

        DigitalSignatureUIState(String name, String description, String icon16Path) {
            this.name = name;
            this.description = description;
            icon16 = (icon16Path != null) ? new ImageIcon(ImageCache.getInstance().getIcon(icon16Path)) : null;
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
        public Icon getIcon16() {
            return icon16;
        }
    }

    private static Logger logger = Logger.getLogger(AuditLogTableSorterModel.class.getName());

    private static final String DATE_FORMAT_PATTERN = "yyyyMMdd HH:mm:ss.SSS";

    /**
     * Synchronize all updates and read access not on the UI thread to data structures containing audit records.
     * rawLogCache -> contains all records and is can look up an audit by oid. (HashMap)
     * filteredLogCache -> contains all records with fast direct access (ArrayList)
     * sigValidationIndex -> allows validation to resume where it left off. Offer some performance benefits when the
     * number of record headers gets large.
     */
    private final Object auditHeaderLock = new Object();

    /**
     * Track the last signature validated. Index into filteredLogCache. Reset to 0 when filteredLogCache is modified.
     */
    private volatile int sigValidationIndex;

    /**
     * Holds a reference to the signature verification task. There should only ever be a single task running with
     * none queued. This reference can be used to cancel the task.
     * Do not read or write this variable without holding a lock on sigVerificationExecutor.
     */
    private volatile Future<?> validateFuture;
    private volatile boolean verifySignatures;
    private volatile Runnable validationNoLongerRunningCallback;
    private volatile Runnable validationStartedCallback;

    private final ExecutorService sigVerificationExecutor;

    private boolean ascending = false;
    private int columnToSort = LogPanel.LOG_TIMESTAMP_COLUMN_INDEX;

    /**
     * Holds the index into sorted data. When the view asks for a row, that row index can be used to look up
     * the actual log record index into the filteredLogCache.
     */
    private volatile Integer[] sortedData = null;
    private ClusterStatusAdmin clusterStatusAdmin = null;
    private AuditAdmin auditAdmin = null;
    private boolean displayingFromFile;
    private boolean truncated;
    private final AtomicReference<ClusterLogWorker> workerReference = new AtomicReference<ClusterLogWorker>();
    private TimeZone timeZone;

    private volatile Map<Long, AbstractAuditMessage> rawLogCache = new HashMap<Long, AbstractAuditMessage>();
    private volatile List<AbstractAuditMessage> filteredLogCache = new ArrayList<AbstractAuditMessage>();
    private Map<String, GatewayStatus> currentNodeList;

    /**
     * Creating a SimpleDateFormat shows up as largest hotspot in JProfiler. Instead just reuse instance.
     */
    private SimpleDateFormat sdf;

    /**
     * This flag is required to wrap the worker's cancel flag so that we can be notified to stop creating new workers even if the current worker has finished.
     */
    private boolean refreshCancelled = false;

    /**
     * Cancels log refresh.
     */
    public void cancelRefresh(){
        //flag refreshCancelled so no more workers will be constructed
        refreshCancelled = true;
        cancelWorker();
    }

    public AuditLogTableSorterModel(DefaultTableModel model) {
        this.displayingFromFile = false;

        setRealModel(model);

        sigVerificationExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        // the table cells are not editable
        return false;
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
    private void addLogs(Map<Long, ? extends AbstractAuditMessage> newLogs) {

        // add new logs to the cache
        if (newLogs.size() > 0) {
            //cancel job if running
            stopValidatingSignatures();

            synchronized (auditHeaderLock) {

                for( long key : newLogs.keySet()) {
                    if(rawLogCache.containsKey(key))  {

                        AbstractAuditMessage oldMsg = rawLogCache.get(key);
                        if(oldMsg instanceof AuditHeaderMessage){
                            String oldGuid = ((AuditHeaderMessage) oldMsg).getGuid();
                            byte[] sigDigest = null;
                            try {
                                sigDigest = oldMsg.getSignatureDigest();
                            } catch (IOException e) {
                                sigDigest = null ;
                            }
                            if(oldGuid==null || oldGuid.isEmpty() || sigDigest==null )
                                rawLogCache.put(key,newLogs.get(key));
                        }
                    }
                    else{
                        rawLogCache.put(key,newLogs.get(key));
                    }
                }
                filteredLogCache.clear();
                filteredLogCache.addAll(rawLogCache.values());
                //data structures have no order. For now simply start again at the start.
                sigValidationIndex = 0;
            }
            validateSignatures();
        }
    }

    private int getMaxSignaturesToValidate() {
        return Registry.getDefault().getAuditAdmin().getMaxDigestRecords();
    }

    /**
     * Only applies for Audits. Does not apply to Gateway Log Messages
     * @return Runnable signature verification runnable
     */
    private Runnable getSignatureVerificationRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                boolean interrupted = true;

                try {
                    final int maxNumberToProcess = getMaxSignaturesToValidate();
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Beginning validation of audit records");
                    }

                    while (sigValidationIndex < filteredLogCache.size()) {
                        if (Thread.currentThread().isInterrupted()) {
                            return;
                        }

                        final Map<String, AuditHeaderMessage> auditHeaders = new HashMap<String, AuditHeaderMessage>();
                        int index = sigValidationIndex;
                        int count = 0;
                        boolean fromPolicy = false;
                        synchronized (auditHeaderLock) {

                            while (index < filteredLogCache.size() && count < maxNumberToProcess) {
                                if (Thread.currentThread().isInterrupted()) {
                                    return;
                                }

                                final AuditHeaderMessage logMessage = (AuditHeaderMessage) filteredLogCache.get(index);
                                if (logMessage.getSignatureDigest() == null) {
                                    fromPolicy = fromPolicy || logMessage.getGuid()!=null;
                                    String id = fromPolicy ? logMessage.getGuid() : Long.toString(logMessage.getMsgNumber());
                                    auditHeaders.put(id , logMessage);
                                }
                                index++;
                                count++;
                            }
                        }

                        final List<String> auditRecordIds = new ArrayList<String>(auditHeaders.keySet());//keySet is not serializable
                        if (!auditRecordIds.isEmpty()) {
                            //update digests
                            final Map<String, byte[]> digestsForRecords = auditAdmin.getDigestsForAuditRecords(auditRecordIds, fromPolicy);
                            if (logger.isLoggable(Level.FINE)) {
                                logger.log(Level.FINE, "Validated " + digestsForRecords.size()+" records. Requested " + auditRecordIds.size()+" records.");
                            }

                            if (Thread.currentThread().isInterrupted()) {
                                return;
                            }

                            synchronized (auditHeaderLock) {
                                for (String auditRecordId : auditRecordIds) {
                                    if (Thread.currentThread().isInterrupted()) {
                                        return;
                                    }

                                    final AuditHeaderMessage auditHeader = auditHeaders.get(auditRecordId);
                                    if (digestsForRecords.containsKey(auditRecordId)) {
                                        auditHeader.setSignatureDigest(digestsForRecords.get(auditRecordId));
                                    } else {
                                        // audit record was skipped, possibly if message and audited message is too large.
                                        auditHeader.setDigestWasSkipped(true);
                                    }
                                }
                            }
                        }
                        sigValidationIndex = index;
                    }

                    interrupted = false;
                } catch (FindException e) {
                    logger.log(Level.WARNING, "Find Exception validating signatures: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Unexpected exception validating signatures: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                } finally {
                    if (interrupted) {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "Audit validation cancelled.");
                        }

                    } else {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "All audit records have been updated with the result of signature verification.");
                        }
                    }

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            validationNoLongerRunningCallback.run();
                        }
                    });
                }
            }
        };
    }

    /**
     * Called on the UI thread when validation has started.
     * @param runnable call back.
     */
    public void setValidationHasStartedCallback(Runnable runnable) {
        validationStartedCallback = runnable;
    }

    /**
     * Called when either there are no more records to validate, or the validation was cancelled.
     *
     * Will be called on the UI thread.
     * @param runnable call back.
     */
    public void setValidationNoLongerRunningCallback(Runnable runnable) {
        validationNoLongerRunningCallback = runnable;
    }

    public void setVerifySignatures(boolean validate) {
        verifySignatures = validate;

        if (validate) {
            validateSignatures();
        } else {
            stopValidatingSignatures();
        }

    }

    /**
     * Ensure signatures are being validated. If validation is not applicable (checkbox not checked or not viewing audits,
     * then calling this will not cause signatures to be validated.
     */
    private void validateSignatures() {
        if (!verifySignatures ) {
            return;
        }

        if (!isValidateJobRunning()) {
            //start job
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    validationStartedCallback.run();
                }
            });
            synchronized (sigVerificationExecutor) {
                validateFuture = sigVerificationExecutor.submit(getSignatureVerificationRunnable());
            }
        }
    }

    private boolean isValidateJobRunning() {
        synchronized (sigVerificationExecutor){
            boolean isRunning = validateFuture != null;

            //check if task is done or cancelled
            if (isRunning) {
                if (validateFuture.isDone() || validateFuture.isCancelled()) {
                    isRunning = false;
                }
            }

            return isRunning;
        }
    }

    private void stopValidatingSignatures() {
        if (isValidateJobRunning()) {
            synchronized (sigVerificationExecutor) {
                if (validateFuture != null) {
                    validateFuture.cancel(true);
                }
            }
        }
    }

    /**
      * Remove the logs of the non-exist node from the cache.
      *
      * @param nodeId  The Id of the node whose logs will be removed from the cache.
      */
     private void removeLogs(String nodeId) {
        Collection<AbstractAuditMessage> rawLogCacheCollection = rawLogCache.values();
        for(AbstractAuditMessage logMessage : rawLogCacheCollection){
            if(logMessage.getNodeId().equals(nodeId)){
                rawLogCache.remove(logMessage.getMsgNumber());
            }
        }
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public void setTimeZone( final TimeZone timeZone ) {
        this.timeZone = timeZone;
    }

    /**
     * Perform the data sorting.
     * //todo This should not be done on the UI thread.
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
        AbstractAuditMessage msg = getLogMessageAtRow(row);
        switch (col) {
            case LogPanel.LOG_SIGNATURE_COLUMN_INDEX:
                return getUISignatureState(msg);
            case LogPanel.LOG_MSG_NUMBER_COLUMN_INDEX:
                return msg.getMsgNumber();
            case LogPanel.LOG_NODE_NAME_COLUMN_INDEX:
                return msg.getNodeName();
            case LogPanel.LOG_TIMESTAMP_COLUMN_INDEX:
                SimpleDateFormat sdf = getDateFormatter();
                return sdf.format(new Date(msg.getTimestamp()) );
            case LogPanel.LOG_SEVERITY_COLUMN_INDEX:
                return msg.getSeverity();
            case LogPanel.LOG_MSG_DETAILS_COLUMN_INDEX:
                return msg.getMsgDetails();
            case LogPanel.LOG_REQUEST_ID_COLUMN_INDEX:
                return msg.getReqId();
            case LogPanel.LOG_NODE_ID_COLUMN_INDEX:
                return msg.getNodeId();
            case LogPanel.LOG_SERVICE_COLUMN_INDEX:
                return msg.getServiceName();
            default:
                throw new IllegalArgumentException("Bad Column");
        }
    }

    public AbstractAuditMessage getLogMessageAtRow(int row) {
        return filteredLogCache.get(sortedData[row]);
    }

    private DigitalSignatureUIState getUISignatureState(AbstractAuditMessage msg) {
        try {
            return compareSignatureDigests(msg);
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

            Object elementA = null;
            Object elementB = null;

            AbstractAuditMessage logMsgA = filteredLogCache.get(a);
            AbstractAuditMessage logMsgB = filteredLogCache.get(b);

            switch (column) {
                case LogPanel.LOG_SIGNATURE_COLUMN_INDEX:
                    final DigitalSignatureUIState stateA = getUISignatureState(logMsgA);
                    final DigitalSignatureUIState stateB = getUISignatureState(logMsgB);
                    return (ascending ? 1 : -1) * stateA.compareTo(stateB);
                case LogPanel.LOG_MSG_NUMBER_COLUMN_INDEX:
                    elementA = logMsgA.getMsgNumber();
                    elementB = logMsgB.getMsgNumber();
                    break;
                case LogPanel.LOG_NODE_NAME_COLUMN_INDEX:
                    elementA = logMsgA.getNodeName();
                    elementB = logMsgB.getNodeName();
                    break;
                case LogPanel.LOG_TIMESTAMP_COLUMN_INDEX:
                    elementA = logMsgA.getTimestamp();
                    elementB = logMsgB.getTimestamp();
                    break;
                case LogPanel.LOG_SEVERITY_COLUMN_INDEX:
                    elementA = logMsgA.getSeverity();
                    elementB = logMsgB.getSeverity();
                    break;
                case LogPanel.LOG_MSG_DETAILS_COLUMN_INDEX:
                    elementA = logMsgA.getMsgDetails();
                    elementB = logMsgB.getMsgDetails();
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
                default:
                    logger.warning("Bad Statistics Table Column: " + column);
                    break;
            }

            // Treat empty strains like nulls
            if (elementA != null && (elementA instanceof String) && ((String) elementA).length() == 0) {
                elementA = null;
            }
            if (elementB != null && (elementB instanceof String) && ((String) elementB).length() == 0) {
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
                    if (elementA instanceof Long)
                        return ((Long)elementA).compareTo((Long)elementB);
                    else if (elementA instanceof Integer)
                        return ((Integer)elementA).compareTo((Integer)elementB);
                    else
                        return ((String)elementA).compareToIgnoreCase((String) elementB);
                } else {
                    if (elementB instanceof Long)
                        return ((Long)elementB).compareTo((Long)elementA);
                    else if (elementB instanceof Integer)
                        return ((Integer)elementB).compareTo((Integer)elementA);
                    else
                        return ((String)elementB).compareToIgnoreCase((String) elementA);
                }
            }
        }
    }

    private SimpleDateFormat getDateFormatter() {
        if (sdf == null) {
            sdf = new SimpleDateFormat(DATE_FORMAT_PATTERN);
            TimeZone timeZone = this.timeZone;
            if (timeZone != null) sdf.setTimeZone(timeZone);
        }
        return sdf;
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

        sigVerificationExecutor.shutdown();

        clusterStatusAdmin = null;
        auditAdmin = null;
    }

    public int getDelay() {
        int delay = 3;

        if (auditAdmin != null) {
            delay = auditAdmin.getSystemLogRefresh();
        }

        return delay;
    }

    /**
     * Clear all caches.
     */
    public void clearLogCache() {
        cancelWorker();

        synchronized (sigVerificationExecutor) {
            if (validateFuture != null) {
                validateFuture.cancel(true);
            }
        }

        synchronized (auditHeaderLock) {
            rawLogCache = new HashMap<Long,AbstractAuditMessage>();
            sigValidationIndex = 0;
            filteredLogCache = new ArrayList<AbstractAuditMessage>();
        }
        currentNodeList = new HashMap<String, GatewayStatus>();
        sortedData = new Integer[0];
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
            Map<String, Long> nodeToHighestId = new HashMap<String, Long>();
            Collection<AbstractAuditMessage> rawLogCacheCollection = rawLogCache.values();

            // new results can only be obtained for known nodes in the cluster.
            for (String nodeId : currentNodeList.keySet()) {
                nodeToHighestId.put(nodeId, -1L);
            }

            for (AbstractAuditMessage logMessage : rawLogCacheCollection) {
                //protect against records from nodes not in the cluster anymore
                if (nodeToHighestId.containsKey(logMessage.getNodeId())) {
                    final Long nodesHighest = nodeToHighestId.get(logMessage.getNodeId());
                    if (logMessage.getMsgNumber() > nodesHighest) {
                        nodeToHighestId.put(logMessage.getNodeId(), logMessage.getMsgNumber());
                    }
                }
            }

            for (Map.Entry<String, Long> entry : nodeToHighestId.entrySet()) {
                //some of entry.getValue() may be -1, this just means we are yet to see a record from the node.
                logRequest.setStartMsgNumberForNode(entry.getKey(), entry.getValue());
            }
        }
        doRefreshLogs(logPane, logRequest, restartTimer, 0);
    }

    /**
     * Set the log/audit data to be displayed.
     *
     * @param logPane   The object reference to the LogPanel.
     * @param logs the data list.
     */
    public void setLogs(final LogPanel logPane, final Map<Long, ? extends AbstractAuditMessage> logs) {
        logger.info("Importing "+/*count*/logs.size()+"audit records.");

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
     * Retrieve logs from the cluster.
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

        if( refreshCancelled ){
            hideProgressAndRestart(logPane, restartTimer);
            return;
        }

        try {
            // create a worker thread to retrieve the cluster info
            //Record current row before model is potentially modified
            //row is based on audit record identity and not position number
            final String msgNumSelected = logPane.getSelectedMsgNumber();
            final ClusterLogWorker infoWorker = new ClusterLogWorker(
                    clusterStatusAdmin,
                    auditAdmin,
                    //currentNodeList,
                    logRequest) {
                @Override
                public void finished() {
                    //todo finished() is called on the UI thread. No expensive tasks should be done here. Sorting in particular should be moved into construct().
                    // Note: the get() operation is a blocking operation.
                    // get() will never block based on construct()'s implementation.
                    if ( !isCancelled() && this.get() != null) {
                        Map<Long, AuditHeaderMessage> newLogs = getNewLogs();
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
                            // sort the logs
                            sortData(columnToSort, false);

                            // populate the change to the display

                            // The line "realModel.fireTableDataChanged()" has been deleted to fix bug 10085.
                            // If the table content is changed, then the index of the audit in the table associated with msgNumSelected will be changed.
                            // Then, the line below, "logPane.setSelectedRow(msgNumSelected)" wil eventually invoke DefaultListSelectionModel.fireValueChanged().
                            // Thus, there is no need to call realModel.fireTableDataChanged() again.  The table change event now really depends on the table content change.
                            // If no content change, then no event dispatched.  It turns out ListSelectionListener in LogPanel will not make unnecessary calls on updateMsgDetails()
                            // to frequently update the details pane.  This fix will probably improve the performance of the audit viewer a bit.

                            logPane.updateMsgTotal();
                            logPane.setSelectedRow(msgNumSelected);
                        }

                        logPane.updateTimeStamp(getCurrentClusterSystemTime());

                        final LogRequest unfilledRequest = getUnfilledRequest();

                        // if there unfilled requests
                        final int total = count + logCount;
                        if (unfilledRequest != null && total < MAX_NUMBER_OF_LOG_MESSAGES) {
                            logPane.getMsgProgressBar().setVisible(true);
                            logPane.getCancelButton().setEnabled(true);
                            SwingUtilities.invokeLater(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            doRefreshLogs(logPane, unfilledRequest, restartTimer, total);
                                        }
                                    });

                        } else {
                            truncated = unfilledRequest != null;
                            if (truncated) {
                                logPane.updateMsgTotal();
                            }
                            hideProgressAndRestart(logPane, restartTimer);
                        }
                    } else {
                        hideProgressAndRestart(logPane, restartTimer);
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

    private void hideProgressAndRestart(final LogPanel logPane, final boolean restartTimer) {
        logPane.getMsgProgressBar().setVisible(false);
        logPane.getCancelButton().setEnabled(false);
        logPane.getSearchButton().setEnabled(true);
        if (restartTimer) {
            logPane.getLogsRefreshTimer().start();
        }
        refreshCancelled = false;
    }

    private DigitalSignatureUIState compareSignatureDigests( AbstractAuditMessage msg ) throws IOException {
        final String signatureToVerify = msg.getSignature();
        final byte[] digestValue =  msg.getSignatureDigest();

        if (signatureToVerify != null && digestValue == null) {
            if (msg instanceof AuditHeaderMessage ) {
                AuditHeaderMessage auditHeader = (AuditHeaderMessage) msg;
                if (auditHeader.isDigestWasSkipped()) {
                    return DigitalSignatureUIState.MANUAL_DOWNLOAD;
                }
            }

            return DigitalSignatureUIState.NOT_YET_VALIDATED;
        }

        if (signatureToVerify == null || signatureToVerify.length() < 1 ) {
            return DigitalSignatureUIState.NONE;
        }

        // get the audit signing key's cert.  If no audit signing key designated, use default SSL key's cert instead
        final X509Certificate cert;
        if (Registry.getDefault().isAdminContextPresent()) {
            cert = TopComponents.getInstance().getSsgAuditSigningCert();
        } else {
            return DigitalSignatureUIState.NONE;
        }

        try {
            boolean result = new AuditRecordVerifier(cert).verifySignatureOfDigest(signatureToVerify, digestValue);
            return result ? DigitalSignatureUIState.VALID : DigitalSignatureUIState.INVALID;
        } catch (Exception e) {
            logger.log(Level.WARNING, "cannot verify signature", e);
        }
        return DigitalSignatureUIState.INVALID;
    }
}