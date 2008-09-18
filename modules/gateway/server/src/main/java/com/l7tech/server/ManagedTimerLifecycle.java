package com.l7tech.server;

import java.util.logging.Logger;

import com.l7tech.server.util.ManagedTimer;

/**
 * Bean to shutdown the managed timers.
 *
 * @author Steve Jones
 */
public class ManagedTimerLifecycle implements ServerComponentLifecycle {

    //- PUBLIC

    public void start() throws LifecycleException {
    }

    public void stop() throws LifecycleException {
    }

    public void close() throws LifecycleException {
        logger.info("Shutting down managed timers.");
        ManagedTimer.cancelAndWaitAll();
        logger.info("Managed timers stopped.");
    }

    public String toString() {
        return "ManagedTimer Controller";
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ManagedTimerLifecycle.class.getName());
}
