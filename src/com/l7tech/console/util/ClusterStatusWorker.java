package com.l7tech.console.util;

import com.l7tech.common.gui.util.SwingWorker;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.cluster.GatewayStatus;
import com.l7tech.cluster.ServiceUsage;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.rmi.RemoteException;

/*
 * This class retrieves status of all nodes in a cluster.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class ClusterStatusWorker extends SwingWorker {

    private ClusterStatusAdmin clusterStatusService = null;
    private Hashtable statsList;
    private Hashtable newNodeList;
    private Hashtable currentNodeList;
    private long clusterRequestCount;
    private ServiceAdmin serviceManager = null;
    private java.util.Date currentClusterSystemTime = null;
    static Logger logger = Logger.getLogger(ClusterStatusWorker.class.getName());

    /**
     * Constructor
     *
     * @param manager  The reference to the remote ServiceAdmin object.
     * @param clusterStatusService   The reference to the remote ClusterStatusService object.
     * @param currentNodeList   The list of nodes in the cluster obtained from the last retrieval.
     */
    public ClusterStatusWorker(ServiceAdmin manager, ClusterStatusAdmin clusterStatusService, Hashtable currentNodeList){
        this.clusterStatusService = clusterStatusService;
        this.serviceManager = manager;
        this.currentNodeList = currentNodeList;

        statsList = new Hashtable();
    }

    /**
     * Return the new list of the nodes in the cluter
     *
     * @return  Hashtable  The new node list.
     */
    public Hashtable getNewNodeList(){
        return newNodeList;
    }

    /**
     * Return the total request count of the cluster
     *
     * @return long  The total request count of the cluster.
     */
    public long getClusterRequestCount(){
        return clusterRequestCount;
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
     * Return the list of statistics for the service usages in every nodes of the cluster.
     *
     * @return  Vector  The list of statistics.
     */
    public Vector getStatisticsList(){
        Vector stats = new Vector();

        if(statsList !=  null){
            for (Iterator i = statsList.keySet().iterator(); i.hasNext(); ) {
                ServiceUsage su = (ServiceUsage) statsList.get(i.next());
                stats.add(su);
            }
        }
        return stats;
    }

    /**
     * Consturct the value. This function performs the actual work of retrieving statistics.
     *
     * @return Object  An object with the value constructed by this function.
     */
    public Object construct() {

        if(serviceManager == null || clusterStatusService == null)
        {
            return null;
        }

        if(currentNodeList == null){
            throw new RuntimeException("The current node list is NULL");
        }

        // create a new empty node list
        newNodeList = new Hashtable();

        // retrieve node status
        ClusterNodeInfo[] cluster = new ClusterNodeInfo[0];

        try {
            cluster = clusterStatusService.getClusterStatus();
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to find cluster status from server", e);
        } catch (RemoteException e) {
            throw new RuntimeException("Remote exception when retrieving cluster status from server",e);
        }

        if(cluster == null){
            return null;
        }

//        System.out.println("Number of nodes in the new list is: " + cluster.length);
//       System.out.println("Number of nodes in the old list is: " + currentNodeList.size());

        Object node = null;
        for (int i = 0; i < cluster.length; i++) {

            GatewayStatus nodeStatus = new GatewayStatus(cluster[i]);
            String nodeId = nodeStatus.getNodeId();
            if (nodeId != null) {
                if ((node = currentNodeList.get(nodeId)) != null) {
                    if (node instanceof GatewayStatus) {
                        // set the caches that already exist
                        nodeStatus.setRequestCounterCache(((GatewayStatus) node).getRequestCounterCache());
                        nodeStatus.setCompletedCounterCache(((GatewayStatus) node).getCompletedCounterCache());

                        // reset the flag
                        nodeStatus.resetCacheUpdateFlag();

                        // copy the TimeStampUpdateFailureCount
                        nodeStatus.setTimeStampUpdateFailureCount(((GatewayStatus) node).getTimeStampUpdateFailureCount());

                        // store the last update time
                        nodeStatus.setSecondLastUpdateTimeStamp(((GatewayStatus) node).getLastUpdateTimeStamp());

                        // store the last node state
                        nodeStatus.setLastState(((GatewayStatus) node).getLastState());
                    }
                }

                // add the node to the new list
                newNodeList.put(nodeStatus.getNodeId(), nodeStatus);
            }
        }

        // retrieve service usage
        ServiceUsage[] serviceStats = null;
        try {
            serviceStats = clusterStatusService.getServiceUsage();
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to find service statistics from server", e);
        } catch (RemoteException e) {
            throw new RuntimeException("Remote exception when retrieving service statistics from server", e);
        }

        if (serviceStats == null) {
            return null;
        }

//        System.out.println("Number of service usage records received is " + serviceStats.length);
        com.l7tech.objectmodel.EntityHeader[] entityHeaders = null;

        // create the statistics list
        try {
            entityHeaders = serviceManager.findAllPublishedServices();

            EntityHeader header = null;
            for (int i = 0; i < entityHeaders.length; i++) {

                header = entityHeaders[i];
                if (header.getType().toString() == com.l7tech.objectmodel.EntityType.SERVICE.toString()) {

                    ServiceUsage su = new ServiceUsage();
                    su.setServiceid(header.getOid());
                    su.setServiceName(header.getName());

                    // add the stats to the list
                    statsList.put(new Long(su.getServiceid()), su);
                }

            }
        } catch (RemoteException e) {
            throw new RuntimeException("Remote exception when retrieving published services from server",e);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to find all published services from server", e);
        }

        // update the statistics list and the node caches with the retrived data
        for (int i = 0; i < serviceStats.length; i++) {

            Object clusterServiceUsage =  statsList.get(new Long(serviceStats[i].getServiceid()));

//            System.out.println("Service id: " + new Long(serviceStats[i].getServiceid()));
            // update cluster service usage
            if(clusterServiceUsage != null){
                if(clusterServiceUsage instanceof ServiceUsage){
                    ServiceUsage csu = (ServiceUsage) clusterServiceUsage;
                    csu.setAuthorized((csu.getAuthorized() + serviceStats[i].getAuthorized()));
                    csu.setCompleted(csu.getCompleted() + serviceStats[i].getCompleted());
                    csu.setRequests(csu.getRequests() + serviceStats[i].getRequests());
                }
            }

//            System.out.println("Node id is: " + serviceStats[i].getNodeid());
            // update counter in the node record for calculating load sharing and request failure percentage
            if((node = newNodeList.get(serviceStats[i].getNodeid())) != null){
                if(node instanceof GatewayStatus){
                    GatewayStatus gatewayNode = (GatewayStatus) node;

                    gatewayNode.updateCompletedCounterCache(serviceStats[i].getCompleted());
                    gatewayNode.updateRequestCounterCache(serviceStats[i].getRequests());
//                    System.out.println("Node is: " + gatewayNode.getName());
//                    System.out.println("adding completedCount to cache: " + serviceStats[i].getCompleted());                    System.out.println("adding AttempCount to cache: " + serviceStats[i].getRequests());
                }
            }

            //todo: only count the node in service
            clusterRequestCount += serviceStats[i].getRequests();
        }

        try {
            currentClusterSystemTime = clusterStatusService.getCurrentClusterSystemTime();
        } catch (RemoteException e) {
            throw new RuntimeException("Remote exception when retrieving cluster status from server",e);
        }

        if (currentClusterSystemTime == null) {
            return null;
        } else {
            // return a dummy object
            return statsList;
        }
    }
}

