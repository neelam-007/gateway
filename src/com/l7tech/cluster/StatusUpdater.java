package com.l7tech.cluster;

import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.*;
import com.l7tech.server.util.UptimeMonitor;
import com.l7tech.common.util.UptimeMetrics;
import com.l7tech.common.util.Locator;
import com.l7tech.service.ServiceManager;
import com.l7tech.service.ServiceStatistics;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collection;
import java.util.Iterator;
import java.sql.SQLException;
import java.io.FileNotFoundException;


/**
 * Thread that updates server status and service usage statistics.
 *
 * This is used when a server is member of a cluster.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Dec 18, 2003<br/>
 * $Id$
 *
 */
public class StatusUpdater extends Thread {
    public static void initialize() {
        Logger logger = LogManager.getInstance().getSystemLogger();
        if (updater.isAlive()) {
            logger.warning("updater is already alive.");
        }
        else {
            logger.info("starting status updater");
            // this thread should not prevent the VM from exiting
            updater.setDaemon(true);
            updater.start();
        }
    }

    public static void stopUpdater() {
        updater.die();
    }

    public void run() {
        try {
            sleep(UPDATE_FREQUENCY*2);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Status updater stopping prematurely", e);
            return;
        }
        while (true) {
            if (die) break;

            PersistenceContext context = null;
            try {
                context = PersistenceContext.getCurrent();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "error getting persistence context. " +
                                         "this is stopping prematurely", e);
                return;
            }
            try {
                try {
                    context.beginTransaction();
                } catch (TransactionException e) {
                    logger.log(Level.SEVERE, "error begining transaction. " +
                                             "this is stopping prematurely", e);
                    return;
                }
                try {
                    updateNodeStatus();
                    updateServiceUsage();
                } catch (Throwable e) {
                    logger.log(Level.SEVERE, "error in update", e);
                } finally {
                    try {
                        context.commitTransaction();
                    } catch (TransactionException e) {
                        logger.log(Level.SEVERE, "error commiting transaction. " +
                                                 "this is stopping prematurely", e);
                    }
                }
            } finally {
                context.close();
                context = null;
            }

            if (die) break;
            // sleep
            try {
                sleep(UPDATE_FREQUENCY);
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "Status updater stopping prematurely", e);
                return;
            }

        }
        logger.info("stopping the status updater");
    }

    public void updateNodeStatus() throws UpdateException {
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

    public void updateServiceUsage() {
        // get service usage from local cache
        ServiceManager serviceManager = (ServiceManager)Locator.getDefault().lookup(ServiceManager.class);
        Collection stats = null;
        try {
            stats = serviceManager.getAllServiceStatistics();
        } catch (FindException e) {
            logger.log(Level.SEVERE, "could not update service usage");
        }
        if (stats != null) {
            try {
                serviceUsageManager.clear();
            } catch (DeleteException e) {
                logger.log(Level.SEVERE, "could not update service usage");
                return;
            }
            for (Iterator i = stats.iterator(); i.hasNext();) {
                ServiceStatistics statobj = (ServiceStatistics)i.next();
                ServiceUsage sa = new ServiceUsage();
                sa.setServiceid(statobj.getServiceOid());
                sa.setNodeid(clusterInfoManager.thisNodeId());
                sa.setAuthorized(statobj.getAuthorizedRequestCount());
                sa.setCompleted(statobj.getCompletedRequestCount());
                sa.setRequests(statobj.getAttemptedRequestCount());
                try {
                    serviceUsageManager.record(sa);
                } catch (UpdateException e) {
                    logger.log(Level.SEVERE, "could not update service usage");
                }
            }
        }
    }

    public void die() {
        die = true;
    }

    private boolean die = false;
    private final ClusterInfoManager clusterInfoManager = new ClusterInfoManager();
    private final ServiceUsageManager serviceUsageManager = new ServiceUsageManager();

    private static final StatusUpdater updater = new StatusUpdater();
    private final Logger logger = LogManager.getInstance().getSystemLogger();
    public static final long UPDATE_FREQUENCY = 4000;
}
