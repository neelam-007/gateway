package com.l7tech.assertion.base.util.classloaders;

import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.Permission;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.HashMap;

/**
 * A custom classloader; which implements all the methods that should be required for any
 * classloader that needs to be written, there is also an abstract initialize method which
 * has to be implemented which will determine which classes to load during setup of the classloader.
 */
abstract class CustomClassLoader extends ClassLoader {

    /**
     * mkwan TODO: There is also a requirement to allow using different version of class than what is loaded in the
     *             parent classLoader but is not supported.
     */

    private ProtectionDomain pd;
    protected HashMap<String, byte[]> availableClassBytes = new HashMap<String, byte[]>();
    protected HashMap<String, byte[]> availableResourceBytes = new HashMap<String, byte[]>();

    /**
     * mkwan TODO: It is strange that this CustomClassLoader keep tracks of the classes loaded.
     *             Since this Classloader used the super.definedClass(), these should be unnecessary.
     *             In addition, some methods like findLoadedClass() is not overridden to look for class in these collections (to fix)
     */
    protected HashMap<String, Class> loadedClasses = new HashMap<String, Class>();
    protected HashMap<String, Package> loadedPackages = new HashMap<String, Package>();

    /**
     * The default constructor which will set up the security manager, and permissions for the class loader. After
     * these have to been set up then it is required to call the initialization method which should be implemented
     * by subclasses.
     *
     * @param parent the parent classloader.
     */
    public CustomClassLoader(ClassLoader parent) {
        super(parent);

        pd = new ProtectionDomain(CustomClassLoader.class.getProtectionDomain().getCodeSource(), getPermissions());
    }

    /**
     * Get the permissions to be granted to classes loaded via the classloader
     * @return Permissions
     */
    protected Permissions getPermissions() {
        Permissions toSetPermissions = new Permissions();

        Enumeration<Permission> permissions = CustomClassLoader.class.getProtectionDomain().getPermissions().elements();
        while(permissions.hasMoreElements()){
            Permission tempPermission = permissions.nextElement();
            toSetPermissions.add(tempPermission);
        }

        return toSetPermissions;
    }

    /**
     * Abstract method which will be implemented by subclasses; this method will determine
     * where to load the classes from.
     */
    public abstract void initialize();

    /**
     * Check the available classes that have been loaded by this custom class loader, if not found then
     * default back to the original classloader load class methods.
     *
     * @param className the class name
     * @return the loaded class
     *
     * @throws ClassNotFoundException exception if class not found
     */
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        if(!availableClassBytes.containsKey(className)) {
            return super.loadClass(className);
        }

        byte[] b = availableClassBytes.get(className);
        Class<?> obj = this.customDefineClass(className, b, 0, b.length);

        return obj;
    }

    /**
     * Check if the class has already been custom loaded before we define a custom class. If the class
     * has not been loaded, then we need to use the default methodology to define the class and resolve it
     * then add it to the loaded classes and return the class.
     *
     * @param name the class name
     * @param b the bytes
     * @param off the offset
     * @param len the length
     * @return the class
     *
     * @throws ClassFormatError exception on class format
     */
    private Class<?> customDefineClass(String name, byte[] b, int off, int len) throws ClassFormatError {
        Class cc = loadedClasses.get(name);
        if(cc != null) {
            return cc;
        }

        Class c = super.defineClass(name, b, off, len, pd);
        super.resolveClass(c);
        loadedClasses.put(name, c);
        return c;
    }

    /**
     * The method will find class on the loaded classes first, if not found then it will look at the available
     * class bytes and if it does not find the class there, we will look for the class in the parent classloader
     * or define it custom. If the class is found in the available class bytes then we will define the class
     * using these bytes, resolve the class and return it.
     *
     * @param name the class name
     * @return the found class
     *
     * @throws ClassNotFoundException exception not found
     */
    public Class<?> findClass(String name) throws ClassNotFoundException {
        /**
         * mkwan TODO: For unknown reason, this method is changed to public (which it shouldn't).  Classes should be
         *             loaded via loadClass() instead (@see java.lang.ClassLoader#findClass API document)
         */
        if(loadedClasses.containsKey(name)) {
            return loadedClasses.get(name);
        }

        if(!availableClassBytes.containsKey(name)) {
            try {
                return Class.forName(name, false, getParent());
            } catch(ClassNotFoundException e) {
                InputStream is = getParent().getResourceAsStream(name.replace('.', '/') + ".class");
                if(is != null) {
                    ByteArrayOutputStream baos = null;

                    try {
                        baos = new ByteArrayOutputStream();
                        IOUtils.copyStream(is, baos);
                        byte[] bytes = baos.toByteArray();
                        Class clazz = customDefineClass(name, bytes, 0, bytes.length);
                        return clazz;
                    } catch(IOException ex) {
                        throw new ClassNotFoundException(name);
                    } finally {
                        ResourceUtils.closeQuietly(is);
                        ResourceUtils.closeQuietly(baos);
                    }
                }
            }
        }

        try {
            Class clazz = customDefineClass(name, availableClassBytes.get(name), 0, availableClassBytes.get(name).length);
            return clazz;
        } catch(NullPointerException e) {
            throw new ClassNotFoundException(name + " class not found");
        }

    }

    /**
     *
     * The resource as stream method will check the available resource bytes or available class bytes and try to retrieve
     * the correct resource, otherwise it will use the default classloader method to return the resource as a stream.
     *
     * @param name the resource name
     * @return the resource as a stream
     */
    public InputStream getResourceAsStream(String name) {
        if(availableResourceBytes.containsKey(name)) {
            return new ByteArrayInputStream(availableResourceBytes.get(name));
        } else if(availableClassBytes.containsKey(name.replace('/', '.').substring(0, name.length() - 6))) {
            return new ByteArrayInputStream(availableClassBytes.get(name.replace('/', '.').substring(0, name.length() - 6)));
        } else {
            return super.getResourceAsStream(name);
        }
    }

    /**
     * Retrieves the resources with the provided name. Will check the custom resources that have been loaded, and try to
     * return it; otherwise will double back to the original classloader method to get the resources.
     *
     * @param name the resources name
     * @return enumerated URL
     *
     * @throws IOException exception on IO exception
     */
    public Enumeration<URL> getResources(final String name) throws IOException {
        if(availableResourceBytes.containsKey(name)) {
            URL url = getResource(name);
            return new SingletonEnumeration<URL>(url);
        } else {
            return super.getResources(name);
        }
    }

    /**
     * Retrieves the resource with the provided name. Will check the available resource bytes to check for the resource, otherwise
     * will double back to the original classloader method to get the resources.
     *
     * @param name the resource name
     * @return the URL to the resource
     */
    public URL getResource(final String name) {
        if(availableResourceBytes.containsKey(name)) {
            try {
                return new URL(null, "custom-jar:/" + name, new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL u) throws IOException {
                        return new CustomURLConnection(availableResourceBytes.get(name));
                    }
                });
            } catch(MalformedURLException e) {
                return null;
            }
        } else {
            return super.getResource(name);
        }
    }

    /**
     * The singleton enumeration.
     */
    protected static class SingletonEnumeration<E> implements Enumeration<E> {
        private boolean started = false;
        private E element;

        /**
         * Default constructor.
         *
         * @param element the element
         */
        public SingletonEnumeration(E element) {
            this.element = element;
        }

        /** {@inheritDoc **/
        @Override
        public boolean hasMoreElements() {
            return !started && element != null;
        }

        /** {@inheritDoc} **/
        @Override
        public E nextElement() {
            if(!started && element != null) {
                started = true;
                return element;
            } else {
                return null;
            }
        }
    }

    /**
     * The custom URL connection.
     */
    protected static class CustomURLConnection extends URLConnection {
        byte[] bytes;

        /**
         * The default custom URL connection constructor.
         *
         * @param bytes the bytes
         */
        public CustomURLConnection(byte[] bytes) {
            super(null);
            this.bytes = bytes;
        }

        /** {@inheritDoc} **/
        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(bytes);
        }

        /** {@inheritDoc **/
        @Override
        public void connect() throws IOException {
            //do nothing
        }
    }

}
