package com.l7tech.console.policy;

import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.common.util.ExceptionUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * A ClassLoader that knows how to load classes from inside an assertion module installed on the Gateway.
 * <p/>
 * <b>Note:</b>  This class loader can load resources from the module, but only if you load them with
 * getResourceAsStream().  getResource() will not find anything because no URL can be constructed for
 * such resources.
 */
class ConsoleRemoteAssertionModuleClassLoader extends ClassLoader {
    private final ClusterStatusAdmin cluster;
    private final String moduleFilename;

    private Map<String, byte[]> cache = new ConcurrentHashMap<String, byte[]>();

    /**
     * Create a loader that will load classes from the specified named assertion module on the Gateway, accessed
     * via the specified ClusterStatusAdmin proxy instance, and using the specified parent class loader
     * for delegation.
     *
     * @param parent  the parent class loader.  Required.
     * @param cluster the connected Gateway instance from which to load classes.  Required.
     * @param moduleFilename the name of the module of interest.  Required.
     */
    public ConsoleRemoteAssertionModuleClassLoader(ClassLoader parent, ClusterStatusAdmin cluster, String moduleFilename) {
        super(parent);
        this.cluster = cluster;
        this.moduleFilename = moduleFilename;
    }

    private byte[] getAssertionModuleResourceCached(String path) throws RemoteException, ClusterStatusAdmin.ModuleNotFoundException {
        byte[] got = cache.get(path);
        if (got != null)
            return got;

        got = cluster.getAssertionModuleResource(moduleFilename, path);
        if (got != null) cache.put(path, got);
        return got;
    }

    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        String resourcepath = name.replace('.', '/').concat(".class");
        try {
            final byte[] bytes = getAssertionModuleResourceCached(resourcepath);
            if (bytes == null)
                throw new ClassNotFoundException("Class not found in module " + moduleFilename + ": " + name);

            return AccessController.doPrivileged(new PrivilegedAction<Class>() {
                public Class run() {
                    return defineClass(name, bytes, 0, bytes.length);
                }
            });

        } catch (ClusterStatusAdmin.ModuleNotFoundException e) {
            throw new ClassNotFoundException("Unable to load class from module " + moduleFilename + ": " + ExceptionUtils.getMessage(e), e);
        } catch (RemoteException e) {
            throw new ClassNotFoundException("Unable to load class from module " + moduleFilename + ": " + ExceptionUtils.getMessage(e), e);
        }
    }

    public InputStream getResourceAsStream(String name) {
        InputStream sup = super.getResourceAsStream(name);
        if (sup != null)
            return sup;
        byte[] got = getResourceBytes(name);
        return got == null ? null : new ByteArrayInputStream(got);
    }

    private byte[] getResourceBytes(String name) {
        try {
            return getAssertionModuleResourceCached(name);
        } catch (ClusterStatusAdmin.ModuleNotFoundException e) {
            ConsoleAssertionRegistry.logger.log(Level.WARNING, "Unable to find resource from module: " + ExceptionUtils.getMessage(e), e);
            return null;
        } catch (RemoteException e) {
            ConsoleAssertionRegistry.logger.log(Level.WARNING, "Unable to find resource from module: " + ExceptionUtils.getMessage(e), e);
            return null;
        }
    }

    protected URL findResource(final String name) {
        URL sup = super.findResource(name);
        if (sup != null)
            return sup;
        final byte[] gotBytes = getResourceBytes(name);
        if (gotBytes == null)
            return null;

        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<URL>() {
                public URL run() throws Exception {
                    return new URL("file", null, -1, "!!" + toString() + "!" + name, new URLStreamHandler() {
                        protected URLConnection openConnection(URL u) throws IOException {
                            return new URLConnection(u) {
                                public void connect() throws IOException { }
                                public InputStream getInputStream() throws IOException {
                                    return new ByteArrayInputStream(gotBytes);
                                }
                            };
                        }
                    });
                }
            });
        } catch (PrivilegedActionException e) {
            // Probably running applet in untrusted mode, and disallowed from specifying URLStreamHandler
            ConsoleAssertionRegistry.logger.log(Level.WARNING, "*** findResource unable to create URL with URLStreamHandler for module class loader: resource=" + name, e);
        }
        return null;
    }

    protected Enumeration<URL> findResources(String name) throws IOException {
        ConsoleAssertionRegistry.logger.log(Level.WARNING, "*** findResources called on module class loader: resource=" + name);
        return super.findResources(name);
    }
}
