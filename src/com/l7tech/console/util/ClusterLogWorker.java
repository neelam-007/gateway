package com.l7tech.console.util;

import com.l7tech.common.gui.util.SwingWorker;
import com.l7tech.objectmodel.FindException;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.cluster.GatewayStatus;
import com.l7tech.cluster.LogRequest;
import com.l7tech.logging.LogAdmin;
import com.l7tech.logging.LogMessage;
import com.l7tech.console.table.FilteredLogTableModel;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.rmi.RemoteException;

/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class ClusterLogWorker extends SwingWorker {

    private ClusterStatusAdmin clusterStatusService = null;
    private LogAdmin logService = null;
    private Hashtable newNodeList;
    private Hashtable currentNodeList;
    private Vector requests;
    private Hashtable retrievedLogs;
    private boolean remoteExceptionCaught;

    static Logger logger = Logger.getLogger(ClusterStatusWorker.class.getName());

    public ClusterLogWorker(ClusterStatusAdmin clusterStatusService, LogAdmin logService, Hashtable currentNodeList, Vector requests) {
        this.clusterStatusService = clusterStatusService;
        this.currentNodeList = currentNodeList;
        this.logService = logService;
        this.requests = requests;

        remoteExceptionCaught = false;
        retrievedLogs = new Hashtable();
    }

    public Hashtable getNewNodeList() {
        return newNodeList;
    }

    public boolean isRemoteExceptionCaught() {
        return remoteExceptionCaught;
    }

    public Hashtable getNewLogs() {
        return retrievedLogs;
    }

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

//        System.out.println("Number of nodes in the new list is: " + cluster.length);
//       System.out.println("Number of nodes in the old list is: " + currentNodeList.size());

        Object node = null;
        for (int i = 0; i < cluster.length; i++) {

            GatewayStatus nodeStatus = new GatewayStatus(cluster[i]);

            if ((node = currentNodeList.get(nodeStatus.getNodeId())) != null) {
                if (node instanceof GatewayStatus) {
                    // set the caches that already exist
                    nodeStatus.setRequestCounterCache(((GatewayStatus) node).getRequestCounterCache());
                    nodeStatus.setCompletedCounterCache(((GatewayStatus) node).getCompletedCounterCache());

                    // copy the TimeStampUpdateFailureCount
                    nodeStatus.setTimeStampUpdateFailureCount(((GatewayStatus) node).getTimeStampUpdateFailureCount());

                    // store the last update time
                    nodeStatus.setSecondLastUpdateTimeStamp(((GatewayStatus) node).getLastUpdateTimeStamp());
                }
            }
            else{
                // add the node to the request array with the startMsgNumber and endMsgNumber set to -1
                requests.add(new LogRequest(nodeStatus.getNodeId(), -1, -1));
            }

            // add the node to the new list
            newNodeList.put(nodeStatus.getNodeId(), nodeStatus);
        }

        String[] rawLogs = new String[]{};
        int accumulatedNewLogs = 0;
        Vector newLogs = new Vector();

        if (requests.size() > 0) {

            do {
                try {

                    rawLogs = logService.getSystemLog(((LogRequest) requests.get(0)).getStartMsgNumber(), ((LogRequest) requests.get(0)).getEndMsgNumber(), FilteredLogTableModel.MAX_MESSAGE_BLOCK_SIZE);
                    //System.out.println("Number of logs received: " + rawLogs.length);

                    // todo: retrieve multiple nodes later
                    //rawLogs = logService.getSystemLog(requests[0].getNodeId(), requests[0].getStartMsgNumber(), requests[0].getEndMsgNumber(), FilteredLogTableModel.MAX_MESSAGE_BLOCK_SIZE);

                    LogMessage logMsg = null;

                    if (rawLogs.length > 0) {

                        for (int i = 0; i < (rawLogs.length) && (newLogs.size() < FilteredLogTableModel.MAX_NUMBER_OF_LOG_MESSGAES); i++) {
                            logMsg = new LogMessage(rawLogs[i]);
                            newLogs.add(logMsg);
                        }

                        if (accumulatedNewLogs + rawLogs.length <= FilteredLogTableModel.MAX_NUMBER_OF_LOG_MESSGAES) {
                            // update the startMsgNumber
                            ((LogRequest) requests.get(0)).setStartMsgNumber(logMsg.getMsgNumber());

                            accumulatedNewLogs += rawLogs.length;
                        } else {

                            // done
                            break;
                        }
                    }

                } catch (RemoteException e) {
                    logger.log(Level.SEVERE, "Unable to retrieve logs from server", e);
                }
            } while (rawLogs.length == FilteredLogTableModel.MAX_MESSAGE_BLOCK_SIZE);    // may be more messages for retrieval

            retrievedLogs.put(((LogRequest) requests.get(0)).getNodeId(), newLogs);

        }
        return newNodeList;

    }
}

