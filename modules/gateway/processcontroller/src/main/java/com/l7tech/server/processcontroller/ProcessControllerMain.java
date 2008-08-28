/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.logging.Level;
import java.util.logging.Logger;

/** @author alex */
public final class ProcessControllerMain {
    private static final Logger logger = Logger.getLogger(ProcessControllerMain.class.getName());
    private ClassPathXmlApplicationContext ctx;
    private volatile boolean shutdown = false;
    private static final int SHUTDOWN_POLL_INTERVAL = 5000;
    private ProcessController processController;

    private ProcessControllerMain() { }

    public static void main(String[] args) {
        new ProcessControllerMain().runUntilShutdown();
    }

    private void runUntilShutdown() {
        start();
        do {
            try {
                Thread.sleep(SHUTDOWN_POLL_INTERVAL);
                processController.visitNodes();
            } catch (InterruptedException e) {
                logger.info("Thread interrupted - treating as shutdown request");
                break;
            }
        } while (!shutdown);
        stop(0);
    }

    public void start() {
        logger.info("Starting Process Controller...");
        final ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("/com/l7tech/server/processcontroller/resources/processControllerApplicationContext.xml");
        ctx.registerShutdownHook();
        this.ctx = ctx;
        this.processController = (ProcessController)ctx.getBean("processController");
    }

    public int stop(int exitCode) {
        logger.info("Shutting down");
        try {
            ctx.close();
            return exitCode;
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Caught exception on shutdown", t);
            return 1;
        }
    }
}
