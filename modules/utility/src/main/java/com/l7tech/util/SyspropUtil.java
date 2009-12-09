/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.util;

import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities for safely reading system properties from within code that might have to run as an Applet.
 */
public class SyspropUtil {
    private static final Logger logger = Logger.getLogger(SyspropUtil.class.getName());

    private static class CacheHolder {
        private static final ConcurrentMap<String, String> propertyCache = new ConcurrentHashMap<String, String>();
        static {
            Background.scheduleRepeated(new TimerTask() {
                @Override
                public void run() {
                    propertyCache.clear();
                }
            }, 19543, 19543);
        }
    }

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
            return value;
        }
    }

    public static boolean getBoolean(String name) {
        return getBoolean(name, false);
    }

    public static boolean getBooleanCached(String name) {
        return getBooleanCached(name, false);
    }

    public static boolean getBooleanCached(String name, boolean dflt) {
        return toBoolean(name, dflt, getPropertyCached(name));
    }

    public static boolean getBoolean(String name, boolean dflt) {
        return toBoolean(name, dflt, getProperty(name));
    }

    private static boolean toBoolean(String name, boolean dflt, String property) {
        try {
            if (dflt)
                return !"false".equalsIgnoreCase(property);
            else
                return "true".equalsIgnoreCase(property);
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

    public static String getPropertyCached(String name) {
        String value = CacheHolder.propertyCache.get(name);
        if (value != null)
            return value;
        value = getProperty(name);
        if (name != null && value != null)
            CacheHolder.propertyCache.put(name, value);
        return value;
    }

    public static Long getLong(String name, long dflt) {
        try {
             return Long.getLong(name, dflt);
        } catch (AccessControlException e) {
            logger.fine("Unable to access system property " + name + "; using default value of " + dflt);
            return dflt;
        }
    }

    public static Double getDouble(String name, double dflt) {
        return toDouble(name, dflt, getProperty(name));
    }

    public static Double getDoubleCached(String name, double dflt) {
        return toDouble(name, dflt, getPropertyCached(name));
    }

    private static Double toDouble(String name, double dflt, String value) {
        try {
            return value != null && value.length() > 0 ? Double.parseDouble(value) : dflt;
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Invalid system property " + name + " (using default value of " + dflt + "): " + ExceptionUtils.getMessage(e));
            return dflt;
        } catch (AccessControlException e) {
            logger.fine("Unable to access system property " + name + ": using default value of " + dflt);
            return dflt;
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
            @Override
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
