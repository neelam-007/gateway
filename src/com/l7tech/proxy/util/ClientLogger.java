/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Logging for the Agent.
 * @author mike
 * @version 1.0
 */
public class ClientLogger {
    private Logger logger = Logger.getLogger("com.l7tech.proxy");

    private static class Holder {
        private static ClientLogger instance = new ClientLogger();
    }

    public static ClientLogger getInstance(Class source) {
        return Holder.instance;
    }

    public void error(String s, Throwable e) {
        logger.log(Level.SEVERE, s, e);
    }

    public void error(Throwable e) {
        logger.log(Level.SEVERE, e.getMessage(), e);
    }

    public void info(String s) {
        logger.info(s);
    }

    public void warn(String s, Throwable e) {
        logger.log(Level.WARNING, s, e);
    }

    public void error(String s) {
        logger.log(Level.SEVERE,  s);
    }

    public void warn(String s) {
        logger.log(Level.WARNING,  s);
    }

    public void warn(Throwable t) {
        logger.log(Level.WARNING,  t.getMessage(), t);
    }

}
