package com.l7tech.console.util;

import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.cluster.GatewayStatus;
import com.l7tech.cluster.LogRequest;
import com.l7tech.common.gui.util.SwingWorker;
import com.l7tech.console.table.FilteredLogTableModel;
import com.l7tech.logging.GenericLogAdmin;
import com.l7tech.logging.LogMessage;
import com.l7tech.logging.SSGLogRecord;
import com.l7tech.objectmodel.FindException;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * This class performs the log retrieval from the cluster. The work is carried out on a separate thread.
 * Upon completion of the data retrieval, the Log Browser window is updated by the Swing thread.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class ClusterLogWorker extends SwingWorker {

    private ClusterStatusAdmin clusterStatusService = null;
    private GenericLogAdmin logService = null;
    private int logType;
    private String nodeId;
    private Map newNodeList;
    private Map currentNodeList;
    private List requests;
    private Map retrievedLogs;
    private java.util.Date currentClusterSystemTime = null;
    private final Date startDate;
    private final Date endDate;
    static Logger logger = Logger.getLogger(ClusterLogWorker.class.getName());

    /**
     * Create a new cluster log worker.
     *
     * @param clusterStatusService  An object reference to the <CODE>ClusterStatusAdmin</CODE>service
     * @param logService  An object reference to the <CODE>GenericLogAdmin</CODE> service
     * @param logType the type (log or audit)
     * @param startDate the earliest date to fetch
     * @param endDate the latest date to fetch
     * @param currentNodeList  A list of nodes obtained from the last retrieval
     * @param requests  A list of requests for retrieving logs. One request per node.
     */
    public ClusterLogWorker(ClusterStatusAdmin clusterStatusService,
                            GenericLogAdmin logService,
                            int logType,
                            String nodeId,
                            Date startDate,
                            Date endDate,
                            Map currentNodeList,
                            List requests) {
        this.clusterStatusService = clusterStatusService;
        this.logService = logService;
        this.logType = logType;
        this.nodeId = nodeId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.currentNodeList = currentNodeList;

        if (currentNodeList == null || logService == null || clusterStatusService == null) {
            throw new IllegalArgumentException();
        }

        this.requests = requests;

        retrievedLogs = new HashMap();
    }

    /**
     * Return the updated node list.
     *
     * @return  The list of nodes obtained from the lateset retrieval.
     */
    public Map getNewNodeList() {
        return newNodeList;
    }

    /**
     * Return the logs newly retrieved from the cluster.
     *
     * @return  new logs
     */
    public Map getNewLogs() {
        return retrievedLogs;
    }

    /**
     * Return the list of unfilled requests.
     *
     * @return  the list of unfilled requests.
     */
    public List getUnfilledRequest() {
        return requests;
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
     * Consturct the value. This function performs the actual work of retrieving logs.
     *
     * @return  Object  An object with the value constructed by this function.
     */
    public Object construct() {

        // create a new empty node list
        newNodeList = new HashMap();

        // retrieve node status
        ClusterNodeInfo[] cluster = new ClusterNodeInfo[0];

        try {
            cluster = clusterStatusService.getClusterStatus();
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to find cluster status from server", e);
        } catch (RemoteException e) {
            throw new RuntimeException("Remote exception when retrieving cluster status from server", e);
        }

        if (cluster == null) {
            return null;
        }

        for (int i = 0; i < cluster.length; i++) {

            GatewayStatus nodeStatus = new GatewayStatus(cluster[i]);
            String clusterNodeId = nodeStatus.getNodeId();
            if (clusterNodeId != null) {
                if (currentNodeList.get(clusterNodeId) == null
                 && (nodeId==null || nodeId.equals(clusterNodeId))) {
                    // add the new node to the request array with the startMsgNumber and endMsgNumber set to -1
                    requests.add(new LogRequest(nodeStatus.getNodeId(), -1, -1, startDate, endDate));
                }

                // add the node to the new list
                newNodeList.put(nodeStatus.getNodeId(), nodeStatus);
            }
        }

        SSGLogRecord[] rawLogs = new SSGLogRecord[]{};
        List requestCompleted = new ArrayList();

        if (requests.size() > 0) {

            for (int i = 0; i < requests.size(); i++) {

                Collection newLogs = new LinkedHashSet();
                LogRequest logRequest = (LogRequest) requests.get(i);

                try {

                    rawLogs = new SSGLogRecord[]{};

                    //System.out.println("Calling getSystemLog with start#='"+logRequest.getStartMsgNumber()+"', end#='"+logRequest.getEndMsgNumber()+"', startDate='"+logRequest.getStartMsgDate()+"', endDate='"+logRequest.getEndMsgDate()+"'.");
                    rawLogs = logService.getSystemLog(logRequest.getNodeId(),
                            this.logType,
                            logRequest.getStartMsgNumber(),
                            logRequest.getEndMsgNumber(),
                            logRequest.getStartMsgDate(),
                            logRequest.getEndMsgDate(),
                            FilteredLogTableModel.MAX_MESSAGE_BLOCK_SIZE);

                    //System.out.println("startMsgNumber: " + logRequest.getStartMsgNumber());
                    //System.out.println("endMsgNumber: " + logRequest.getEndMsgNumber());
                    //System.out.println("NodeId: " + logRequest.getNodeId() + ", Number of logs received: " + rawLogs.length);

                    LogMessage logMsg = null;

                    if (rawLogs.length > 0) {
                        long lowest = -1;
                        for (int j = 0; j < (rawLogs.length) && (newLogs.size() < FilteredLogTableModel.MAX_NUMBER_OF_LOG_MESSGAES); j++) {

                            logMsg = new LogMessage(rawLogs[j]);
                            if (j == 0) lowest = logMsg.getMsgNumber();
                            else if (lowest > logMsg.getMsgNumber()) lowest = logMsg.getMsgNumber();
                            newLogs.add(logMsg);
                        }
                        logRequest.setStartMsgNumber(lowest);

                    }
                } catch (RemoteException e) {
                    throw new RuntimeException("Unable to retrieve logs from server",e);
                } catch (FindException e) {
                    logger.log(Level.SEVERE, "Unable to retrieve logs from server", e);
                }

                if (newLogs.size() > 0) {
                    retrievedLogs.put(logRequest.getNodeId(), newLogs);
                    logRequest.addRetrievedLogCount(newLogs.size());
                }

                if (this.logType == GenericLogAdmin.TYPE_LOG || 
                    rawLogs.length < FilteredLogTableModel.MAX_MESSAGE_BLOCK_SIZE ||
                    logRequest.getRetrievedLogCount() >= FilteredLogTableModel.MAX_NUMBER_OF_LOG_MESSGAES) {

                    requestCompleted.add(logRequest);
                }
            }
        }

        for (int i = 0; i < requestCompleted.size(); i++) {
            LogRequest logRequest = (LogRequest) requestCompleted.get(i);

            // remove the request from the list
            requests.remove(logRequest);
        }

        try {
            currentClusterSystemTime = clusterStatusService.getCurrentClusterSystemTime();
        } catch (RemoteException e) {
            throw new RuntimeException("Remote exception when retrieving cluster status from server",e);
        }

        if (currentClusterSystemTime == null) {
            return null;
        } else {
            return newNodeList;
        }

    }
}

