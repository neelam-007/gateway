package com.l7tech.server.ems;

import com.l7tech.util.ExceptionUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Process entry point that starts the EMS server.
 */
public class EmsMain {
    private static final Logger logger = Logger.getLogger(EmsMain.class.getName());

    public static void main(String[] args) {
        try {
            new Ems().start();

        } catch (Throwable t) {
            final String msg = "Unable to start EMS: " + ExceptionUtils.getMessage(t);
            logger.log(Level.SEVERE, msg, t);
            System.err.println(msg);
            t.printStackTrace(System.err);
            System.exit(1);
        }

        try {
            waitForShutdown();
        } catch (Exception t) {
            final String msg = "Exception while waiting for EMS shutdown: " + ExceptionUtils.getMessage(t);
            logger.log(Level.SEVERE, msg, t);
            System.err.println(msg);
            t.printStackTrace(System.err);
            System.exit(2);
        }
    }

    private static void waitForShutdown() throws Exception {
        // Wait forever until server is shut down
        // TODO look for a SHUTDOWN.NOW file or some other lifecycle control mechanism
        Object thing = new Object();
        synchronized (thing) {
            try {
                thing.wait();
            } catch (InterruptedException e) {
                throw new Exception("thread interrupted");
            }
        }
    }
}
