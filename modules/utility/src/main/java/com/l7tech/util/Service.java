package com.l7tech.util;

import com.l7tech.common.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implement a lookup of provider implementations using the technique described in the
 * JAR File Specification.
 *
 * <p>In normal usage you would not create an instance of this class.</p>
 *
 * <p>Commonly the provider instances would be factories that can create the objects
 * used by the application.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public final class Service {

    //- PUBLIC

    /**
     * Get the provider implementations for the given service.
     *
     * <p>Locate and instantiate providers of the given type.</p>
     *
     * @param serviceClass the service interface
     * @return an Iterator of objects castable to serviceClass
     */
    public static Iterator providers(final Class serviceClass) {
        return providers(serviceClass, Service.class.getClassLoader());
    }

    /**
     * Get the provider implementations for the given service.
     *
     * <p>Locate and instantiate providers of the given type.</p>
     *
     * @param serviceClass the service interface
     * @param classLoader the class loader to use
     * @return an Iterator of objects castable to serviceClass
     */
    public static Iterator providers(final Class serviceClass, final ClassLoader classLoader) {
        Iterator iter = Collections.EMPTY_LIST.iterator();
        Collection classNameUrlPairs = lookupProviderClassNames(SERVICE_PREFIX + serviceClass.getName(), classLoader);
        if(!classNameUrlPairs.isEmpty()) {
            HashSet providers = new LinkedHashSet();
            for (Iterator classNameIter=classNameUrlPairs.iterator(); classNameIter.hasNext();) {
                String[] pair = (String[]) classNameIter.next();
                String className = pair[0];
                String urlStr = pair[1];
                Object instance = instantiateProvider(serviceClass, classLoader, className, urlStr);
                if(instance!=null) providers.add(instance);
            }
            iter = Collections.unmodifiableCollection(providers).iterator();
        }
        return iter;
    }

    /**
     * Get the provider implementation names for the given service.
     *
     * <p>Locate providers of the given type.</p>
     *
     * @param serviceClass the service interface
     * @return an Iterator of String class names
     */
    public static Iterator providerClassNames(final Class serviceClass) {
        return providerClassNames(serviceClass, Service.class.getClassLoader());
    }

    /**
     * Get the provider implementation names for the given service.
     *
     * <p>Locate providers of the given type.</p>
     *
     * @param serviceClass the service interface
     * @param classLoader the class loader to use
     * @return an Iterator of String class names
     */
    public static Iterator providerClassNames(final Class serviceClass, final ClassLoader classLoader) {
        Iterator iter = Collections.EMPTY_LIST.iterator();
        Collection classNameUrlPairs = lookupProviderClassNames(SERVICE_PREFIX + serviceClass.getName(), classLoader);
        if(!classNameUrlPairs.isEmpty()) {
            HashSet providers = new LinkedHashSet();
            for (Iterator classNameIter=classNameUrlPairs.iterator(); classNameIter.hasNext();) {
                String[] pair = (String[]) classNameIter.next();
                String className = pair[0];
                providers.add(className);
            }
            iter = Collections.unmodifiableCollection(providers).iterator();
        }
        return iter;
    }

    //- PRIVATE

    /**
     * Logger for the class
     */
    private static final Logger logger = Logger.getLogger(Service.class.getName());

    /**
     * The prefix for service implementations.
     */
    private static final String SERVICE_PREFIX = "META-INF/services/";

    /**
     * Locate the provider classes
     *
     * @return a set of provider class name / url pairs (String[])
     */
    private static Collection lookupProviderClassNames(String resourcePath, ClassLoader cl) {
        Map classes = new LinkedHashMap();
        try {
            Iterator resUrlIter = new EnumerationIterator(cl.getResources(resourcePath));
            while (resUrlIter.hasNext()) {
                URL url = (URL) resUrlIter.next();
                InputStream resStream = null;
                try {
                    resStream = url.openStream();

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    IOUtils.copyStream(resStream, baos);
                    StringTokenizer dataStr = new StringTokenizer(new String(baos.toByteArray(), "UTF-8"));
                    if(dataStr.hasMoreTokens()) {
                        String className = dataStr.nextToken();
                        if(classes.containsKey(className)) {
                            logger.warning("Duplicate provider class definition in: " + url.toString());
                        }
                        else {
                            classes.put(className, new String[]{className, url.toString()});
                        }
                    }
                    else {
                        logger.warning("Could not find class definition in: " + url.toString());
                    }
                }
                catch(IOException ioe) {
                    logger.log(Level.WARNING, "IO error when reading provider information from '" + url.toString() + "'.", ioe);
                }
                finally {
                    if(resStream!=null) try{resStream.close();}catch(IOException ioe){;}
                }
            }
        }
        catch(IOException ioe) {
            logger.log(Level.WARNING, "IO error when reading service information.", ioe);
        }
        return classes.values();
    }

    /**
     * Create the provider instance.
     *
     * @return the instance or null.
     */
    private static Object instantiateProvider(Class desiredType, ClassLoader classLoader, String className, String sourceUrl) {
        Object instance = null;
        try {
            Class clazz = Class.forName(className, true, classLoader);
            if(desiredType.isAssignableFrom(clazz)) {
                instance = clazz.newInstance();
            }
            else {
                logger.warning("The provider class '"+className+"', defined in '"+sourceUrl+"', is not of the correct type.");
            }
        }
        catch(ClassNotFoundException cnfe) {
            logger.warning("The provider class '"+className+"', defined in '"+sourceUrl+"', was not found.");
        }
        catch(IllegalAccessException iae) {
            logger.warning("The provider class '"+className+"', defined in '"+sourceUrl+"', has an constructor that cannot be accessed.");
        }
        catch(InstantiationException ie) {
            logger.warning("The provider class '"+className+"', defined in '"+sourceUrl+"', cannot be instantiated (could be abstract or an interface, etc).");
        }
        return instance;
    }
}
