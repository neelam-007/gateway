package com.l7tech.cluster;

import com.l7tech.common.util.UptimeMetrics;
import com.l7tech.objectmodel.*;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.util.UptimeMonitor;
import com.l7tech.service.ServiceStatistics;
import org.springframework.context.ApplicationContext;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;


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
    private ApplicationContext springContext;

    public static synchronized void initialize(ApplicationContext springContext) {
        if (updater.isAlive()) {
            logger.warning("updater is already alive.");
        }
        else {
            logger.info("starting status updater");
            // this thread should not prevent the VM from exiting
            updater.springContext = springContext;
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
                logger.log(Level.SEVERE, "error getting persistence context.", e);
                return;
            }
            if (context != null) {
                try {
                    boolean transactionok = true;
                    try {
                        context.beginTransaction();
                    } catch (TransactionException e) {
                        logger.log(Level.SEVERE, "error begining transaction. ", e);
                        transactionok = false;
                    }
                    if (transactionok) {
                        try {
                            updateNodeStatus();
                            updateServiceUsage(context);
                        } catch (Throwable e) {
                            logger.log(Level.SEVERE, "error in update", e);
                        } finally {
                            try {
                                context.commitTransaction();
                            } catch (TransactionException e) {
                                logger.log(Level.SEVERE, "error commiting transaction. ", e);
                            }
                        }
                    }
                } finally {
                    context.close();
                    context = null;
                }
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

    public void updateServiceUsage(PersistenceContext context) {
        // get service usage from local cache
        ServiceManager serviceManager = (ServiceManager)springContext.getBean("serviceManager");
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
            // commit those deletes so that the following saves work properly
            try {
                context.commitTransaction();
                context.beginTransaction();
            } catch (TransactionException e) {
                logger.log(Level.WARNING, "could not reset transaction", e);
                return;
            }
            for (Iterator i = stats.iterator(); i.hasNext();) {
                ServiceStatistics statobj = (ServiceStatistics)i.next();
                ServiceUsage sa = new ServiceUsage();
                sa.setServiceid(statobj.getServiceOid());
                sa.setNodeid(ourid);
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
    private final ClusterInfoManager clusterInfoManager = ClusterInfoManager.getInstance();
    private final ServiceUsageManager serviceUsageManager = new ServiceUsageManager();

    private static final StatusUpdater updater = new StatusUpdater();
    private static final Logger logger = Logger.getLogger(StatusUpdater.class.getName());
    public static final long UPDATE_FREQUENCY = 4000;
    //public static final long UPDATE_FREQUENCY = 10;
}
