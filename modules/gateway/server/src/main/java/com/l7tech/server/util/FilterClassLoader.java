package com.l7tech.server.util;

import java.net.URL;
import java.util.Enumeration;
import java.util.Collections;
import java.io.IOException;

/**
 * ClassLoader that delegates to it's parent only for one package (prefix).
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class FilterClassLoader extends ClassLoader {

    //- PUBLIC

    /**
     * Create a filter classloader with the given parent and filter.
     *
     * <p>The parent is not delegated to in the usual manner. Only requests to
     * load classes / resources that are "under" the filter prefix are delegated
     * to the parent. All other classes / resources will NOT be found.</p>
     *
     * @param parent the classloader to delegate to for matching classes/resources
     * @param filter the package/resource prefix for delegated classes/resources
     */
    public FilterClassLoader(ClassLoader parent, String filter) {
        super();
        if (parent == null) throw new IllegalArgumentException("parent must not be null.");
        resourcePrefix = asResource(filter);
        filteredParent = parent;
    }

    //- PROTECTED

    /**
     *
     */
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (propagate(asResource(name))) {
            return filteredParent.loadClass(name);
        }
        throw new ClassNotFoundException(name);
    }

    /**
     *
     */
    protected URL findResource(String name) {
        if (propagate(asResource(name))) {
            return filteredParent.getResource(name);
        }
        return null;
    }

    /**
     *
     */
    protected Enumeration<URL> findResources(String name) throws IOException {
        if (propagate(asResource(name))) {
            return filteredParent.getResources(name);
        }
        return Collections.enumeration(Collections.EMPTY_LIST);
    }

    //- PRIVATE

    private final ClassLoader filteredParent;
    private final String resourcePrefix;

    /**
     * Convert a classes binary name to a resource path
     *
     * @return the /resource/path
     */
    private String asResource(String pathOrClassName) {
        String resource = null;

        if (pathOrClassName != null) {
            String res = pathOrClassName.replace('.', '/');
            if (!res.startsWith("/")) {
                res = "/" + res;
            }
            resource = res;
        }

        return resource;
    }

    /**
     * Check if the request should be passed to the parent.
     *
     * @param resourcePath The path to check
     * @return true to delegate
     */
    private boolean propagate(String resourcePath) {
        return resourcePrefix != null && resourcePath.startsWith(resourcePrefix);
    }
}
