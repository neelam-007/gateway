package com.l7tech.cluster;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Thread that updates server status and service usage statistics.
 * <p/>
 * This is used when a server is member of a cluster.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Dec 18, 2003<br/>
 * $Id$
 */
public class StatusUpdater extends Thread {
    private final StatusUpdateManager statusUpdateManager;


    private StatusUpdater(StatusUpdateManager statusUpdateManager) {
        this.statusUpdateManager = statusUpdateManager;
    }

    public static synchronized void initialize(StatusUpdateManager statusUpdateManager) {
        if (updater == null) {
            updater = new StatusUpdater(statusUpdateManager);
        }
        if (updater.isAlive()) {
            logger.warning("updater is already alive.");
        } else {
            logger.info("starting status updater");
            // this thread should not prevent the VM from exiting
            updater.setDaemon(true);
            updater.start();
        }
    }

    public static synchronized void stopUpdater() {
        updater.die();
    }

    public void run() {
        try {
            sleep(UPDATE_FREQUENCY * 2);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Status updater stopping prematurely", e);
            return;
        }
        while (true) {
            if (die) break;
            try {
                statusUpdateManager.update();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "error in update", e);
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


    public void die() {
        die = true;
    }

    private boolean die = false;

    private static StatusUpdater updater;
    private static final Logger logger = Logger.getLogger(StatusUpdater.class.getName());
    public static final long UPDATE_FREQUENCY = 4000;
}
