package com.l7tech.server.util;

import com.l7tech.util.IOUtils;

import java.net.URL;
import java.io.InputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * ClassLoader that loads classes as resources from parent loaders.
 *
 * <p>This allows you to load classes with visiblity to other classes
 * that they would not usually be able to access.</p>
 *
 * @since 4.7
 */
public class ResourceClassLoader extends ClassLoader {


    //- PUBLIC

    /**
     * Create a resource classloader with the given parent.
     *
     * <p>The parent is not delegated to in the usual manner..</p>
     *
     * @param parent the classloader to delegate to for classes and resources
     * @param resourcePrefix the prefix of classes that must NOT be loaded from parent classloader
     */
    public ResourceClassLoader( final ClassLoader parent, final Collection<String> resourcePrefix ) {
        super(parent);

        if (resourcePrefix == null) throw new IllegalArgumentException("resourcePrefix must not be null.");

        this.resourcePrefixes = resourcePrefix;
    }

    //- PROTECTED

    /**
     * As this class does not support the usual delegation model, we override loadClass.
     */
    @Override
    protected synchronized Class<?> loadClass( final String name, final boolean resolve) throws ClassNotFoundException {
        if ( isResourceClass( name ) ) {
            if ( logger.isLoggable( Level.FINER ) ) {
                logger.log( Level.FINER, "Loading class ''{0}'' as resource.", name );
            }

            Class<?> c = findClass( name );
            if ( resolve ) {
                resolveClass(c);
            }

            return c;
        } else {
            ClassLoader parent = getParent();
            if ( parent != null ) {
                return parent.loadClass(name);
            } else {
                throw new ClassNotFoundException(name);
            }
        }
    }

    @Override
    protected Class<?> findClass( final String name ) throws ClassNotFoundException {
        try {
            String resName = name.replace(".", "/").concat(".class");
            ClassLoader parent = getParent();
            URL resUrl = parent==null ? null : parent.getResource(resName);
            if (resUrl == null)
                throw new ClassNotFoundException("Resource not found for class '" + name + "'.");

            InputStream resIn = resUrl.openStream();
            byte[] classData = IOUtils.slurpStream(resIn, 102400);
            return defineClass(name, classData, 0, classData.length);
        } catch(IOException ioe) {
            throw new ClassNotFoundException("Error loading resource for class '" + name + "'.", ioe);
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( ResourceClassLoader.class.getName() );

    private final Collection<String> resourcePrefixes;

    private boolean isResourceClass( final String name ) {
        boolean resourceClass = false;

        for ( String prefix : resourcePrefixes ) {
            if ( name.startsWith(prefix) ) {
                resourceClass = true;
                break;
            }
        }

        return resourceClass;
    }

}
