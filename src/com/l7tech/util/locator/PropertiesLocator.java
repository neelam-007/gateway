package com.l7tech.util.locator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Enumeration;


/**
 * The <code>PropertiesLocator</code> is the <code>Locator</code>
 * backed by the properties key value pairs.
 *
 * *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class PropertiesLocator extends AbstractLocator {
     /** The default resource name of the properties source.  */
    public static final String DEFAULT_PROPERTIES = "/services.properties";
    /**
     * Default constructor, uses this class cllassloader, and the default
     * service properties location
     *
     * @throws InstantiationException thrown if unable to initialize services
     */
    public PropertiesLocator() throws InstantiationException {
        this(DEFAULT_PROPERTIES, null);
    }

    /**
     * Fuill constructor
     * @throws InstantiationException
     */
    public PropertiesLocator(String resource, ClassLoader cl) throws InstantiationException {
        this.cl = cl;
        this.servicesResource = resource;
        try {
            initialize();
        } catch (Exception e) {
            throw new InstantiationException("Cannot load " + DEFAULT_PROPERTIES, e);
        }
    }

    /**
     * Special purpose runtime exception to indicate a failure loading
     * the properties.
     */
    public static final class InstantiationException
      extends RuntimeException {
        /**
         * Constructor supports a message and a nested exception.
         */
        InstantiationException(String message, Throwable nestedException) {
            super(message, nestedException);

        }
    }

    /**
     * Initialize the instance from resource using the specified classloader.
     * <code>ClassLoader</code> is optional.
     */
    private void initialize() throws IOException {
        InputStream inputStream = null;
        if (cl == null) {
            inputStream = getClass().getResourceAsStream(servicesResource);
        } else {
            inputStream = cl.getResourceAsStream(servicesResource);
        }

        try {
            properties.load(inputStream);
            for (Enumeration e = properties.keys(); e.hasMoreElements();) {
                String key = (String)e.nextElement();
                String name = (String)properties.get(key);
                Class cls =  null;
                try {
                    cls = Class.forName(name, true, cl);
                    addPair(key, cls);
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace();  // too early to use error manager
                }

            }
        } finally {
            inputStream.close();
        }
    }

    private String servicesResource;
    private final ClassLoader cl;
    private Properties properties = new Properties();

}
