package com.l7tech.common.util;

import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
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

    /** this class cannot be instantiated */
    private JdkLoggerConfigurator() {}

    /**
     * initialize logging, try different strategies. First look for the system
     * property <code>java.util.logging.config.file</code>, then look for
     * <code>logging.properties</code>. If that fails fall back to the 
     * application-specified config file (ie,
     * <code>com/l7tech/console/resources/logging.properties</code>).
     *
     * @param classname the classname to use for logging info about which logging.properties was found
     * @param shippedLoggingProperties the logging.properties to use if no locally-customized file is found,
     *                                 for example "com/l7tech/console/resources/logging.properties"
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
            for (Iterator iterator = configCandidates.iterator(); iterator.hasNext();) {
                configCandidate = (String)iterator.next();
                final File file = new File(configCandidate);

                if (file.exists()) {
                    in = file.toURL().openStream();
                    if (in != null) {
                        LogManager.getLogManager().readConfiguration(in);
                        configFound = true;
                        break;
                    }
                }
                ClassLoader cl = JdkLoggerConfigurator.class.getClassLoader();
                in = cl.getResourceAsStream(configCandidate);
                if (in != null) {
                    LogManager.getLogManager().readConfiguration(in);
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
}
