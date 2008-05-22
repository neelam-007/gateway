package com.l7tech.server.ems;

import com.l7tech.common.util.ExceptionUtils;

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

        } catch (Exception e) {
            final String msg = "Unable to start EMS: " + ExceptionUtils.getMessage(e);
            logger.log(Level.SEVERE, msg, e);
            System.err.println(msg);
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
