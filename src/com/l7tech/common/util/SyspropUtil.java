/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.util;

import java.security.AccessControlException;
import java.util.logging.Logger;

/**
 * Utilities for safely reading system properties from within code that might have to run as an Applet.
 */
public class SyspropUtil {
    private static final Logger logger = Logger.getLogger(SyspropUtil.class.getName());

    public static Integer getInteger(String name, int value) {
        try {
            return Integer.getInteger(name, value);
        } catch (AccessControlException e) {
            logger.fine("Unable to access system property " + name + "; using default value of " + value);
            return new Integer(value);
        }
    }

    public static boolean getBoolean(String name) {
        try {
            return Boolean.getBoolean(name);
        } catch (AccessControlException e) {
            logger.fine("Unable to access system property " + name + "; using default value of false");
            return false;
        }
    }

    public static String getString(String name, String dflt) {
        try {
            return System.getProperty(name, dflt);
        } catch (AccessControlException e) {
            logger.fine("Unable to access system property " + name + "; using default value of " + dflt);
            return dflt;
        }
    }
}
