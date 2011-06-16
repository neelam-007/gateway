package com.l7tech.console.util;

import com.l7tech.gateway.common.cluster.LogRequest;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.cluster.GatewayStatus;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gui.util.SwingWorker;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gateway.common.audit.AuditSearchCriteria;
import com.l7tech.gateway.common.audit.AuditRecordHeader;
import com.l7tech.console.table.AuditLogTableSorterModel;
import com.l7tech.gateway.common.logging.GenericLogAdmin;
import com.l7tech.gateway.common.logging.SSGLogRecord;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * This class performs the log/audit header retrieval from the cluster. The work is carried out on a separate thread.
 * Upon completion of the data retrieval, the Log Browser window is updated by the Swing thread.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class ClusterLogWorker extends SwingWorker {

    private final static Logger logger = Logger.getLogger(ClusterLogWorker.class.getName());

    private final ClusterStatusAdmin clusterStatusService;
    private final AuditAdmin logService;
    private final int logType;
    private Map<String, GatewayStatus> newNodeList;
    private LogRequest logRequest;
    private final Map<Long, LogMessage> retrievedLogs = new HashMap<Long, LogMessage>();
    private java.util.Date currentClusterSystemTime = null;
    private final AtomicBoolean cancelled;

    /**
     * Create a new cluster log worker.
     * <p/>
     * @param clusterStatusService  An object reference to the <CODE>ClusterStatusAdmin</CODE>service
     *
     * @param logService An object reference to the <CODE>GenericLogAdmin</CODE> service
     * @param logType    the type (log or audit)
     * @param logRequest  A request for retrieving logs.
     */
    public ClusterLogWorker(final ClusterStatusAdmin clusterStatusService,
                            final AuditAdmin logService,
                            final int logType,
                            final LogRequest logRequest) {
        if (logService == null || clusterStatusService == null) {
            throw new IllegalArgumentException();
        }

        this.clusterStatusService = clusterStatusService;
        this.logService = logService;
        this.logType = logType;
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
     * Return the logs newly retrieved from the cluster.
     *
     * @return new logs
     */
    public Map<Long, LogMessage> getNewLogs(){
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

            SSGLogRecord[] rawLogs;
            List<AuditRecordHeader> rawHeaders;
            if (logRequest != null) {
                Map<Long, LogMessage> newLogs = new HashMap<Long, LogMessage>();

                try {
                    LogMessage logMessage;
                    switch (logType) {
                        case GenericLogAdmin.TYPE_AUDIT:
                            AuditSearchCriteria asc = new AuditSearchCriteria.Builder(logRequest).
                                nodeId(nodeNameIdMap.get(logRequest.getNodeName())).
                                maxRecords(AuditLogTableSorterModel.MAX_MESSAGE_BLOCK_SIZE).build();

                            logger.finer("Start time to grab data:: " + new Date(System.currentTimeMillis()));
                            rawHeaders = logService.findHeaders(asc);
                            logger.finer("End time of grab data:: " + new Date(System.currentTimeMillis()));
                            if (!isCancelled() && rawHeaders.size() > 0) {
                                long oldest = Long.MAX_VALUE;

                                Map<String, Long> nodeToLowestId = new HashMap<String, Long>();
                                for (String nodeId : newNodeList.keySet()) {
                                    nodeToLowestId.put(nodeId, Long.MAX_VALUE);
                                }

                                for (int j = 0; j < (rawHeaders.size()) && (retrievedLogs.size() < AuditLogTableSorterModel.MAX_NUMBER_OF_LOG_MESSAGES); j++) {
                                    AuditRecordHeader header = rawHeaders.get(j);
                                    logMessage = new AuditHeaderLogMessage(header);

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
                                    oldest = -1;
                                }
                                logRequest.setEndMsgDate(new Date(oldest + 1)); // end date is exclusive

                            } else {
                                //we are done
                                logRequest = null;
                            }
                            break;
                        case GenericLogAdmin.TYPE_LOG:
                            rawLogs = logService.getSystemLog(logRequest.getNodeName(),
                                    logRequest.getStartMsgNumber(),
                                    logRequest.getEndMsgNumber(),
                                    logRequest.getStartMsgDate(),
                                    logRequest.getEndMsgDate(),
                                    AuditLogTableSorterModel.MAX_MESSAGE_BLOCK_SIZE);

                            if (!isCancelled() && rawLogs.length > 0) {
                                long lowest = -1;
                                long oldest = -1;
                                GatewayStatus nodeStatus;
                                for (int j = 0; j < (rawLogs.length) && (newLogs.size() < AuditLogTableSorterModel.MAX_NUMBER_OF_LOG_MESSAGES); j++) {
                                    logMessage = new LogRecordLogMessage(rawLogs[j]);
                                    nodeStatus = newNodeList.get(logMessage.getNodeId());
                                    if (nodeStatus != null) { // do not add log messages for nodes that are no longer in the cluster
                                        if (j == 0) {
                                            lowest = logMessage.getMsgNumber();
                                            oldest = logMessage.getTimestamp();
                                        } else if (lowest > logMessage.getMsgNumber()) {
                                            lowest = logMessage.getMsgNumber();
                                            oldest = logMessage.getTimestamp();
                                        }
                                        logMessage.setNodeName(nodeStatus.getName());
                                        newLogs.put(logMessage.getMsgNumber(), logMessage);
                                    }
                                }
                                logRequest.setStartMsgNumber(lowest);
                                logRequest.setEndMsgDate(new Date(oldest + 1)); // end date is exclusive
                            } else {
                                //we are done
                                logRequest = null;
                            }
                            break;
                    }
                } catch (FindException e) {
                    logger.log(Level.SEVERE, "Unable to retrieve logs from server", e);
                }

                if (newLogs.size() > 0) {
                    retrievedLogs.putAll(newLogs);
                    if ( logRequest != null )
                        logRequest.addRetrievedLogCount(newLogs.size());
                }

                if (this.logType == GenericLogAdmin.TYPE_LOG) {
                    logRequest = null;
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

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void cancel() {
        cancelled.set( true );
    }
}