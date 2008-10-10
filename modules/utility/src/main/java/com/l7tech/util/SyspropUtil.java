/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.util;

import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities for safely reading system properties from within code that might have to run as an Applet.
 */
public class SyspropUtil {
    private static final Logger logger = Logger.getLogger(SyspropUtil.class.getName());

    public static Integer getInteger(String name) {
        try {
            return Integer.getInteger(name);
        } catch (AccessControlException e) {
            logger.fine("Unable to access system property " + name + "; assuming it is not set");
            return null;
        }
    }

    public static Integer getInteger(String name, int value) {
        try {
            return Integer.getInteger(name, value);
        } catch (AccessControlException e) {
            logger.fine("Unable to access system property " + name + "; using default value of " + value);
            return new Integer(value);
        }
    }

    public static boolean getBoolean(String name) {
        return getBoolean(name, false);
    }

    public static boolean getBoolean(String name, boolean dflt) {
        try {
            if (dflt)
                return !"false".equalsIgnoreCase(getProperty(name));
            else
                return "true".equalsIgnoreCase(getProperty(name));
        } catch (AccessControlException e) {
            logger.fine("Unable to access system property " + name + "; using default value of " + dflt);
            return dflt;
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

    public static String getProperty(String name) {
        return getString(name, null);
    }

    public static Long getLong(String name, long dflt) {
        try {
             return Long.getLong(name, dflt);
        } catch (AccessControlException e) {
            logger.fine("Unable to access system property " + name + "; using default value of " + dflt);
            return new Long(dflt);
        }
    }

    public static Double getDouble(String name, double dflt) {
        try {
            String s = System.getProperty(name);
            return s != null && s.length() > 0 ? Double.parseDouble(s) : new Double(dflt);
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Invalid system property " + name + " (using default value of " + dflt + "): " + ExceptionUtils.getMessage(e));
            return new Double(dflt);
        } catch (AccessControlException e) {
            logger.fine("Unable to access system property " + name + ": using default value of " + dflt);
            return new Double(dflt);
        }
    }

    /**
     * Attempt to set a system property, if possible in this security context.
     *
     * @param name   name of property to set.  May not be null.
     * @param value  value to set.
     */
    public static void setProperty(final String name, final String value) {
        //noinspection unchecked
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                try {
                    System.setProperty(name, value);
                } catch (AccessControlException e) {
                    logger.warning("Unable to set system property " + name);
                }
                return null;
            }
        });
    }
}
