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
import java.util.Hashtable;
import java.util.Vector;
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
    private Hashtable newNodeList;
    private Hashtable currentNodeList;
    private Vector requests;
    private Hashtable retrievedLogs;
    private boolean remoteExceptionCaught;
    private java.util.Date currentClusterSystemTime = null;
    static Logger logger = Logger.getLogger(ClusterLogWorker.class.getName());

    /**
     * A constructor
     * @param clusterStatusService  An object reference to the <CODE>ClusterStatusAdmin</CODE>service
     * @param logService  An object reference to the <CODE>GenericLogAdmin</CODE> service
     * @param currentNodeList  A list of nodes obtained from the last retrieval
     * @param requests  A list of requests for retrieving logs. One request per node.
     */
    public ClusterLogWorker(ClusterStatusAdmin clusterStatusService, GenericLogAdmin logService, Hashtable currentNodeList, Vector requests) {
        this.clusterStatusService = clusterStatusService;
        this.currentNodeList = currentNodeList;
        this.logService = logService;
        this.requests = requests;

        remoteExceptionCaught = false;
        retrievedLogs = new Hashtable();
    }

    /**
     * Return the updated node list.
     *
     * @return  The list of nodes obtained from the lateset retrieval.
     */
    public Hashtable getNewNodeList() {
        return newNodeList;
    }

    /**
     * Return the state indicating whether a remote exception is encountered or not.
     *
     * @return  true if remote exception is caught, false otherwise.
     */
    public boolean isRemoteExceptionCaught() {
        return remoteExceptionCaught;
    }

    /**
     * Return the logs newly retrieved from the cluster.
     *
     * @return  new logs
     */
    public Hashtable getNewLogs() {
        return retrievedLogs;
    }

    /**
     * Return the list of unfilled requests.
     *
     * @return  the list of unfilled requests.
     */
    public Vector getUnfilledRequest() {
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
        newNodeList = new Hashtable();

        if (clusterStatusService == null) {
            logger.log(Level.SEVERE, "ClusterServiceAdmin reference is NULL");
            return newNodeList;
        }

        if (currentNodeList == null) {
            throw new RuntimeException("The current node list is NULL");
        }

        // retrieve node status
        ClusterNodeInfo[] cluster = new ClusterNodeInfo[0];

        try {
            cluster = clusterStatusService.getClusterStatus();
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to find cluster status from server", e);
        } catch (RemoteException e) {
            remoteExceptionCaught = true;
            logger.log(Level.SEVERE, "Remote exception when retrieving cluster status from server", e);
        }

        if (cluster == null) {
            return null;
        }

        for (int i = 0; i < cluster.length; i++) {

            GatewayStatus nodeStatus = new GatewayStatus(cluster[i]);
            String nodeId = nodeStatus.getNodeId();
            if (nodeId != null) {
                if (currentNodeList.get(nodeId) == null) {
                    // add the new node to the request array with the startMsgNumber and endMsgNumber set to -1
                    requests.add(new LogRequest(nodeStatus.getNodeId(), -1, -1));
                }

                // add the node to the new list
                newNodeList.put(nodeStatus.getNodeId(), nodeStatus);
            }
        }

        SSGLogRecord[] rawLogs = new SSGLogRecord[]{};
        Vector requestCompleted = new Vector();

        if (requests.size() > 0) {

            for (int i = 0; i < requests.size(); i++) {

                Vector newLogs = new Vector();
                LogRequest logRequest = (LogRequest) requests.elementAt(i);

                try {

                    rawLogs = new SSGLogRecord[]{};

                    rawLogs = logService.getSystemLog(logRequest.getNodeId(), logRequest.getStartMsgNumber(), logRequest.getEndMsgNumber(), FilteredLogTableModel.MAX_MESSAGE_BLOCK_SIZE);

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
                    logger.log(Level.SEVERE, "Unable to retrieve logs from server", e);
                    remoteExceptionCaught = true;
                    throw new RuntimeException(e);
                } catch (FindException e) {
                    logger.log(Level.SEVERE, "Unable to retrieve logs from server", e);
                    remoteExceptionCaught = true;
                    throw new RuntimeException(e);
                }

                if (newLogs.size() > 0) {
                    retrievedLogs.put(logRequest.getNodeId(), newLogs);
                    logRequest.addRetrievedLogCount(newLogs.size());
                }

                if ((rawLogs.length < FilteredLogTableModel.MAX_MESSAGE_BLOCK_SIZE ||
                        logRequest.getRetrievedLogCount() >= FilteredLogTableModel.MAX_NUMBER_OF_LOG_MESSGAES)) {

                    requestCompleted.add(logRequest);
                }
            }
        }

        for (int i = 0; i < requestCompleted.size(); i++) {
            LogRequest logRequest = (LogRequest) requestCompleted.elementAt(i);

            // remove the request from the list
            requests.removeElement(logRequest);
        }

        try {
            currentClusterSystemTime = clusterStatusService.getCurrentClusterSystemTime();
        } catch (RemoteException e) {
            remoteExceptionCaught = true;
            logger.log(Level.SEVERE, "Remote exception when retrieving cluster status from server", e);
        }

        if (currentClusterSystemTime == null) {
            return null;
        } else {
            //System.out.println("Number of outstanding requests: " + requests.size());
            return newNodeList;
        }

    }
}

