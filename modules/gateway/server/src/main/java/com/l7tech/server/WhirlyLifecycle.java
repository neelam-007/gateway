package com.l7tech.server;

import java.util.logging.Logger;

import com.l7tech.common.io.WhirlycacheFactory;

/**
 * Bean to shutdown the cache tuning threads.
 *
 * @author Steve Jones
 */
public class WhirlyLifecycle implements ServerComponentLifecycle {

    //- PUBLIC

    public void setServerConfig(ServerConfig config) throws LifecycleException {
    }

    public void start() throws LifecycleException {
    }

    public void stop() throws LifecycleException {
    }

    public void close() throws LifecycleException {
        logger.info("Shutting down cache tuning.");
        WhirlycacheFactory.shutdown();
    }

    public String toString() {
        return "Whirlycache Controller";
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(WhirlyLifecycle.class.getName());
}
