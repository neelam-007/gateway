package com.l7tech.common.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * The class is a JDK logging configurator utility.
 * Initialize logging, trying different strategies. First look for the system
 * property <code>java.util.logging.config.file</code>, then look for
 * <code>logging.properties</code>. If that fails fall back to the
 * application-specified config file (ie, <code>com/l7tech/console/resources/logging.properties</code>).
 * Set the configuration properties, such as <i>org.apache.commons.logging.Log</i>
 * to the vlaues to trigger JDK logger.
 *
 * @author emil
 * @version 23-Apr-2004
 */
public class JdkLoggerConfigurator {

    /**
     * this class cannot be instantiated
     */
    private JdkLoggerConfigurator() {}

    /**
     * initialize logging, try different strategies. First look for the system
     * property <code>java.util.logging.config.file</code>, then look for
     * <code>logging.properties</code>. If that fails fall back to the
     * application-specified config file (ie,
     * <code>com/l7tech/console/resources/logging.properties</code>).
     *
     * @param classname                the classname to use for logging info about which logging.properties was found
     * @param shippedLoggingProperties the logging.properties to use if no locally-customized file is found,
     */
    public static synchronized void configure(String classname, String shippedLoggingProperties) {
        InputStream in = null;
        try {
            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Jdk14Logger");
            String cf = System.getProperty("java.util.logging.config.file");
            List configCandidates = new ArrayList(3);
            if (cf != null) {
                configCandidates.add(cf);
            }
            configCandidates.add("logging.properties");
            configCandidates.add(shippedLoggingProperties);

            boolean configFound = false;
            String configCandidate = null;
            final LogManager logManager = LogManager.getLogManager();
            for (Iterator iterator = configCandidates.iterator(); iterator.hasNext();) {
                configCandidate = (String)iterator.next();
                final File file = new File(configCandidate);

                if (file.exists()) {
                    in = file.toURL().openStream();
                    if (in != null) {
                        logManager.readConfiguration(in);
                        configFound = true;
                        break;
                    }
                }
                ClassLoader cl = JdkLoggerConfigurator.class.getClassLoader();
                URL resource = cl.getResource(configCandidate);
                if (resource != null) {
                    logManager.readConfiguration(resource.openStream());
                    configFound = true;
                    break;
                }
            }
            if (configFound) {
                Logger.getLogger(classname).info("Logging initialized from '" + configCandidate + "'");
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        } catch (SecurityException e) {
            e.printStackTrace(System.err);
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException e) { /*swallow*/
            }
        }
    }

    /**
     * initialize logging, try different strategies. First look for the system
     * property <code>java.util.logging.config.file</code>, then look for
     * <code>logging.properties</code>. If that fails fall back to the
     * application-specified config file (ie,
     * <code>com/l7tech/console/resources/logging.properties</code>).
     *
     * @param classname                the classname to use for logging info about which logging.properties was found
     * @param shippedLoggingProperties the logging.properties to use if no locally-customized file is found,
     */
    public static synchronized void configure(String classname, String shippedLoggingProperties, boolean reloading) {
    }


    /**
     * Add the log handler programatically
     *
     * @param handler
     */
    public static void addHandler(Handler handler) {
        if (handler == null) {
            throw new IllegalArgumentException();
        }
        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(handler);
    }

    /**
     * Thread to probe for config file changes and force reread
     */
    private class Probe extends Thread {
        private LogManager logManager = LogManager.getLogManager();

        /**
         * Time in milliseconds between probes
         */
        private long interval;
        /**
         * The last file read
         */
        private File file;
        /**
         * The lastModified time of prevFile
         */
        private long prevModified;

        Probe(File file) {
            this(file, getInterval());
        }

        Probe(File file, long interval) {
            super("Logging config file probe");
            setDaemon(true);
            this.interval = interval;
            prevModified = this.file.lastModified();
            this.file = file;
        }

        public void run() {
            Logger logger = Logger.getLogger("com.l7tech");
            try {
                while (interval > 0) {
                    Thread.sleep(interval);
                    long lastModified = file.lastModified();
                    if (lastModified > 0 &&
                      (lastModified != prevModified)) {
                        try {
                            logManager.readConfiguration();
                            interval = getInterval();
                            logger.log(Level.CONFIG,
                              "logging config file reread complete," +
                              " new interval is {0}",
                              new Long(interval));
                        } catch (Throwable t) {
                            logger.log(Level.WARNING,
                              "exception reading logging config file",
                              t);
                        }
                        prevModified = lastModified;
                    }
                }
            } catch (InterruptedException e) {
            } finally {
                logger.config("logging config file probe terminating");
            }
        }
    }

    /**
     * Return the probe interval.
     */
    private static long getInterval() {
        LogManager logManager = LogManager.getLogManager();
        String val = logManager.getProperty("com.l7tech.logging.interval");
        if (val != null) {
            try {
                return Long.decode(val).longValue();
            } catch (NumberFormatException e) {
            }
        }
        return 10;
    }

}
