/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.remote.jini.Services;
import com.l7tech.remote.jini.export.RemoteService;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class AdminServicesBootProcess implements ServerComponentLifecycle {
    public void init( ComponentConfig config ) throws LifecycleException {
        this.services = Services.getInstance();
    }

    public void start() throws LifecycleException {
        logger.info("Starting admin services");
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                try {
                    services.start();
                } catch (Exception e) {
                    logger.log(Level.WARNING,
                      "There was an error in initalizing admin services.\n" +
                      " The admin services may not be available.", e);
                }
            }
        }, 3000);
    }

    public void stop() throws LifecycleException {
    }

    public void close() throws LifecycleException {
        logger.info("Stopping admin services.");
        RemoteService.unexportAll();
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    private Services services;
}
