/**
 * Copyright (C) 2008-2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.util.JdkLoggerConfigurator;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.UncaughtExceptionLogger;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.beans.factory.BeanCreationException;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.BindException;

/** @author alex */
public final class ProcessControllerDaemon {
    private final Logger logger;

    private ClassPathXmlApplicationContext ctx;
    private volatile boolean shutdown = false;
    static final int SHUTDOWN_POLL_INTERVAL = 5000;
    private ProcessController processController;
    public static final String[] DAEMON_CONTEXTS = new String[] {
        "/com/l7tech/server/processcontroller/resources/processControllerApplicationContext.xml",
        "/com/l7tech/server/processcontroller/resources/processControllerServletContainerContext.xml",
    };

    private ProcessControllerDaemon(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        shutdown = true;
                        synchronized (this) {
                            if (processController!=null) processController.stopNodes();
                            notifyAll();
                            // Kick the loop to enact the changes immediately
                        }
                    }
                });

        // This is here so that the logging system's shutdown hook runs after mine
        logger = Logger.getLogger(ProcessControllerDaemon.class.getName());
    }

    public static void main(String[] args) throws IOException {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionLogger());
        new ProcessControllerDaemon(args).runUntilShutdown();
    }

    private synchronized void runUntilShutdown() throws IOException {
        init();
        try {
            start();
        } catch ( BeanCreationException bce ) {
            if ( ExceptionUtils.causedBy( bce, BindException.class ) ) {
                logger.severe("Process controller unable to bind listener, please ensure server ip address and port are valid and available and restart.");
                System.exit(2);
            } else {
                throw bce;
            }
        }

        do {
            synchronized (this) {
                try {
                    wait(SHUTDOWN_POLL_INTERVAL);
                    if (shutdown) break;
                } catch (InterruptedException e) {
                    logger.info("Interrupted; cancelling this poll");
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            processController.visitNodes();
        } while (!shutdown);
        stop(0);
    }

    private void init() {
        // configure logging if the logs directory is found, else leave console output
        final File logsDir = new File("var/logs");
        if ( logsDir.exists() && logsDir.canWrite() ) {
            JdkLoggerConfigurator.configure("com.l7tech.server.processcontroller", "com/l7tech/server/processcontroller/resources/logging.properties", "etc/conf/logging.properties", false, true);
        }
    }

    private void start() throws IOException {
        logger.info("Starting Process Controller...");

        final ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(DAEMON_CONTEXTS);
        ctx.registerShutdownHook();
        this.ctx = ctx;
        this.processController = (ProcessController)ctx.getBean("processController");
        processController.setDaemon(true);

        processController.visitNodes(); // Detect states for any nodes that are already running
    }

    private int stop(int exitCode) {
        logger.info("Shutting down");
        try {
            processController.stopNodes();
            ctx.close();
            return exitCode;
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Caught exception on shutdown", t);
            return 1;
        }
    }
}
