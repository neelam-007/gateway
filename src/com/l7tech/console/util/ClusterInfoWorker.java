package com.l7tech.console.util;

import com.l7tech.common.gui.util.SwingWorker;
import com.l7tech.objectmodel.FindException;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.cluster.GatewayStatus;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.rmi.RemoteException;

/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class ClusterInfoWorker extends SwingWorker {

    private ClusterStatusAdmin clusterStatusService = null;
    private Hashtable newNodeList;
    private Hashtable currentNodeList;
    private boolean remoteExceptionCaught;

    static Logger logger = Logger.getLogger(ClusterStatusWorker.class.getName());

    public ClusterInfoWorker(ClusterStatusAdmin clusterStatusService, Hashtable currentNodeList) {
        this.clusterStatusService = clusterStatusService;
        this.currentNodeList = currentNodeList;

        remoteExceptionCaught = false;
    }

    public Hashtable getNewNodeList() {
        return newNodeList;
    }

    public boolean isRemoteExceptionCaught() {
        return remoteExceptionCaught;
    }

    public Object construct() {

        // create a new empty node list
        newNodeList = new Hashtable();

        if(clusterStatusService == null) {
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

            // add the node to the new list
            newNodeList.put(nodeStatus.getNodeId(), nodeStatus);
        }

        // return a dummy object
        return newNodeList;
    }
}

