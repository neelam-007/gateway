/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import java.util.logging.Logger;
import java.lang.reflect.Method;

/**
 * Get the appropriate logger regardless of whether we are running in a Gateway or an Agent, and without maintaining
 * any static references to LogManager classes that might not be present.
 * <p>
 * Problem: the LogManager is SSG-specific, and thus is not shipped with the Agent.  Writing the reflection code
 * to check if the LogManager is present is tedious and error-prone.
 * <p>
 * Work-around: Use this class to do the ugly work.
 * <p>
 * Future TODO: This is very, very ugly.  The Agent should use the same logging framework as the Gateway.
 */
public class CommonLogger {
    private CommonLogger() {}

    /**
     * Get the appropriate system logger for this environment.  If the LogManager is available, will use that;
     * otherwise, uses the Agent logger.
     * @return a Logger that can be used to log your various goings-on.
     */
    public static Logger getSystemLogger() {
        return LogHolder._systemLogger;
    }

    private static class LogHolder {
        private static Logger getSystemLogger() {
            try {
                Class logManagerClass = Class.forName("com.l7tech.logging.LogManager");
                Method logManager_getInstance = logManagerClass.getMethod( "getInstance", new Class[0] );
                Object logManager = logManager_getInstance.invoke( null, new Object[0] );
                Method logManager_getSystemLogger = logManagerClass.getMethod( "getSystemLogger" , new Class[0] );
                Logger logger = (Logger) logManager_getSystemLogger.invoke( logManager, new Object[0] );
                return logger;
            } catch ( Exception e ) {
                // just use Client logger
                Logger logger = Logger.getLogger("com.l7tech.proxy");
                logger.finest("LogManager unavailable: " + e.toString() + "; will log in Agent mode");
                return logger;
            }
        }

        private static final Logger _systemLogger = getSystemLogger();
    }
}
