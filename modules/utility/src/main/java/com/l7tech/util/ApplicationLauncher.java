package com.l7tech.util;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.JarURLConnection;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collection;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * Application launcher that supports automatic classpath construction.
 *
 * <p>This launcher supports "proxying" of class-path information from a
 * referenced Jar file.</p>
 *
 * <p>This can be used to launch applications without having to duplicate
 * libraries and configuration. This would commonly be due to a Jar file
 * containing multiple applications.</p>
 *
 * <p>To use this utility, include this classes Jar in the main classpath and
 * use this class as the main class. An example configuration is:</p>
 *
 * <code>
 *   Main-Class: com.l7tech.util.ApplicationLauncher
 *   Class-Path: lib/layer7-utility.jar
 *   X-Layer7-Jar: ../Application.jar
 *   X-Layer7-Main-Class: com.l7tech.application.Main
 * </code>
 *
 * <p>In this case the "Application.jar" contains a "Class-Path" that is the
 * applications classpath (with paths relative to it's own location) and the
 * "X-Layer7-Main-Class" is then invoked.</p>
 *
 * @author Steve Jones
 */
public class ApplicationLauncher {

    //- PUBLIC

    /**
     * Launch the application with the given arguments.
     */
    public static void main(final String[] args) throws Throwable {
        // Locate configuration from Manifest
        String[] jarAttrs = getManifestAttributes();
        String jarName = jarAttrs[0];
        String className = jarAttrs[1];

        if ( jarName != null && className != null ) {
            File applicationJarFile = new File(jarName);
            if ( !applicationJarFile.isFile() ) {
                throw new RuntimeException("Could not find/access application Jar file '"+jarName+"'.");               
            }

            // Collect extra Jar URLs
            Collection<URL> jarUrls = getJarClasspathUrls( applicationJarFile );
            logger.log( Level.FINEST, "Using classpath ''{0}''.", jarUrls );

            // Add Jar URLs to classpath
            addURLs( jarUrls );

            // Invoke application main
            Method main = Class.forName( className ).getMethod("main", new Class[]{(new String[0]).getClass()});
            try {
                main.invoke(null, new Object[]{ args });
            } catch (InvocationTargetException ite) {
                throw ite.getCause();
            }
        } else {
            throw new RuntimeException( "Missing manifest attributes." );            
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ApplicationLauncher.class.getName());

    private static final String RESOURCE_MANIFEST = "META-INF/MANIFEST.MF";

    private static final String ATTR_CLASSPATH = "Class-Path";
    private static final String ATTR_L7_MAIN = "X-Layer7-Main-Class";
    private static final String ATTR_L7_JAR = "X-Layer7-Jar";

    private static String[] getManifestAttributes() throws IOException {
        String jarName;
        String className;
        URL jarUrl = ApplicationLauncher.class.getClassLoader().getResource( RESOURCE_MANIFEST );
        logger.log( Level.FINE, "Using Jar URL ''{0}''.", jarUrl);

        if ( jarUrl != null ) {
            JarURLConnection jarUrlConn = (JarURLConnection) jarUrl.openConnection();
            JarFile jarFile = jarUrlConn.getJarFile();

            jarName = getManifestAttribute( jarFile, ATTR_L7_JAR );
            logger.log( Level.FINE, "Using application Jar file ''{0}''.", jarName );

            className = getManifestAttribute( jarFile, ATTR_L7_MAIN );
            logger.log( Level.FINE, "Using application main class ''{0}''.", className );

            jarFile.close();
        } else {
            throw new RuntimeException( "Could not access Jar Manifest." );
        }

        return new String[]{ jarName, className };
    }

    /**
     * Get list of URLs for the classpath (main Jar first, then in declared Class-Path order) 
     */
    private static Collection<URL> getJarClasspathUrls( final File applicationJarFile ) throws IOException {
        Collection<URL> jarUrls = new ArrayList<URL>();

        URL baseURL = applicationJarFile.toURI().toURL();
        jarUrls.add( baseURL );

        JarFile applicationJar = new JarFile( applicationJarFile );
        Manifest appManifest = applicationJar.getManifest();
        if ( appManifest != null ) {
            Attributes appAttributes = appManifest.getMainAttributes();
            String classpath = appAttributes.getValue( ATTR_CLASSPATH );
            for ( String relativeUrl : classpath.split( " " )) {
                jarUrls.add( new URL( baseURL, relativeUrl ) );
            }
        } else {
            logger.log( Level.WARNING, "Manifest not found in application Jar (''{0}'').", applicationJarFile.getAbsolutePath() );
        }

        return jarUrls;
    }

    /**
     * Get an attribute from the manifest of the given Jar file.
     */
    private static String getManifestAttribute( final JarFile jarFile, final String attributeName ) throws IOException {
        String attributeValue = null;

        Manifest manifest = jarFile.getManifest();
        if ( manifest != null ) {
            Attributes attributes = manifest.getMainAttributes();
            if ( attributes != null ) {
                attributeValue = attributes.getValue( attributeName );
            }
        }

        return attributeValue;
    }

    /**
     * Add Jars to the system classpath.
     *
     * TODO [steve] Investigate better method of launching applications using a child ClassLoader
     */
    private static void addURLs( final Collection<URL> jarUrls ) {
		URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        try {
            Method method = URLClassLoader.class.getDeclaredMethod( "addURL", new Class[]{ URL.class } );
            method.setAccessible( true );
            for ( URL jarUrl : jarUrls ) {
                method.invoke( classLoader, jarUrl );
            }
        } catch ( NoSuchMethodException nsme ) {
            throw new RuntimeException( "Error configuring classpath", nsme );
        } catch ( IllegalAccessException iae ) {
            throw new RuntimeException( "Error configuring classpath", iae );
        } catch ( InvocationTargetException ite ) {
            throw new RuntimeException( "Error configuring classpath", ite.getCause() );
        }
    }
}
