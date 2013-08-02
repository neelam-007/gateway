/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.util;

import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.cluster.GatewayStatus;
import com.l7tech.gateway.common.cluster.ServiceUsage;
import com.l7tech.gui.util.SwingWorker;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.objectmodel.FindException;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.service.ServiceAdmin;

import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class retrieves status of all nodes in a cluster.
 */
public class ClusterStatusWorker extends SwingWorker {

    private ClusterStatusAdmin clusterStatusService;
    private Hashtable<Goid, ServiceUsage> statsList;
    private Hashtable<String, GatewayStatus> newNodeList;
    private Hashtable<String, GatewayStatus> currentNodeList;
    private long clusterRequestCount;
    private ServiceAdmin serviceManager;
    private Date currentClusterSystemTime;
    private AtomicBoolean cancelled;
    private static final Logger logger = Logger.getLogger(ClusterStatusWorker.class.getName());

    /**
     * Constructor
     *
     * @param manager  The reference to the remote ServiceAdmin object.
     * @param clusterStatusService   The reference to the remote ClusterStatusService object.
     * @param currentNodeList   The list of nodes in the cluster obtained from the last retrieval.
     */
    public ClusterStatusWorker(ServiceAdmin manager, ClusterStatusAdmin clusterStatusService, Hashtable<String, GatewayStatus> currentNodeList, AtomicBoolean cancelled){
        this.clusterStatusService = clusterStatusService;
        this.serviceManager = manager;
        this.currentNodeList = currentNodeList;
        this.cancelled = cancelled;

        statsList = new Hashtable<Goid, ServiceUsage>();
    }

    /**
     * Return the new list of the nodes in the cluter
     *
     * @return The new node list.
     */
    public Hashtable<String, GatewayStatus> getNewNodeList(){
        return newNodeList;
    }

    /**
     * Return the total request count of the cluster
     *
     * @return The total request count of the cluster.
     */
    public long getClusterRequestCount(){
        return clusterRequestCount;
    }

    /**
     * Get Cluster's current system time.
     *
     * @return The current system time of the cluster.
     */
    public Date getCurrentClusterSystemTime() {
        return currentClusterSystemTime;
    }

    /**
     * Return the list of statistics for the service usages in every nodes of the cluster.
     *
     * @return The list of statistics.
     */
    public Vector<ServiceUsage> getStatisticsList(){
        Vector<ServiceUsage> stats = new Vector<ServiceUsage>();

        if (statsList != null) {
            stats.addAll(statsList.values());
        }
        return stats;
    }

    /**
     * Construct the value. This function performs the actual work of retrieving statistics.
     *
     * @return An object with the value constructed by this function.
     */
    public Object construct() {
        if (serviceManager == null || clusterStatusService == null) {
            return null;
        }

        if (currentNodeList == null) {
            throw new RuntimeException("The current node list is NULL");
        }

        try {
            // create a new empty node list
            newNodeList = new Hashtable<String, GatewayStatus>();

            // retrieve node status
            ClusterNodeInfo[] cluster = new ClusterNodeInfo[0];

            try {
                cluster = clusterStatusService.getClusterStatus();
            } catch (FindException e) {
                logger.log(Level.WARNING, "Unable to find cluster status from server", e);
            }

            if (cluster == null) {
                return null;
            }

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
            }

            if (serviceStats == null) {
                return null;
            }

            // create the statistics list
            try {
                ServiceHeader[] entityHeaders = serviceManager.findAllPublishedServices();

                for ( ServiceHeader header : entityHeaders ) {
                    ServiceUsage su = new ServiceUsage();
                    su.setServiceid(header.getGoid());
                    su.setName(header.getDisplayName());

                    // add the stats to the list
                    statsList.put(su.getServiceid(), su);
                }
            } catch (FindException e) {
                logger.log(Level.WARNING, "Unable to find all published services from server", e);
            }

            // update the statistics list and the node caches with the retrived data
            for (int i = 0; i < serviceStats.length; i++) {

                Object clusterServiceUsage =  statsList.get(serviceStats[i].getServiceid());

                // update cluster service usage
                if(clusterServiceUsage != null){
                    if(clusterServiceUsage instanceof ServiceUsage){
                        ServiceUsage csu = (ServiceUsage) clusterServiceUsage;
                        csu.setAuthorized((csu.getAuthorized() + serviceStats[i].getAuthorized()));
                        csu.setCompleted(csu.getCompleted() + serviceStats[i].getCompleted());
                        csu.setRequests(csu.getRequests() + serviceStats[i].getRequests());
                    }
                }

                // update counter in the node record for calculating load sharing and request failure percentage
                if((node = newNodeList.get(serviceStats[i].getNodeid())) != null){
                    if(node instanceof GatewayStatus){
                        GatewayStatus gatewayNode = (GatewayStatus) node;

                        gatewayNode.updateCompletedCounterCache(serviceStats[i].getCompleted());
                        gatewayNode.updateRequestCounterCache(serviceStats[i].getRequests());
                    }
                }

                //todo: only count the node in service
                clusterRequestCount += serviceStats[i].getRequests();
            }

            currentClusterSystemTime = clusterStatusService.getCurrentClusterSystemTime();

            if (currentClusterSystemTime == null) {
                return null;
            } else {
                // return a dummy object
                return statsList;
            }
        } catch (RuntimeException re) {
            if (cancelled.get()) { // eat any exceptions if we've been cancelled
                logger.log(Level.INFO, "Ignoring error for cancelled operation ''{0}''.", ExceptionUtils.getMessage(re));
                return null;
            } else {
                throw re;
            }
        }
    }
}

