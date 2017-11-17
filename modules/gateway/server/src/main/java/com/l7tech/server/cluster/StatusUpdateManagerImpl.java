/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.cluster;

import com.l7tech.gateway.common.cluster.ServiceUsage;
import com.l7tech.gateway.common.service.ServiceStatistics;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.util.UptimeMonitor;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.UptimeMetrics;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ther class that performs the status update activities.
 *
 * @author emil
 * @version Jan 3, 2005
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class StatusUpdateManagerImpl extends HibernateDaoSupport implements StatusUpdateManager {
    private final Logger logger = Logger.getLogger(StatusUpdateManagerImpl.class.getName());

    private final ClusterInfoManager clusterInfoManager;
    private final ServiceUsageManager serviceUsageManager;
    private final ServiceCache serviceCache;
    private final ClusterMaster clusterMaster;
    private final Config config;

    public StatusUpdateManagerImpl(ClusterInfoManager clusterInfoManager,
                                   ServiceCache serviceCache,
                                   ServiceUsageManager serviceUsageManager,
                                   ClusterMaster clusterMaster,
                                   Config config)
    {
        this.clusterInfoManager = clusterInfoManager;
        this.serviceCache = serviceCache;
        this.serviceUsageManager = serviceUsageManager;
        this.clusterMaster = clusterMaster;
        this.config = config;
    }

    /**
     * Update node status and service usage
     *
     * XXX IMPORTANT XXX: This method may appear to be unused according to IDEA but it is called from a Spring timer 
     * XXX IMPORTANT XXX: defined in webApplicationContext.xml.
     */
    public void update() {
        updateNodeStatus();
        clearStaleNodes();
        updateServiceUsage();
    }

    /**
     * This cleans up stale nodes from the ClusterNodeInfo table
     */
    private void clearStaleNodes() {
        //Only the master needs to do cleanup
        if (clusterMaster.isMaster()) {
            final int staleTimeoutSeconds = config.getIntProperty("com.l7tech.server.clusterStaleNodeCleanupTimeoutSeconds", 3600);
            final Calendar oldestTimestamp = Calendar.getInstance();
            oldestTimestamp.add(Calendar.SECOND, -1 * staleTimeoutSeconds);
            clusterInfoManager.removeStaleNodes(oldestTimestamp.getTime().getTime());
        }
    }

    private void updateNodeStatus() {
        double load;
        UptimeMetrics metrics;
        try {
            metrics = UptimeMonitor.getLastUptime();
            load = metrics.getLoad1();
            clusterInfoManager.updateSelfStatus(load);
        } catch (FileNotFoundException e) {
            String msg = "cannot get uptime metrics";
            logger.log(Level.FINE, msg, ExceptionUtils.getDebugException(e));
        } catch (IllegalStateException e) {
            String msg = "cannot get uptime metrics";
            logger.log(Level.SEVERE, msg, e);
        } catch (UpdateException e) {
            String msg = "cannot update node status";
            logger.log(Level.INFO, msg, e);
        }
    }

    private void updateServiceUsage() {
        // get service usage from local cache
        String ourid = clusterInfoManager.thisNodeId();
        Collection<ServiceStatistics> stats = serviceCache.getAllServiceStatistics();
        if (stats != null) {
            try {
                serviceUsageManager.clear(ourid);
            } catch (DeleteException e) {
                logger.log(Level.SEVERE, "could not update service usage");
                return;
            }
            for( ServiceStatistics statobj : stats ) {
                ServiceUsage sa = ServiceUsage.fromStat( statobj, ourid );
                /*ServiceUsage sa = new ServiceUsage();
                sa.setServiceid(statobj.getServiceOid());
                sa.setNodeid(ourid);
                sa.setAuthorized(statobj.getAuthorizedRequestCount());
                sa.setCompleted(statobj.getCompletedRequestCount());
                sa.setRequests(statobj.getAttemptedRequestCount());*/
                try {
                    serviceUsageManager.record(sa);
                } catch (UpdateException e) {
                    logger.log(Level.SEVERE, "could not update service usage");
                }
            }
        }
    }

}