package com.l7tech.util;

import org.jetbrains.annotations.Nullable;

import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities for safely reading system properties from within code that might have to run as an Applet.
 * <p/>
 * Any code able to reach this class can read and set arbitrary system properties.
 * When running with a non-trivial SecurityManager access to this class must be restricted.
 */
public class SyspropUtil {
    private static final Logger logger = Logger.getLogger(SyspropUtil.class.getName());

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

    public static String getString(String name, @Nullable String dflt) {
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
            return dflt;
        }
    }

    public static Double getDouble(String name, double dflt) {
        return toDouble(name, dflt, getProperty(name));
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
                    ConfigFactory.clearCachedConfig();
                } catch (AccessControlException e) {
                    logger.warning("Unable to set system property " + name);
                }
                return null;
            }
        });
    }

    public static void clearProperty(final String name) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                try {
                    System.clearProperty(name);
                    ConfigFactory.clearCachedConfig();
                } catch (AccessControlException e) {
                    logger.warning("Unable to clear system property " + name);
                }
                return null;
            }
        });
    }

    public static void clearProperties(final String... names) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                try {
                    for (String name : names) {
                        try {
                            System.clearProperty(name);
                        } catch (AccessControlException e) {
                            logger.warning("Unable to clear system property " + name);
                        }
                    }
                } finally {
                    ConfigFactory.clearCachedConfig();
                }
                return null;
            }
        });
    }
}
