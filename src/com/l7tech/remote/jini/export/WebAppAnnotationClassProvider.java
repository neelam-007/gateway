/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.remote.jini.export;

import net.jini.loader.ClassAnnotation;
import net.jini.loader.pref.PreferredClassProvider;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;

/**
 * The <code>WebAppAnnotationClassProvider</code> is the implementation of the
 * {@link java.rmi.server.RMIClassLoaderSpi} service. This implementaton overrides the
 * jini {@link PreferredClassProvider} so only implementations of {@link ClassAnnotation}
 * are considered, and if that is empty the property <code>java.rmi.server.codebase</code> is
 * used.
 * <p/>
 * This effectively skips the step of creating the class annotation by using the
 * {@link java.net.URLClassLoader#getURLs()},  if the classloader extends from URLClassLoader, as
 * Tomcat's WebAppClassloader does.  In the case  of Tomcat, that annotation will include everything
 * in WEB-INF/lib, common/lib, etc.  For RMI purposes, it's entirely useless anyway, since it doesn't
 * contain a url to your publicly accessible classes for  RMI download.
 * <p/>
 * In runtime this class gets registered  using the jar service provider spec, that is, in the
 * jar META-INF/services directory (the META-INF/services is the location for the service
 * provider configuration files).
 *
 * @author emil
 * @version Aug 31, 2004
 * @see sun.misc.Service
 */
public class WebAppAnnotationClassProvider extends PreferredClassProvider {
    /**
     * provider logger
     */
    private static final Logger logger = Logger.getLogger(WebAppAnnotationClassProvider.class.getName());

    /**
     * table of "local" class loaders
     */
    private static final Map localLoaders =
      Collections.synchronizedMap(new WeakHashMap());

    static {
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                for (ClassLoader loader = ClassLoader.getSystemClassLoader();
                     loader != null;
                     loader = loader.getParent()) {
                    localLoaders.put(loader, null);
                }
                return null;
            }
        });
    }

    /**
     * Returns the annotation string (representing a location for the class definition) that
     * RMI will use to annotate the class descriptor when marshalling objects of the given class.
     * This implementation considers only class loaders that are implementations of
     * {@link ClassAnnotation} (the one of our RMI class loaders).
     *
     * @param cl the class to obtain the annotation for
     * @return a string to be used to annotate the given class when it gets marshalled, or null
     */
    public String getClassAnnotation(Class cl) {
        String name = cl.getName();
        int nameLength = name.length();
        if (nameLength > 0 && name.charAt(0) == '[') {
            // skip past all '[' characters (see bugid 4211906)
            int i = 1;
            while (nameLength > i && name.charAt(i) == '[') {
                i++;
            }
            if (nameLength > i && name.charAt(i) != 'L') {
                return null;
            }
        }

        String annotation = getLoaderAnnotation(getClassLoader(cl));
        // logger.finest("getClassAnnotation '" + cl + "' returns annotation '" + (annotation == null ? "null" : annotation) + "'");
        return annotation;

    }

    /**
     * Returns the annotation string for the specified class loader
     * (possibly null). Only the {@link ClassAnnotation} implementations
     * are considered
     */
    private String getLoaderAnnotation(ClassLoader loader) {

        if (isLocalLoader(loader)) {
            return getClassAnnotation(loader);
        }
        String annotation = null;
        if (loader instanceof ClassAnnotation) {
            annotation = ((ClassAnnotation)loader).getClassAnnotation();
        } else {
            // everything else is ignored
        }
        if (annotation != null) {
            return annotation;
        } else {
            return getClassAnnotation(loader);
        }
    }

    /**
     * Return true if the given loader is the system class loader or
     * its parent (i.e. the loader for installed extensions) or the null
     * class loader
     */
    private static boolean isLocalLoader(ClassLoader loader) {
        return (loader == null || localLoaders.containsKey(loader));
    }

    private static ClassLoader getClassLoader(final Class c) {
        return (ClassLoader)AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() { return c.getClassLoader(); }
        });
    }

}