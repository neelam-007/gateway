package com.l7tech.server.policy.module;

import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.IteratorEnumeration;
import com.l7tech.util.ResourceUtils;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A URLClassloader that keeps track of loaded classes so they can be notified when it is time to unload them.
 * @noinspection CustomClassloader
 */
class ModularAssertionClassLoader extends URLClassLoader implements Closeable {
    protected static final Logger logger = Logger.getLogger(ModularAssertionClassLoader.class.getName());

    /** Protocol that we register. */
    public static final String NR_PROTO = "assnmod";

    static ServerAssertionRegistry registry = null;

    /** Classes that have been loaded from this module. */
    private final Set<Class> classes = Collections.synchronizedSet(new HashSet<Class>());
    private final String moduleName;
    private final Map<NestedZipFile, Object> nestedJarFiles;
    private final Set<ClassLoader> delegates = Collections.synchronizedSet(new LinkedHashSet<ClassLoader>());
    private final String resourceLoadPrefix;
    private final CodeSource codeSource;
    private final AccessControlContext accessControlContext;

    ModularAssertionClassLoader(String moduleName, URL jarUrl, ClassLoader parent, Set<NestedZipFile> nestedJarFiles, boolean useApplicationClasspath) {
        super(new URL[] { jarUrl }, parent);
        this.moduleName = moduleName;
        this.nestedJarFiles = new ConcurrentHashMap<>();
        for (NestedZipFile file : nestedJarFiles)
            this.nestedJarFiles.put(file, new Object());
        this.resourceLoadPrefix = useApplicationClasspath ? "com.l7tech.external.assertions" : null;
        this.codeSource = new CodeSource(jarUrl, (java.security.cert.Certificate[]) null);
        this.accessControlContext = AccessController.getContext();
    }

    /**
     * Add a ClassLoader to the list of extra, last-second delegates to ask if a needed class or resource
     * can't be found while loading classes or resources from this module.
     * <p/>
     * These will be invoked in order until one of them produces the sought-after class or resource.
     *
     * @param classLoader a ClassLoader to invoke if a class or resource couldn't be found directly in the .aar, a nested
     *        jar file, or a parent classloader
     */
    public void addDelegate(ClassLoader classLoader) {
        if (classLoader == null) throw new IllegalArgumentException("classLoader argument must be non-null");
        delegates.add(classLoader);
    }

    /**
     * Revove a ClassLoader from the list of extra delegates.
     *
     * @param classLoader the ClassLoader to remove from the list.
     * @return true if the specified classloader was found and removed
     */
    public boolean removeDelegate(ClassLoader classLoader) {
        return delegates.remove(classLoader);
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class c = findLoadedClass(name);

        if ( c == null ) {
            if ( shouldLoadFromParentResources( name ) ) {
                c = findClass( name );
            } else {
                c = super.loadClass( name, resolve );
            }
        }

        if ( resolve ) {
            resolveClass(c);
        }

        return c;
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Class>() {
                @Override
                public Class run() throws Exception {
                    Class<?> found = null;
                    try {
                        if ( shouldLoadFromParentResources( name ) )
                            found = findClassFromParentResource( name );
                        else if (name.startsWith("com.l7tech.external.") || (!name.startsWith("com.l7tech.") && !name.startsWith("java.")))
                            found = findClassFromNestedJars(name, false);
                        if (found == null)
                            found = ModularAssertionClassLoader.super.findClass(name);
                    } catch (ClassNotFoundException e) {
                        found = findClassFromNestedJars(name, false);
                        if (found == null)
                            found = findClassFromDelegates(name);
                        if (found == null)
                            throw new ClassNotFoundException(ExceptionUtils.getMessage(e), e);
                    }
                    classes.add(found);
                    return found;
                }
            }, accessControlContext);
        } catch (PrivilegedActionException pae) {
            throw new ClassNotFoundException(ExceptionUtils.getMessage(pae), pae);
        }
    }

    private boolean shouldLoadFromParentResources( final String name ) {
        return resourceLoadPrefix != null && name.startsWith( resourceLoadPrefix + "." );
    }

    private Class findClassFromParentResource( final String name ) {
        final String path = toResourcePath( name );
        return define( name, super.getResourceAsStream(path) );
    }

    private Class findClassFromNestedJars(String name, boolean hidePrivate) {
        final String path = toResourcePath( name );
        return define(name, getResourceBytesFromNestedJars(path, hidePrivate));
    }

    private String toResourcePath( final String name ) {
        return name.replace('.', '/').concat(".class");
    }

    private String toClassPath( final String path ) {
        return path.replace( '/', '.' );
    }

    private Class define( final String name, final InputStream bytecode ) {
        if ( bytecode != null ) {
            try {
                return define( name, IOUtils.slurpStream( bytecode ) );
            } catch ( IOException e ) {
                logger.log(Level.SEVERE, "Unable to read resource for class " + name + " in module " +
                                         moduleName + ": " + ExceptionUtils.getMessage(e), e);
            }
        }

        return null;
    }

    private Class define( final String name, final byte[] bytecode ) {
        if (bytecode == null)
            return null;

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

        return defineClass(name, bytecode, 0, bytecode.length, codeSource);
    }

    private Class findClassFromDelegates(String name) {
        for (ClassLoader loader : delegates) {
            try {
                return loader.loadClass(name);
            } catch (ClassNotFoundException e) {
                /* FALLTHROUGH and try next delegate */
            }
        }
        return null;
    }

    /**
     * Get the bytes for the specified resource from this assertion module, without looking in any parent
     * class loaders (unless the resource is under resourceLoadPrefix)
     *
     * @param path  the path, ie "com/l7tech/console/panels/resources/RateLimitAssertionPropertiesDialog.form".  Required.
     * @param hidePrivateLibraries  true if the resource bytes will be sent back to a remote client, and so any classes from nested jarfiles
     *                              matching patterns listed in the .AAR manifest's "Private-Libraries:" header should not be loadable.
     *                              <p/>
     *                              false if all resource bytes should be loadable, even those from private nested jarfiles.
     * @return the requested resource bytes, or null if the resource was not found.
     * @throws IOException if there is an error reading the resource
     */
    byte[] getResourceBytes(final String path, boolean hidePrivateLibraries) throws IOException {
        URL url = shouldLoadFromParentResources( toClassPath(path) ) ?
                super.getResource(path):
                super.findResource(path);
        if (url == null) {
            byte[] found = getResourceBytesFromNestedJars(path, hidePrivateLibraries);
            if (found == null)
                found = getResourceBytesFromDelegates(path);
            return found;
        }
        InputStream is = null;
        try {
            is = url.openStream();
            return IOUtils.slurpStream(is);
        } finally {
            ResourceUtils.closeQuietly(is);
        }
    }

    private byte[] getResourceBytesFromNestedJars(String path, boolean hidePrivateLibraries) {
        for (NestedZipFile nested : nestedJarFiles.keySet()) {
            if (!hidePrivateLibraries || !nested.isPrivateLibrary()) {
                try {
                    byte[] bytes = nested.getFile(path);
                    if (bytes != null)
                        return bytes;
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Unable to read resource from nested jar file " + nested.getEntryName() + " in module " +
                                             moduleName + ": " + ExceptionUtils.getMessage(e), e);
                }
            }
        }
        return null;
    }

    private byte[] getResourceBytesFromDelegates(String path) {
        for (ClassLoader loader : delegates) {
            InputStream foundStream = loader.getResourceAsStream(path);
            if (foundStream != null) {
                try {
                    return IOUtils.slurpStream(foundStream);
                } catch (IOException e) {
                    logger.log(Level.WARNING,
                               "Error reading resource " + path +
                               " from delegate ClassLoader for module name " + moduleName + ": " + ExceptionUtils.getMessage(e),
                               e);
                } finally {
                    ResourceUtils.closeQuietly(foundStream);
                }
            }
        }
        return null;
    }

    @Override
    public URL findResource(final String name) {
        return AccessController.doPrivileged(new PrivilegedAction<URL>() {
            @Override
            public URL run() {
                URL url = ModularAssertionClassLoader.super.findResource(name);
                if (url == null)
                    url = findResourceFromNestedJars(name);
                if (url == null)
                    url = findResourceFromDelegates(name);
                return url;
            }
        }, accessControlContext);
   }

    private URL findResourceFromNestedJars(String name) {
        for (NestedZipFile nested : nestedJarFiles.keySet()) {
            try {
                final byte[] bytes = nested.getFile(name);
                if (bytes != null)
                    return makeResourceUrl(nested, name, bytes);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Unable to read resource from nested jar file " + nested.getEntryName() + " in module " +
                                         moduleName + ": " + ExceptionUtils.getMessage(e), e);
                return null;
            }
        }
        return null;
    }

    private URL findResourceFromDelegates(String name) {
        for (ClassLoader loader : delegates) {
            URL found = loader.getResource(name);
            if (found != null)
                return found;
        }
        return null;
    }

    private URL makeResourceUrl(NestedZipFile nested, String name, final byte[] bytes) {
        try {
            return new URL(NR_PROTO, null, -1, moduleName + '!' + nested.getEntryName() + '!' + name, new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL url) throws IOException {
                    return new URLConnection(url) {
                        @Override
                        public void connect() throws IOException { }
                        @Override
                        public InputStream getInputStream() throws IOException {
                            return new ByteArrayInputStream(bytes);
                        }
                    };
                }
            });
        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, "Unable to create resource URL: " + ExceptionUtils.getMessage(e), e);
            return null;
        }
    }

    @Override
    public Enumeration<URL> findResources(final String name) throws IOException {
        List<URL> urls = new ArrayList<>();
        Enumeration<URL> resen = super.findResources(name);
        while (resen != null && resen.hasMoreElements()) {
            URL url = resen.nextElement();
            urls.add(url);
        }
        URL r = findResourceFromNestedJars(name);
        if (r != null)
            urls.add(r);
        r = findResourceFromDelegates(name);
        if (r != null)
            urls.add(r); // TODO should add all of them, not just one of them
        return new IteratorEnumeration<>(urls.iterator());
    }

    /**
     * Notify any interested classes that their module is being unloaded and they should dismantle any
     * datastructures that would keep their instances from being collected
     */
    void onModuleUnloaded() {
        Set<Class> toNotify = new HashSet<>(classes);
        for (Class clazz : toNotify) {
            onModuleUnloaded(clazz);
            classes.remove(clazz);
        }
    }

    /**
     * Notify the specified class that its module is being unloaded and it should dismantle any datastructures
     * that would keep its instances from being collected.  Classes are assumed to be interested in such notfication
     * only if they have a public static method "onModuleUnloaded" that takes no arguments and returns void.
     * <p/>
     * Otherwise, they need to register as an application listener and watch for an AssertionModuleUnregisteredEvent
     * that pertains to them.
     *
     * @param clazz the class to notify.  If this is null, or does not include a public onModuleUnloaded static
     *        method, this method takes no action.
     */
    protected void onModuleUnloaded(Class clazz) {
        try {
            clazz.getMethod("onModuleUnloaded").invoke(null);
        } catch (NoClassDefFoundError | IllegalAccessException e) {
            // Must be not-entirely-initialized class
            logger.log(Level.WARNING, "Module " + moduleName + ": unable to notify class " + clazz.getName() + " of module unload: " + ExceptionUtils.getMessage(e));
        } catch (NoSuchMethodException e) {
            // Ok, it doesn't care to be notified
        } catch (InvocationTargetException e) {
            logger.log(Level.SEVERE, "Module " + moduleName + ": exception while notifying class " + clazz.getName() + " of module unload: " + ExceptionUtils.getMessage(e), e);
        }
    }

    /** @return registry we will use for locating modules when resolving assnmod: URLs. */
    public static ServerAssertionRegistry getRegistry() {
        return registry;
    }

    /** @param registry registry to use for locating modules when resolving assnmod: URLs. */
    public static void setRegistry(ServerAssertionRegistry registry) {
        ModularAssertionClassLoader.registry = registry;
    }

    @Override
    public void close() throws IOException {
        onModuleUnloaded();
        Set<NestedZipFile> toClose = new HashSet<>(nestedJarFiles.keySet());
        for (NestedZipFile nested : toClose) {
            nested.close();
            nestedJarFiles.remove(nested);
        }
    }
}
