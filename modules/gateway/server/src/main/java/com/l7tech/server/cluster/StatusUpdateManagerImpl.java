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
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.UptimeMetrics;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileNotFoundException;
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

    public StatusUpdateManagerImpl(ClusterInfoManager clusterInfoManager,
                                   ServiceCache serviceCache,
                                   ServiceUsageManager serviceUsageManager)
    {
        this.clusterInfoManager = clusterInfoManager;
        this.serviceCache = serviceCache;
        this.serviceUsageManager = serviceUsageManager;
    }

    /**
     * Update node status and service usage
     *
     * XXX IMPORTANT XXX: This method may appear to be unused according to IDEA but it is called from a Spring timer 
     * XXX IMPORTANT XXX: defined in webApplicationContext.xml.
     *
     * @throws UpdateException
     */
    public void update() throws UpdateException {
        updateNodeStatus();
        updateServiceUsage();
    }

    private void updateNodeStatus() throws UpdateException {
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