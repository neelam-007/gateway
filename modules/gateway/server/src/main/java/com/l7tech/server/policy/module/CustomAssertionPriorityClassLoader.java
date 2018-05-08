package com.l7tech.server.policy.module;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a class loader for certain custom assertion jar.<br/>
 * This class loader loads classes from the jars included within the custom assertion first then looks for them in the
 * parent class loader. The standard CustomAssertionClassLoader looks in the parent class loader first.
 */
public class CustomAssertionPriorityClassLoader extends CustomAssertionClassLoader {
    protected static final Logger logger = Logger.getLogger(CustomAssertionPriorityClassLoader.class.getName());

    public CustomAssertionPriorityClassLoader(URL[] urls) {
        super(urls);
    }

    public CustomAssertionPriorityClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // First, check if the class has already been loaded
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                try {
                    //find the class in one of the jars included in the custom assertion. If it is found it will be cached
                    findClass(name);
                } catch (ClassNotFoundException e) {
                    // The class was not included in any of the embedded jars so fail silently to look in the parent.
                    logger.log(Level.FINEST, () -> "Could not find class in custom assertion. Check parent class loader. Classname: '" + name + "'");
                }
            }
            //Do not return c because super.loadClass could potentially do more work if resolve is true
            return super.loadClass(name, resolve);
        }
    }

    @Override
    public URL getResource(String name) {
        // attempt to get a resource within the custom assertion jar.
        URL url = findResource(name);
        if (url == null) {
            //the resource was not found in the custom assertion jar so check the parent class loader
            url = super.getResource(name);
        }
        return url;
    }
}
