package com.l7tech.console.util;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.cluster.LogRequest;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.cluster.GatewayStatus;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gui.util.SwingWorker;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gateway.common.audit.AuditSearchCriteria;
import com.l7tech.gateway.common.audit.AuditRecordHeader;
import com.l7tech.console.table.AuditLogTableSorterModel;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * This class performs the audit header retrieval from the cluster.
 *
 * The work is carried out on a separate thread. Upon completion of the data
 * retrieval, the Log Browser window is updated by the Swing thread.
 */
public class ClusterLogWorker extends SwingWorker {

    private final static Logger logger = Logger.getLogger(ClusterLogWorker.class.getName());

    private static final String PROP_PREFIX = "com.l7tech.console";
    private static final long DELAY_INITIAL = ConfigFactory.getLongProperty( PROP_PREFIX + ".auditSearch.serverSideDelay.initial", 50L );
    private static final long DELAY_CAP = ConfigFactory.getLongProperty(PROP_PREFIX + ".auditSearch.serverSideDelay.maximum", 5000L);
    private static final double DELAY_MULTIPLIER = SyspropUtil.getDouble(PROP_PREFIX + ".auditSearch.serverSideDelay.multiplier", 1.6);

    private final ClusterStatusAdmin clusterStatusService;
    private final AuditAdmin logService;
    private Map<String, GatewayStatus> newNodeList;
    private LogRequest logRequest;
    private final Map<Long, AuditHeaderMessage> retrievedLogs = new HashMap<Long, AuditHeaderMessage>();
    private java.util.Date currentClusterSystemTime = null;
    private final AtomicBoolean cancelled;

    /**
     * Create a new cluster log worker.
     * <p/>
     * @param clusterStatusService  An object reference to the <CODE>ClusterStatusAdmin</CODE>service
     *
     * @param logService An object reference to the <CODE>GenericLogAdmin</CODE> service
     * @param logRequest  A request for retrieving logs.
     */
    public ClusterLogWorker(final ClusterStatusAdmin clusterStatusService,
                            final AuditAdmin logService,
                            final LogRequest logRequest) {
        if (logService == null || clusterStatusService == null) {
            throw new IllegalArgumentException();
        }

        this.clusterStatusService = clusterStatusService;
        this.logService = logService;
        this.logRequest = logRequest;
        this.cancelled = new AtomicBoolean(false);
    }

    /**
     * Return the updated node list.
     *
     * @return The list of nodes obtained from the lateset retrieval.
     */
    public Map<String, GatewayStatus> getNewNodeList() {
        return newNodeList;
    }

    /**
     * Return the audit header log messages newly retrieved from the cluster.
     *
     * @return new logs
     */
    public Map<Long, AuditHeaderMessage> getNewLogs(){
        return retrievedLogs;
    }

    /**
     * Return a LogRequest if the construct() method found results. When a query returns no results this will be null
     * and signals that there is no more work to do for the given search criteria.
     *
     * @return a LogRequest if there are possibly more search results as a result of the current query having returned
     * results, null otherwise.
     */
    public LogRequest getUnfilledRequest() {
        return logRequest;
    }

    /**
     * Get Cluster's current system time.
     *
     * @return java.util.Date  The current system time of the cluster.
     */
    public java.util.Date getCurrentClusterSystemTime() {
        return currentClusterSystemTime;
    }

    /**
     * Construct the value. This function performs the actual work of retrieving logs.
     *
     * @return Object  An object with the value constructed by this function.
     */
    @Override
    public Object construct() {

        Map<String, String> nodeNameIdMap = new HashMap<String, String>();
        // create a new empty node list
        newNodeList = new HashMap<String, GatewayStatus>();
        try {
            // retrieve node status
            final ClusterNodeInfo[] clusterNodes;

            try {
                clusterNodes = clusterStatusService.getClusterStatus();
            } catch (FindException e) {
                logger.log(Level.WARNING, "Unable to find cluster status from server", e);
                return null;
            }

            if (isCancelled()) {
                return null;
            }

            for (ClusterNodeInfo nodeInfo : clusterNodes) {
                GatewayStatus nodeStatus = new GatewayStatus(nodeInfo);
                String clusterNodeId = nodeStatus.getNodeId();
                if (clusterNodeId != null) {

                    // add the node to the new list
                    newNodeList.put(nodeStatus.getNodeId(), nodeStatus);
                    nodeNameIdMap.put(nodeStatus.getName(), nodeStatus.getNodeId());
                }
            }

            AuditRecordHeader[] rawHeaders;
            if (logRequest != null) {
                Map<Long, AuditHeaderMessage> newLogs = new HashMap<Long, AuditHeaderMessage>();

                try {
                    AuditHeaderMessage logMessage;

                    AuditSearchCriteria asc = new AuditSearchCriteria.Builder(logRequest).
                        nodeId(nodeNameIdMap.get(logRequest.getNodeName())).
                        maxRecords(AuditLogTableSorterModel.MAX_MESSAGE_BLOCK_SIZE).build();

                    logger.finer("Start time to grab data:: " + new Date(System.currentTimeMillis()));
                    rawHeaders = getLogs(asc);
                    logger.finer("End time of grab data:: " + new Date(System.currentTimeMillis()));
                    if (!isCancelled() && rawHeaders.length > 0) {
                        long oldest = Long.MAX_VALUE;

                        Map<String, Long> nodeToLowestId = new HashMap<String, Long>();
                        for (String nodeId : newNodeList.keySet()) {
                            nodeToLowestId.put(nodeId, Long.MAX_VALUE);
                        }

                        for (int j = 0; j < (rawHeaders.length) && (retrievedLogs.size() < AuditLogTableSorterModel.MAX_NUMBER_OF_LOG_MESSAGES); j++) {
                            AuditRecordHeader header = rawHeaders[j];
                            logMessage = new AuditHeaderMessage(header);

                            final GatewayStatus nodeStatus = newNodeList.get(header.getNodeId());
                            if (nodeStatus != null) { // do not add log messages for nodes that are no longer in the cluster
                                if (nodeToLowestId.containsKey(header.getNodeId())) {
                                    //based on nodeStatus != null this if is always true

                                    //track lowest object id seen from each node
                                    final Long lowest = nodeToLowestId.get(header.getNodeId());
                                    if (header.getOid() < lowest ) {
                                        nodeToLowestId.put(header.getNodeId(), header.getOid());
                                    }

                                    //track oldest across a cluster
                                    if (header.getTimestamp() < oldest ) {
                                        oldest = header.getTimestamp();
                                    }
                                }

                                logMessage.setNodeName(nodeStatus.getName());
                                newLogs.put(logMessage.getMsgNumber(), logMessage);
                            }
                        }

                        for (Map.Entry<String, Long> entry : nodeToLowestId.entrySet()) {
                            //for each node - we only want records with an object smaller than entry.getValue()
                            logRequest.setEndMsgNumberForNode(entry.getKey(), entry.getValue());
                        }
                        if (oldest == Long.MAX_VALUE) {
                            //should never happen.
                            oldest = -1L;
                        }
                        logRequest.setEndMsgDate(new Date(oldest - 1L)); // end date is exclusive

                    } else {
                        //we are done
                        logRequest = null;
                    }
                } catch (FindException e) {
                    logger.log(Level.SEVERE, "Unable to retrieve audits from server:" + e.getMessage(), ExceptionUtils.getDebugException(e));
                    logRequest = null;
                } catch (InterruptedException e) {
                    logger.log(Level.SEVERE, "Unable to retrieve audits from server:" + e.getMessage(), ExceptionUtils.getDebugException(e));
                    logRequest = null;
                } catch (AsyncAdminMethods.JobStillActiveException e) {
                    logger.log(Level.SEVERE, "Unable to retrieve audits from server:" + e.getMessage(), ExceptionUtils.getDebugException(e));
                    logRequest = null;
                } catch (AsyncAdminMethods.UnknownJobException e) {
                    logger.log(Level.SEVERE, "Unable to retrieve audits from server:" + e.getMessage(), ExceptionUtils.getDebugException(e));
                    logRequest = null;
                }

                if (newLogs.size() > 0) {
                    retrievedLogs.putAll(newLogs);
                    if ( logRequest != null )
                        logRequest.addRetrievedLogCount(newLogs.size());
                }
            }
            currentClusterSystemTime = isCancelled() ? null : clusterStatusService.getCurrentClusterSystemTime();

            if ( currentClusterSystemTime == null ) {
                return null;
            } else {
                return newNodeList;
            }
        } catch ( RuntimeException re ) {
            if ( isCancelled() ) { // eat any exceptions if we've been cancelled
                logger.log(Level.INFO, "Ignoring error for cancelled operation ''{0}''.", ExceptionUtils.getMessage(re));
                logRequest = null;
                return null;
            } else {
                throw re;
            }
        }
    }

    /**
     * Gets logs from gateway
     *
     * @param asc
     * @return List of retrieved audits, returns empty list if job is cancelled
     * @throws FindException
     * @throws InterruptedException
     * @throws AsyncAdminMethods.UnknownJobException
     * @throws AsyncAdminMethods.JobStillActiveException
     */
    private AuditRecordHeader[] getLogs(AuditSearchCriteria asc) throws FindException, InterruptedException, AsyncAdminMethods.UnknownJobException, AsyncAdminMethods.JobStillActiveException {
        AsyncAdminMethods.JobId<AuditRecordHeader[]> jobId = logService.findHeaders(asc);
        double delay = DELAY_INITIAL;
        Thread.sleep((long)delay);
        while( true ) {
            if(cancelled.get()) {
                logService.cancelJob(jobId, true);
                return new AuditRecordHeader[0];
            }
            final String status = logService.getJobStatus( jobId );
            if ( status == null ) {
                throw new  AsyncAdminMethods.UnknownJobException( "Unknown job" );
            } else if ( !status.startsWith( "a" ) ) {
                final AsyncAdminMethods.JobResult<AuditRecordHeader[]> jobResult = logService.getJobResult( jobId );
                if ( jobResult.result != null ) {
                    return  jobResult.result ;
                } else {
                    throw new FindException(jobResult.throwableMessage) ;
                }
            }
            delay = delay >= DELAY_CAP ? DELAY_CAP : delay * DELAY_MULTIPLIER;
            Thread.sleep((long)delay);
        }
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void cancel() {
        cancelled.set( true );
    }
}