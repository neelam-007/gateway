/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.cluster;

import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.util.UptimeMonitor;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.common.util.UptimeMetrics;
import com.l7tech.service.ServiceStatistics;
import org.springframework.orm.hibernate.support.HibernateDaoSupport;

import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.Iterator;

/**
 * Ther class that performs the status update activities.
 *
 * @author emil
 * @version Jan 3, 2005
 */
public class StatusUpdateManagerImpl extends HibernateDaoSupport implements StatusUpdateManager {
    private final Logger logger = Logger.getLogger(StatusUpdateManagerImpl.class.getName());

    private final ClusterInfoManager clusterInfoManager;
    private final ServiceUsageManager serviceUsageManager;
    private final ServiceManager serviceManager;

    public StatusUpdateManagerImpl(ClusterInfoManager clusterInfoManager, ServiceManager serviceManager, ServiceUsageManager serviceUsageManager) {
        this.clusterInfoManager = clusterInfoManager;
        this.serviceManager = serviceManager;
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
        double load = 0.0;
        UptimeMetrics metrics = null;
        try {
            metrics = UptimeMonitor.getLastUptime();
        } catch (FileNotFoundException e) {
            String msg = "cannot get uptime metrics";
            logger.log(Level.SEVERE, msg, e);
        } catch (IllegalStateException e) {
            String msg = "cannot get uptime metrics";
            logger.log(Level.SEVERE, msg, e);
        }
        load = metrics.getLoad1();
        clusterInfoManager.updateSelfStatus(load);
    }

    private void updateServiceUsage() {
        // get service usage from local cache
        String ourid = clusterInfoManager.thisNodeId();
        Collection stats = null;
        try {
            stats = serviceManager.getAllServiceStatistics();
        } catch (FindException e) {
            logger.log(Level.SEVERE, "could not update service usage");
        }
        if (stats != null) {
            try {
                serviceUsageManager.clear(ourid);
            } catch (DeleteException e) {
                logger.log(Level.SEVERE, "could not update service usage");
                return;
            }
            for (Iterator i = stats.iterator(); i.hasNext();) {
                ServiceStatistics statobj = (ServiceStatistics)i.next();
                ServiceUsage sa = ServiceUsage.fromStat(statobj, ourid);
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