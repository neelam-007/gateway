/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.boot;

import java.util.logging.Logger;
import java.util.concurrent.CountDownLatch;

/**
 * Extracted from {@link GatewayBoot} so that it can be a Spring bean.
 *  
 * @author alex
 */
public final class ShutdownWatcher {

    //- PUBLIC

    public ShutdownWatcher() {
    }

    public void shutdownNow() {
        CountDownLatch latch = this.latch;
        if ( latch != null ) {
            logger.info("Shutdown requested");
            latch.countDown();
        } else {
            logger.warning("Shutdown requested but watcher is not configured!");            
        }
    }

    //- PACKAGE

    void setShutdownLatch( final CountDownLatch latch ) {
        this.latch = latch;   
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ShutdownWatcher.class.getName());
    private volatile CountDownLatch latch;

}
