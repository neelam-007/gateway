/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.boot;

import com.l7tech.server.ServerConfig;

import java.io.File;
import java.util.logging.Logger;

/**
 * Extracted from {@link GatewayBoot} so that it can be a Spring bean.
 *  
 * @author alex
 */
public final class ShutdownWatcher {
    private static final Logger logger = Logger.getLogger(ShutdownWatcher.class.getName());

    private final ServerConfig serverConfig;
    private static final String SHUTDOWN_FILENAME = "SHUTDOWN.NOW";
    private static final long SHUTDOWN_POLL_INTERVAL = 1987L;
    private volatile boolean shutdown = false;

    public ShutdownWatcher(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public void shutdownNow() {
        this.shutdown = true;
    }

    void waitForShutdown() {
        if (serverConfig == null)
            throw new IllegalStateException("Unable to wait for shutdown - no serverConfig available");
        File configDir = serverConfig.getLocalDirectoryProperty(ServerConfig.PARAM_CONFIG_DIRECTORY, "/ssg", false);
        if (configDir == null || !configDir.isDirectory())
            throw new IllegalStateException("Config directory not found: " + configDir);
        File shutFile = new File(configDir, SHUTDOWN_FILENAME);

        do {
            try {
                Thread.sleep(SHUTDOWN_POLL_INTERVAL);
            } catch (InterruptedException e) {
                logger.info("Thread interrupted - treating as shutdown request");
                break;
            }
        } while (!shutdown && !shutFile.exists());

        logger.info("SHUTDOWN.NOW file detected - treating as shutdown request");
    }
}
