package com.l7tech.console.util;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.ResourceUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * ClassLoader for loading CustomAssertion classes from the connected SSG.
 */
public class CustomAssertionClassLoader extends ClassLoader {

    //- PUBLIC

    /**
     * Create a new CustomAssertionClassLoader.
     */
    public CustomAssertionClassLoader() {
    }

    /**
     * Create a new CustomAssertionClassLoader.
     *
     * @param parentLoader The parent class loader
     */
    public CustomAssertionClassLoader(ClassLoader parentLoader) {
        super(parentLoader);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        ClassLoader parent = getParent();
        if (parent == null)
            return super.getResourceAsStream(name);

        InputStream ret = parent.getResourceAsStream(name);
        if (ret != null)
            return ret;

        final byte[] resourceBytes = findResourceBytes(name);
        return resourceBytes == null ? null : new ByteArrayInputStream(resourceBytes);
    }

    //- PROTECTED

    protected static final Logger logger = Logger.getLogger(CustomAssertionClassLoader.class.getName());

    public void preCacheResourceBytes( final Collection<String> rawPaths ) {
        Registry registry = Registry.getDefault();
        CustomAssertionsRegistrar car = null;
        try {
            car = registry.isAdminContextPresent() ?
                    registry.getCustomAssertionsRegistrar() :
                    null;
        } catch (IllegalStateException e) {
            logger.log(Level.WARNING, "Unable to load custom/modular assertion class or resource: " + ExceptionUtils.getMessage(e), e);
        }
        if (car == null) {
            logger.warning("Unable to load custom/modular assertion class or resource: No CustomAssertionRegistrar available");
            return;
        }

        ByteArrayInputStream bais = null;
        try {
            byte[] dataPairsAsBytes = car.getAssertionResourceDataAsBytes(rawPaths);
            bais = new ByteArrayInputStream(dataPairsAsBytes);
            GZIPInputStream gzipIn = new GZIPInputStream(bais);
            ObjectInputStream objectIn = new ObjectInputStream(gzipIn);
            Collection<Pair<String, CustomAssertionsRegistrar.AssertionResourceData>> dataPairs = (Collection<Pair<String, CustomAssertionsRegistrar.AssertionResourceData>>) objectIn.readObject();
            objectIn.close();

            for (Pair<String,CustomAssertionsRegistrar.AssertionResourceData> dataPair : dataPairs) {
                processResourceData(dataPair.left, dataPair.right);
            }
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.WARNING, "Unable to load custom/modular assertion class or resource: " + ExceptionUtils.getMessage(e), e);
        } finally {
            ResourceUtils.closeQuietly(bais);
        }
    }

    protected byte[] findResourceBytes( final String rawPath ) {
        Registry registry = Registry.getDefault();
        if (registry == null) {
            logger.warning("Unable to load custom/modular assertion class or resource: No default Registry available");
            return null;
        }

        CustomAssertionsRegistrar car = null;
        try {
            car = registry.isAdminContextPresent() ?
                    registry.getCustomAssertionsRegistrar() :
                    null;
        } catch (IllegalStateException e) {
            logger.log(Level.WARNING, "Unable to load custom/modular assertion class or resource: " + ExceptionUtils.getMessage(e), e);
        }
        if (car == null) {
            logger.warning("Unable to load custom/modular assertion class or resource: No CustomAssertionRegistrar available");
            return null;
        }

        String path = cleanPath( rawPath );
        byte[] cachedData = getCachedResourceBytes( path );
        if ( cachedData != null ) {
            if ( logger.isLoggable( Level.FINE )) {
                logger.log( Level.FINE, "Cache hit, loading custom/modular assertion resource ''{0}''.", path );
            }
            return cachedData;
        }

        if ( isPackageCached( path ) ) {
            // This prevents up making requests to the server for classes that
            //  don't exist (e.g. resource bundle classes, palette nodes, etc)
            return null;
        }

        try {
            if ( logger.isLoggable( Level.FINE )) {
                logger.log( Level.FINE, "Cache miss, loading custom/modular assertion resource ''{0}''.", path );
            }
            return processResourceData( path, car.getAssertionResourceData(path) );
        } catch (RuntimeException e) {
            ErrorManager.getDefault().notify( Level.WARNING, e, "Error loading custom/modular assertion class or resource." );
            return null;
        }
    }

    /**
     * Find the class from the SSG.
     *
     * @param name The class name
     * @return The class
     * @throws ClassNotFoundException if not available from the attached SSG
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = name.replace('.','/') + ".class";
        byte[] classBytes = findResourceBytes(path);
        if (classBytes == null || classBytes.length < 1)
            throw new ClassNotFoundException(name);

        // Check package and define if required
        // TODO package sealing support?
        int i = name.lastIndexOf( '.' );
        if ( i != -1 ) {
            String pkgname = name.substring( 0, i );
            Package pkg = getPackage( pkgname );
            if ( pkg == null ) {
                definePackage( pkgname, null, null, null, null, null, null, null );
            }
        }

        return defineClass(name, classBytes, 0, classBytes.length);
    }

    @Override
    protected URL findResource(final String name) {
        final byte[] resourceBytes = findResourceBytes(name);
        if (resourceBytes == null)
            return null;
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<URL>() {
                @Override
                public URL run() throws Exception {
                    return makeUrl(name, resourceBytes);
                }
            });
        } catch (PrivilegedActionException e) {
            logger.log(Level.WARNING, "Unable to load remote resource: " + ExceptionUtils.getMessage(e), e);
            return null;
        }
    }

    //- PRIVATE

    private final Map<String,byte[]> resourceMap = new ConcurrentHashMap<String,byte[]>();
    private final Set<String> resourcePackages = Collections.synchronizedSet( new HashSet<String>() );

    private byte[] getCachedResourceBytes( final String resourcePath ) {
        return resourceMap.get( resourcePath );
    }

    private boolean isPackageCached( final String resourcePath ) {
        return resourcePackages.contains( getPackagePath( resourcePath ) );        
    }

    private String cleanPath( final String path ) {
        String cleanPath = path;

        if ( cleanPath != null && cleanPath.startsWith( "/" ) ) {
            cleanPath = cleanPath.substring( 1 );                       
        }

        return cleanPath;
    }

    private String getPackagePath( final String path ) {
        String packagePath = path;

        if ( packagePath.startsWith("/") ) {
            packagePath = packagePath.substring( 1 );
        }

        int index = packagePath.lastIndexOf( '/' );
        if ( index >= 0 ) {
            packagePath = packagePath.substring( 0, index );
        }

        return packagePath;
    }

    private byte[] processResourceData( final String resourcePath,
                                        final CustomAssertionsRegistrar.AssertionResourceData resourceData ) {
        byte[] data = null;

        if ( resourceData != null ) {
            if ( resourceData.isGzipTar() ) {
                unGzipTar(resourceData);
                data = getCachedResourceBytes( resourcePath );
            } else {
                data = resourceData.getData();
            }
        }

        return data;
    }

    private URL makeUrl(String name, final byte[] resourceBytes) throws MalformedURLException {
        return new URL("file", null, -1, name, new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL u) throws IOException {
                    return new URLConnection(u) {
                        @Override
                        public void connect() throws IOException { }
                        @Override
                        public InputStream getInputStream() throws IOException {
                            return new ByteArrayInputStream(resourceBytes);
                        }
                    };
                }
            });
    }

    private void unGzipTar(final CustomAssertionsRegistrar.AssertionResourceData resourceData) {
        TarArchiveInputStream tarIn = null;
        try {
            tarIn = new TarArchiveInputStream(new GZIPInputStream( new ByteArrayInputStream( resourceData.getData() )));
            TarArchiveEntry entry;
            while ( (entry = tarIn.getNextTarEntry()) != null ) {
                String path = entry.getName();
                byte[] entryData = IOUtils.slurpStream( tarIn );
                if ( logger.isLoggable( Level.FINE )) {
                    logger.log( Level.FINE, "Caching custom/modular assertion resource ''{0}''.", path );
                }
                resourceMap.put( path, entryData );
                resourcePackages.add( getPackagePath(path) );
            }
        } catch (IOException e) {
            logger.log( Level.WARNING, "Error processing assertion class data.", e );
        } finally {
            ResourceUtils.closeQuietly( tarIn );
        }
    }
}
