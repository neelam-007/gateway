package com.l7tech.server.util;

import java.net.URL;
import java.util.Enumeration;
import java.util.Collection;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.IOException;

/**
 * ClassLoader that delegates either to it's parent or a peer for a subset of classes.
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
     * @param filterParent the classloader to delegate to for matching classes/resources
     * @param filter the package/resource prefix for delegated classes/resources
     */
    public FilterClassLoader( final ClassLoader filterParent,
                              final String filter ) {
        super();
        if (filterParent == null) throw new IllegalArgumentException("filterParent must not be null.");

        resourcePrefixes = Arrays.asList(asResource(filter));
        filteredParent = filterParent;
    }

    /**
     * Create a filter classloader with the given parent and filter.
     *
     * <p>The parent is not delegated to in the usual manner. Only requests to
     * load classes / resources that are "under" the filter prefix are delegated
     * to the parent. All other classes / resources will NOT be found.</p>
     *
     * @param parent the parent classloader
     * @param filterParent the classloader to delegate to for matching classes/resources
     * @param filters the packages/resources prefix for delegated classes/resources
     */
    public FilterClassLoader( final ClassLoader parent,
                              final ClassLoader filterParent,
                              final Collection<String> filters ) {
        super(parent);
        if (filterParent == null) throw new IllegalArgumentException("filterParent must not be null.");

        resourcePrefixes = asResources(filters);
        filteredParent = filterParent;
    }

    //- PROTECTED

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if ( propagate(asResource(name) )) {
            return filteredParent.loadClass(name);
        } else {
            return super.loadClass(name, resolve);
        }
    }

    /**
     *
     */
    protected URL findResource(String name) {
        if (propagate(asResource(name))) {
            return filteredParent.getResource(name);
        } else {
            return super.findResource(name);
        }
    }

    /**
     *
     */
    protected Enumeration<URL> findResources(String name) throws IOException {
        if (propagate(asResource(name))) {
            return filteredParent.getResources(name);
        } else {
            return super.findResources(name);
        }
    }

    //- PRIVATE

    private final ClassLoader filteredParent;
    private final Collection<String> resourcePrefixes;

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
     * Convert a collection of classes binary name to a resource path
     *
     * @return the /resource/path
     */
    private Collection<String> asResources(Collection<String> pathOrClassNames) {
        Collection<String> resources = new ArrayList<String>();

        for ( String pathOrClassName : pathOrClassNames ) {
            resources.add( asResource( pathOrClassName ) );    
        }

        return resources;
    }

    /**
     * Check if the request should be passed to the parent.
     *
     * @param resourcePath The path to check
     * @return true to delegate
     */
    private boolean propagate(String resourcePath) {
        boolean propagate = false;

        for ( String resourcePrefix : resourcePrefixes ) {
            if ( resourcePath.startsWith(resourcePrefix) ) {
                propagate = true;
                break;
            }
        }

        return propagate;
    }
}
