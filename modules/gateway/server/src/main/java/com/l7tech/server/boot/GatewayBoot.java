package com.l7tech.server.boot;

import com.l7tech.gateway.common.Component;
import com.l7tech.server.BootProcess;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.log.JdkLogConfig;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.ExceptionUtils;
import com.mchange.v2.c3p0.C3P0Registry;
import com.mchange.v2.c3p0.PooledDataSource;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Object that represents a complete, running Gateway instance.
 * 
 * TODO: merge this with BootProcess
 */
public class GatewayBoot {
    protected static final Logger logger = Logger.getLogger(GatewayBoot.class.getName());
    private static final long DB_CHECK_DELAY = 30;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private ClassPathXmlApplicationContext applicationContext;
    private ShutdownWatcher shutdowner;

    static {
        JdkLogConfig.configureLogging();
    }

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

        logger.info("Starting " + BuildInfo.getLongBuildString());

        // This thread is responsible for attempting to start the server, and for clearing "running" flag if it fails
        boolean itworked = false;
        try {
            ServerConfig.getInstance().getLocalDirectoryProperty(ServerConfig.PARAM_LOG_DIRECTORY, true);
            spawnDbWarner();
            createApplicationContext();
            String ipAddress = startBootProcess();
            startListeners(ipAddress);
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
        final CountDownLatch shutdownlatch = new CountDownLatch(1);
        final CountDownLatch exitLatch = new CountDownLatch(1);

        start();
        addShutdownHook( shutdownlatch, exitLatch );
        waitForShutdown( shutdownlatch );
        try {
            destroy();
        } finally {
            notifyExit( exitLatch );
        }
    }

    // Launch a background thread that warns if no DB connections appear within a reasonable period of time (Bug #4271)
    private static void spawnDbWarner() {
        Thread dbcheck = new Thread("Database Check") {
            public void run() {
                try {
                    Thread.sleep(DB_CHECK_DELAY * 1000L);
                    int connections = getNumDbConnections();
                    if (connections >= 1) {
                        logger.log(Level.FINE, "Database check: " + connections + " database connections open after " + DB_CHECK_DELAY + " seconds");
                        return;
                    }

                    logger.log(Level.SEVERE, "WARNING: No database connections open after " + DB_CHECK_DELAY + " seconds; possible DB connection failure?");
                } catch (Throwable t) {
                    logger.log(Level.SEVERE, "Unable to check for database connections: " + ExceptionUtils.getMessage(t), t);
                }
            }
        };
        dbcheck.setDaemon(true);
        dbcheck.start();
    }

    private static int getNumDbConnections() throws SQLException {
        int connections = 0;
        //noinspection unchecked
        Set<PooledDataSource> pools = C3P0Registry.getPooledDataSources();
        for ( PooledDataSource pool : pools )
            connections += pool.getNumConnectionsAllUsers();
        return connections;
    }

    private void createApplicationContext() {
        applicationContext = new ClassPathXmlApplicationContext(new String[]{
                "com/l7tech/server/resources/dataAccessContext.xml",
                "com/l7tech/server/resources/ssgApplicationContext.xml",
                "com/l7tech/server/resources/adminContext.xml",
                //"org/codehaus/xfire/spring/xfire.xml",
                "com/l7tech/server/resources/cxfSupportContext.xml",
        });
        shutdowner = (ShutdownWatcher)applicationContext.getBean("ssgShutdown", ShutdownWatcher.class);
    }

    private String startBootProcess() throws LifecycleException {
        BootProcess boot = (BootProcess)applicationContext.getBean("ssgBoot", BootProcess.class);
        boot.start();
        return boot.getIpAddress();
    }

    private void startListeners(String ipAddress) {
        applicationContext.publishEvent(new ReadyForMessages(this, Component.GW_SERVER, ipAddress));
    }

    private void addShutdownHook( final CountDownLatch shutdown, final CountDownLatch exitSync ) {
        // add hook for shutdown bean
        shutdowner.setShutdownLatch( shutdown );

        // add shutdown handler
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
            /**
             * Run before shutdown, we need to be ready to exit when this thread dies.
             */
            public void run() {
                // notify to start shutdown process
                shutdown.countDown();

                // wait for shutdown to complete, we'll assume that the process is killed if it takes too long
                // but we don't want to block forever so timeout after 5 mins
                try {
                    exitSync.await( 5, TimeUnit.MINUTES );
                } catch (InterruptedException e) {
                    // thread exits immediately
                }
            }
        }));
    }

    private static void notifyExit( final CountDownLatch exitSync ){
        // notify to start shutdown process
        exitSync.countDown();
    }

    private static void waitForShutdown( final CountDownLatch shutdown ) {
        // Wait forever until server is shut down
        try {
            shutdown.await();
        } catch (InterruptedException e) {
            logger.info("Shutting down due to interrruped.");
        }
    }
}
