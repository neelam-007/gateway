/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.logging.LogManager;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @deprecated Please use {@link java.util.Timer} and {@link PeriodicVersionCheck} instead!
 * @author alex
 * @version $Revision$
 */
public class PeriodicExecutor extends Thread {
    public PeriodicExecutor( PeriodicTask task ) {
        super( "Periodic executor for " + task );
        this.task = task;
    }

    public void run() {
        try {
            sleep(task.getFrequency()*2);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Interruption", e);
            return;
        }

        logger.finest( "Initiating periodic " + task );

        while (true) {
            if (die) break;

            try {
                sleep(task.getFrequency());
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "Interruption", e);
                break;
            }

            try {
                task.run();
            } catch (Throwable e) {
                logger.log(Level.WARNING, "Unhandled Exception", e);
            }

            lastRun = System.currentTimeMillis();
        }

        logger.finest("Stopping periodic " + task );
    }

    public long getLastRun() {
        return lastRun;
    }

    public void die() {
        die = true;
    }

    private long lastRun = -1;
    private boolean die = false;
    private PeriodicTask task;

    private final Logger logger = LogManager.getInstance().getSystemLogger();
}
