package com.l7tech.cluster;

import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.TransactionException;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.sql.SQLException;

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
        clusterInfoManager.updateSelfStatus(0.5);
        // todo, real value
    }

    public void updateServiceUsage() {
        // todo
    }

    public void die() {
        die = true;
    }

    private boolean die = false;
    private final ClusterInfoManager clusterInfoManager = new ClusterInfoManager();

    private static final StatusUpdater updater = new StatusUpdater();
    private final Logger logger = LogManager.getInstance().getSystemLogger();
    public static final long UPDATE_FREQUENCY = 4000;
}
