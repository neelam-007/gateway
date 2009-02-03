package com.l7tech.util;

import com.l7tech.util.IOUtils;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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
    private static Probe probe;
    private static AtomicBoolean serviceNameAppenderState =
            new AtomicBoolean(Boolean.getBoolean("com.l7tech.logging.appendservicename"));
    private static AtomicBoolean debugState =
            new AtomicBoolean(Boolean.getBoolean("com.l7tech.logging.debug"));
    private static AtomicReference<Properties> nonDefaultProperties =
            new AtomicReference<Properties>();

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
        configure(classname, null, shippedLoggingProperties, false, true);
    }

    /**
     * initialize logging, try different strategies. First look for the system
     * property <code>java.util.logging.config.file</code>, then look for
     * <code>logging.properties</code>. If that fails fall back to the
     * <code>shippedLoggingProperties</code>
     *
     * @param classname                the classname to use for logging info about which logging.properties was found
     * @param shippedLoggingProperties the logging.properties to use if no locally-customized file is found
     * @param reloading                whether to start the configuration reloading thread
     */
    public static synchronized void configure(String classname, String shippedLoggingProperties, boolean reloading) {
        configure(classname, null, shippedLoggingProperties, reloading, true);
    }

    /**
     * initialize logging, try different strategies. First look for the system
     * property <code>java.util.logging.config.file</code>, then look for
     * <code>logging.properties</code>. If that fails fall back to the
     * <code>shippedLoggingProperties</code>
     *
     * @param classname                the classname to use for logging info about which logging.properties was found
     * @param shippedDefaults          path or file for default log properties (these are overridden by those in shippedLoggingProperties)
     * @param shippedLoggingProperties the logging.properties to use if no locally-customized file is found
     * @param reloading                whether to start the configuration reloading thread
     * @param redirectOtherFrameworks  true to redirect other logging frameworks to JUL (should not be used during initial JDK config)
     */
    public static synchronized void configure(final String classname,
                                              final String shippedDefaults,
                                              final String shippedLoggingProperties,
                                              final boolean reloading,
                                              final boolean redirectOtherFrameworks) {
        try {
            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Jdk14Logger");
            String cf = SyspropUtil.getProperty("java.util.logging.config.file");
            List<String> configCandidates = new ArrayList<String>(3);
            if (cf != null) {
                configCandidates.add(cf);
            }
            configCandidates.add("logging.properties");
            if ( shippedLoggingProperties != null ) {
                configCandidates.add(shippedLoggingProperties);
            }

            boolean configFound = false;
            String configCandidate = null;
            File probeFile = null;
            URL probeDef = null;
            final ClassLoader cl = JdkLoggerConfigurator.class.getClassLoader();
            final LogManager logManager = LogManager.getLogManager();

            // Check for defaults
            if ( shippedDefaults!=null ) {
                File defaultsFile = new File(shippedDefaults);
                if ( defaultsFile.exists() ) {
                    probeDef = defaultsFile.toURI().toURL();
                } else {
                    probeDef = cl.getResource(shippedDefaults);
                }
            }

            // Check config files
            for ( String configurationCandidate : configCandidates) {
                configCandidate = configurationCandidate;

                final File file = new File(configCandidate);
                if (file.exists()) {
                    if (readConfiguration(logManager, probeDef, file.toURI().toURL(), false)) {
                        configFound = true;
                        probeFile = file;
                        break;
                    }
                }

                URL resource = cl.getResource(configCandidate);
                if (readConfiguration(logManager, probeDef, resource, false)) {
                    configFound = true;
                    probeFile = new File(resource.getPath());
                    break;
                }
            }

            if ( classname != null ) {
                Logger logger = Logger.getLogger(classname);
                if ( probeDef!=null || configFound ) {
                    if ( probeDef!=null) {
                        if ( configFound ) {
                            logger.config("Logging initialized from '"+configCandidate+"', with defaults from '"+probeDef+"'");
                        } else {
                            logger.config("Logging initialized with defaults from '"+probeDef+"'");
                        }
                    } else {
                        logger.config("Logging initialized from '" + configCandidate + "'");
                    }
                } else {
                    logger.warning("No logging configuration found " + configCandidates);
                }
            }

            if (reloading && probeFile != null) {
                if (probe != null) { // kill the old probe
                    probe.interrupt();
                    try {
                        probe.join();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    probe = null;
                }
                probe = new Probe(probeFile, probeDef, getInterval(), classname);
                probe.start();
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        } catch (SecurityException e) {
            e.printStackTrace(System.err);
        }

        if ( redirectOtherFrameworks ) {
            setupLog4j();
        }
    }

    /**
     * Get the Properties set from the non-defaults configuration file.
     *
     * @return The properties, may be empty but not null
     */
    public static Properties getNonDefaultProperties() {
        Properties properties = new Properties();

        Properties nonDefaultProps = nonDefaultProperties.get();
        if ( nonDefaultProps != null ) {
            properties.putAll( nonDefaultProps );
        }

        return properties;
    }

    public static interface ResettableLogManager {
        void resetLogs();
    }

    /**
     * Read log configuration from the given URL to the given manager.
     *
     * This will read configuration from the defaults URL and from the
     * configuration url in that order (as though they were a single
     * log config file)
     *
     * If either URL is null it is ignored
     */
    @SuppressWarnings({"unchecked"})
    private static boolean readConfiguration(final LogManager logManager,
                                             final URL configDefs,
                                             final URL config,
                                             final boolean resetLevels) throws IOException {
        boolean readConfigUrl = false;

        InputStream defaultsIn = null;
        InputStream configIn = null;
        InputStream fullIn = null;
        try {
            //
            defaultsIn = configDefs!=null ? configDefs.openStream() : null;
            configIn = config!=null ? config.openStream() : null;

            // read into memory
            if ( configIn == null ) {
                fullIn = defaultsIn;
            } else if ( defaultsIn == null ) {
                fullIn = configIn;
                readConfigUrl = true;
            } else {
                //noinspection IOResourceOpenedButNotSafelyClosed
                fullIn = new SequenceInputStream(defaultsIn, configIn);
                readConfigUrl = true;
            }

            if ( fullIn != null ) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IOUtils.copyStream(fullIn, baos);
                byte[] configBytes = baos.toByteArray();

                // work around bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5035854
                // by ensuring all Loggers mentioned in the configuration actually exist.
                ByteArrayInputStream bais = new ByteArrayInputStream(configBytes);
                Properties loggerProps = new Properties();
                loggerProps.load(bais);

                if ( resetLevels ) {
                    for (String propertyName : (Collection<String>) Collections.list(loggerProps.propertyNames())) {
                        if (propertyName.endsWith(".level")) {
                            String loggerName = propertyName.substring(0, propertyName.length()-6);
                            Logger.getLogger(loggerName);
                        }
                    }
                }

                // load configuration
                bais = new ByteArrayInputStream(configBytes);
                if ( logManager instanceof ResettableLogManager ) {
                    ((ResettableLogManager)logManager).resetLogs();
                }
                logManager.readConfiguration(bais);
                updateState(loggerProps);
            }
        } finally {
            ResourceUtils.closeQuietly( fullIn );
            ResourceUtils.closeQuietly( defaultsIn );
            ResourceUtils.closeQuietly( configIn );
        }

        // save for review later
        if ( config != null ) {
            configIn = null;
            try {
                configIn = config.openStream();

                Properties props = new Properties();
                props.load( configIn );
                nonDefaultProperties.set( props );
            } finally {
                ResourceUtils.closeQuietly( configIn );
            }

        }

        return readConfigUrl;
    }

    /**
     * If the Log4j library is present then configure it to use our
     * logging framework.
     */
    private static void setupLog4j() {
        try {
            Class configClass = Class.forName("com.l7tech.util.Log4jJdkLogAppender");
            java.lang.reflect.Method configMethod = configClass.getMethod("init", new Class[0]);
            configMethod.invoke(null);
            // get logger here since we don't want this to occur on class load
            Logger.getLogger(JdkLoggerConfigurator.class.getName()).log(Level.INFO, "Redirected Log4j logging to JDK logging.");
        }
        catch(NoClassDefFoundError ncdfe) {
            // then we won't configure it ...            
        }
        catch(ClassNotFoundException cnfe) {
            // then we won't configure it ...
        }
        catch(Exception e) {
            Logger.getLogger(JdkLoggerConfigurator.class.getName()).log(Level.WARNING, "Error setting up Log4j logging.", e);
        }
    }


    /**
     * Thread to probe for config file changes and force reread
     */
    private static class Probe extends Thread {

        /**
         * Time in milliseconds between probes
         */
        private long interval;
        /**
         * The last file read
         */
        private final File file;

        /**
         * URL for logging configuration defaults
         */
        private final URL defaultsUrl;

        /**
         * The lastModified time of prevFile
         */
        private long prevModified;

        /**
         * The logger where config logs are sent to
         */
        private String loggerName;

        Probe(File file, URL defaultsUrl, long interval, String loggerName) {
            this.file = file;
            this.defaultsUrl = defaultsUrl;
            this.interval = interval;
            this.loggerName = loggerName;
            this.prevModified = this.file.lastModified();
            setName("LoggerConfigProbe");
            setDaemon(true);
        }

        @Override
        public void run() {
            Logger logger = Logger.getLogger(loggerName);
            try {
                while (interval > 0) {
                    Thread.sleep(interval * 1000);
                    LogManager logManager = LogManager.getLogManager();
                    long lastModified = file.lastModified();

                    if (lastModified > 0 &&
                      (lastModified != prevModified)) {
                        InputStream in = null;
                        try {
                            readConfiguration(logManager, defaultsUrl, file.toURI().toURL(), true);
                            interval = getInterval();
                            logger.log(Level.CONFIG,
                                       "logging config file reread complete, new interval is {0} secs",
                                       interval);
                        } catch (Throwable t) {
                            logger.log(Level.WARNING,
                              "exception reading logging config file",
                              t);
                        } finally {
                            if (in != null) {
                                try {
                                    in.close();
                                } catch (IOException e) {
                                    logger.log(Level.WARNING,
                                      "exception closing logging config file",
                                      e);
                                }
                            }
                        }
                        prevModified = lastModified;
                    }
                }
            } catch (InterruptedException e) {
                logger.fine("Log configuration thread interrupted.");
            } finally {
                logger.config("logging config file probe terminating");
            }
        }

    }

    /**
     * Return the probe interval.
     */
    private static long getInterval() {
        long interval = 5L;
        LogManager logManager = LogManager.getLogManager();
        String val = logManager.getProperty("com.l7tech.logging.interval");
        if (val != null) {
            try {
                interval = Long.decode(val.trim());
            } catch (NumberFormatException e) {
                e.printStackTrace(); // can't really log from this class
            }
        }
        return interval;
    }

    private static void updateState(final Properties properties) {
        readServiceNameSufficeAppenderState(properties);
        readDebugState(properties);
    }

    private static void readServiceNameSufficeAppenderState(final Properties properties) {
        String val = properties.getProperty("com.l7tech.logging.appendservicename");
        if (val != null)
            serviceNameAppenderState.set(Boolean.parseBoolean(val));
    }

    public static boolean serviceNameAppenderState() {
        return serviceNameAppenderState.get();
    }

    private static void readDebugState(final Properties properties) {
        String val = properties.getProperty("com.l7tech.logging.debug");
        if (val != null)
            debugState.set(Boolean.parseBoolean(val));
    }

    public static boolean debugState() {
        return debugState.get();
    }
}
