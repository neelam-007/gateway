package com.l7tech.remote.jini;

import com.sun.jini.start.ServiceStarter;
import net.jini.config.ConfigurationException;

import java.net.URL;
import java.util.logging.Logger;

/**
 * The class <code>Services</code> is the wrapper utility that invoks
 * configured <i>Jini</i> services.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class Services {
    public static final String SERVICES_CONFIG = "start-services.config";
    static Logger logger = Logger.getLogger(Services.class.getName());
    private static Services instance = null;

    /**
     * class cannot be instantiated
     */
    private Services() {
    }

    public static synchronized Services getInstance() {
        if (instance == null) {
            instance = new Services();
        }
        return instance;
    }

    /**
     * start reggie
     */
    public void start() throws ConfigurationException {
        URL url = getClass().getClassLoader().getResource(SERVICES_CONFIG);
        if (url == null) {
            throw new ConfigurationException("'" + SERVICES_CONFIG + "' not found");
        }
        String cfg = url.toString();
        start(new String[]{cfg});
    }

    /**
     * start reggie
     */
    public void start(String[] options) throws ConfigurationException {
        setEnvironment();
        if (options != null && options.length > 0) {
            logger.info("initializing from " + options[0]);
        } else {
            throw new ConfigurationException("Empty or null options passed");
        }
        ServiceStarter.main(options);
    }

    private void setEnvironment() {
        System.setProperty("java.protocol.handler.pkgs", "com.l7tech.remote.jini");
    }
}
