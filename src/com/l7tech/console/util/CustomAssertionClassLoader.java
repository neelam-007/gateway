package com.l7tech.console.util;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;

import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.common.util.ExceptionUtils;

/**
 * ClassLoader for loading CustomAssertion classes from the connected SSG.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class CustomAssertionClassLoader extends ClassLoader {
    protected static final Logger logger = Logger.getLogger(CustomAssertionClassLoader.class.getName());

    //- PUBLIC

    /**
     * Create a new CustomAssertionClassLoader.
     */
    public CustomAssertionClassLoader() {
        super();
    }

    /**
     * Create a new CustomAssertionClassLoader.
     *
     * @param parentLoader The parent class loader
     */
    public CustomAssertionClassLoader(ClassLoader parentLoader) {
        super(parentLoader);
    }

    //- PROTECTED

    protected byte[] findResourceBytes(String path) {
        Registry registry = Registry.getDefault();
        if (registry == null) {
            logger.warning("Unable to load custom/modular assertion class or resource: No default Registry available");
            return null;
        }

        CustomAssertionsRegistrar car = null;
        try {
            car = registry.getCustomAssertionsRegistrar();
        } catch (IllegalStateException e) {
            logger.log(Level.WARNING, "Unable to load custom/modular assertion class or resource: " + ExceptionUtils.getMessage(e), e);
        }
        if (car == null) {
            logger.warning("Unable to load custom/modular assertion class or resource: No CustomAssertionRegistrar available");
            return null;
        }

        return car.getAssertionResourceBytes(path);
    }

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

    protected URL findResource(final String name) {
        final byte[] resourceBytes = findResourceBytes(name);
        if (resourceBytes == null)
            return null;
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<URL>() {
                public URL run() throws Exception {
                    return makeUrl(name, resourceBytes);
                }
            });
        } catch (PrivilegedActionException e) {
            logger.log(Level.WARNING, "Unable to load remote resource: " + ExceptionUtils.getMessage(e), e);
            return null;
        }
    }

    private URL makeUrl(String name, final byte[] resourceBytes) throws MalformedURLException {
        return new URL("file", null, -1, name, new URLStreamHandler() {
                protected URLConnection openConnection(URL u) throws IOException {
                    return new URLConnection(u) {
                        public void connect() throws IOException { }
                        public InputStream getInputStream() throws IOException {
                            return new ByteArrayInputStream(resourceBytes);
                        }
                    };
                }
            });
    }

    /**
     * Find the class from the SSG.
     *
     * @param name The class name
     * @return The class
     * @throws ClassNotFoundException if not available from the attached SSG
     */
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = name.replace('.','/') + ".class";
        byte[] classBytes = findResourceBytes(path);
        if (classBytes == null || classBytes.length < 1)
            throw new ClassNotFoundException(name);
        return defineClass(name, classBytes, 0, classBytes.length);
    }
}
