package com.l7tech.server.boot;

import com.l7tech.server.BootProcess;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerConfig;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Object that represents a complete, running Gateway instance.
 * TODO: merge this with BootProcess
 */
public class GatewayBoot {
    protected static final Logger logger = Logger.getLogger(GatewayBoot.class.getName());
    public static final String SHUTDOWN_FILENAME = "SHUTDOWN.NOW";
    private static final long SHUTDOWN_POLL_INTERVAL = 1987L;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private ClassPathXmlApplicationContext applicationContext;
    private ServerConfig serverConfig;

    /**
     * Create a Gateway instance but do not initialize or start it.
     * Set the com.l7tech.server.partitionName system property if the partition name is other than "default_".
     */
    public GatewayBoot() {
    }

    public void start() throws LifecycleException {
        if (!running.compareAndSet(false, true)) {
            // Already started
            return;
        }

        // This thread is responsible for attempting to start the server, and for clearing "running" flag if it fails
        boolean itworked = false;
        try {
            createApplicationContext();
            startBootProcess();
            itworked = true;
        } finally {
            if (!itworked)
                running.set(false);
        }
    }

    /**
     * Stop and destroy this Gateway instance.
     * This Gateway instance can not be started again once it is destroyed and should be discarded.
     *
     * @throws LifecycleException if there is a problem shutting down the server
     */
    public void destroy() throws LifecycleException {
        if (!running.get())
            return;
        applicationContext.close();
        running.set(false);
    }

    /**
     * Start the Gateway and run it until it is shut down.
     * This method does not return until the Gateway has shut down normally.
     *
     * @throws LifecycleException if the Gateway could not be started due to a failure of some kind,
     *                                              or if there was an exception while attempting a normal shutdown.
     */
    public void runUntilShutdown() throws LifecycleException {
        start();
        waitForShutdown();
        destroy();
    }

    private void waitForShutdown() {
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
        } while (!shutFile.exists());

        logger.info("SHUTDOWN.NOW file detected - treating as shutdown request");
    }

    private void createApplicationContext() {
        //hack: remove what catalina added as java.protocol.handler.pkgs and let the default
        // jdk protocol handler resolution  TODO: Is this hack still needed?
        System.getProperties().remove("java.protocol.handler.pkgs");
        applicationContext = new ClassPathXmlApplicationContext(new String[]{
                "com/l7tech/server/resources/dataAccessContext.xml",
                "com/l7tech/server/resources/ssgApplicationContext.xml",
                "com/l7tech/server/resources/adminContext.xml",
                "com/l7tech/server/resources/rbacEnforcementContext.xml",
                "org/codehaus/xfire/spring/xfire.xml",
        });
        serverConfig = (ServerConfig)applicationContext.getBean("serverConfig", ServerConfig.class);
    }

    private void startBootProcess() throws LifecycleException {
        BootProcess boot = (BootProcess)applicationContext.getBean("ssgBoot", BootProcess.class);
        boot.start();
    }
}
