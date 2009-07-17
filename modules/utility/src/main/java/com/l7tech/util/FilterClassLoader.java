package com.l7tech.util;

import java.net.URL;
import java.util.Enumeration;
import java.util.Collection;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;

/**
 * ClassLoader that delegates either to it's parent or a peer for a subset of classes.
 */
public class FilterClassLoader extends ClassLoader {

    //- PUBLIC

    /**
     * Create a filter classloader with the default parent and given filter.
     *
     * <p>The parent is not delegated to in the usual manner. Requests to
     * load classes / resources that are "under" the filter prefix are not
     * delegated to the parent.</p>
     *
     * @param filterParent The classloader to delegate to for matching classes/resources
     * @param filter The package/resource prefix for delegated classes/resources
     * @param filterResources True to filter resources as well as classes
     */
    public FilterClassLoader( final ClassLoader filterParent,
                              final String filter,
                              final boolean filterResources ) {
        super();

        this.resourcePrefixes = Arrays.asList(asResource(filter));
        this.filteredParent = filterParent;
        this.filterResources = filterResources;
    }

    /**
     * Create a filter classloader with the given parent and filter.
     *
     * <p>The parent is not delegated to in the usual manner. Requests to
     * load classes / resources that are "under" the filter prefix are not
     * delegated to the parent.</p>
     *
     * @param parent The parent classloader
     * @param filterParent The classloader to delegate to for matching classes/resources
     * @param filters The packages/resources prefix for delegated classes/resources
     */
    public FilterClassLoader( final ClassLoader parent,
                              final ClassLoader filterParent,
                              final Collection<String> filters,
                              final boolean filterResources ) {
        super(parent);

        this.resourcePrefixes = asResources(filters);
        this.filteredParent = filterParent;
        this.filterResources = filterResources;
    }

    //- PROTECTED

    @Override
    protected synchronized Class<?> loadClass( final String name, final boolean resolve ) throws ClassNotFoundException {
        if ( propagate(asResource(name) )) {
            if ( filteredParent != null ) {
                return filteredParent.loadClass(name);
            } else {
                throw new ClassNotFoundException(name);
            }
        } else {
            return super.loadClass(name, resolve);
        }
    }

    /**
     *
     */
    @Override
    protected URL findResource( final String name ) {
        if ( filterResources && propagate(asResource(name)) ) {
            return filteredParent != null ? filteredParent.getResource(name) : null;
        } else {
            return super.findResource(name);
        }
    }

    /**
     *
     */
    @Override
    protected Enumeration<URL> findResources( final String name ) throws IOException {
        if ( filterResources && propagate(asResource(name)) ) {
            return filteredParent != null ? filteredParent.getResources(name) : Collections.enumeration(Collections.<URL>emptyList());
        } else {
            return super.findResources(name);
        }
    }

    //- PRIVATE

    private final ClassLoader filteredParent; // may be null
    private final Collection<String> resourcePrefixes;
    private final boolean filterResources;

    /**
     * Convert a classes binary name to a resource path
     *
     * @return the /resource/path
     */
    private String asResource( final String pathOrClassName ) {
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
    private Collection<String> asResources( final Collection<String> pathOrClassNames ) {
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
    private boolean propagate( final String resourcePath ) {
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
