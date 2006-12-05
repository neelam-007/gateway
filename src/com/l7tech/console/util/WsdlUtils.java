package com.l7tech.console.util;

import java.security.CodeSource;
import java.security.Permissions;
import java.security.SecureClassLoader;
import java.security.ProtectionDomain;
import java.security.Permission;
import java.security.AccessController;
import java.security.AccessControlException;
import java.security.cert.Certificate;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.IOException;
import java.util.PropertyPermission;
import java.util.Enumeration;
import java.util.Collections;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.WSDLException;

import com.l7tech.common.util.HexUtils;

/**
 * WSDL utility methods for the console. 
 *
 * @author Steve Jones
 */
public class WsdlUtils {

    /**
     * Get a new WSDLFactory.
     *
     * <p>This will create a WSDL factory that can be used from a (trusted)
     * applet or from an application.</p>
     *
     * @return The WSDLFactory.
     * @throws WSDLFactoryNotTrustedException if the factory cannot be created
     *         because of access control restrictions.
     * @throws WSDLException if a WSDLFactory cannot be created
     *         (see {@link WSDLFactory#newInstance}).
     */
    public static WSDLFactory getWSDLFactory() throws WSDLException {
        WSDLFactory factory;

        if(!TopComponents.getInstance().isApplet()) {
            factory = WSDLFactory.newInstance();
        }
        else {
            // See if we are trusted
            try {
                AccessController.checkPermission(new PropertyPermission("file.encoding", "read"));
                AccessController.checkPermission(new PropertyPermission("line.separator", "read"));
            }
            catch(AccessControlException ace) {
                throw new WSDLFactoryNotTrustedException(WSDLException.OTHER_ERROR, "Insufficient permissions for factory use.");
            }

            // Load classes with permission to access required property
            try {
                final ClassLoader resourceLoader = WSDLFactory.class.getClassLoader();
                final CodeSource cs = new CodeSource(new URL("file:/resource/wsdl"), (Certificate[])null);
                final Permissions permissions = new Permissions();
                permissions.add(new PropertyPermission("file.encoding", "read"));
                permissions.add(new PropertyPermission("line.separator", "read"));

                final ClassLoader classLoader = new SecureClassLoader(new FilterClassLoader(resourceLoader, "com.ibm.wsdl")){
                    private ProtectionDomain pd;
                    public Class<?> findClass(final String name) throws ClassNotFoundException {
                        try {
                            String resName = name.replace(".", "/").concat(".class");
                            URL resUrl = resourceLoader.getResource(resName);
                            if (resUrl == null)
                                throw new ClassNotFoundException("Resource not found for class '" + name + "'.");

                            if (pd == null) {
                                // lazily resolve to add in permission for URL
                                Permission permission = resUrl.openConnection().getPermission();
                                if (permission != null)
                                    permissions.add(permission);
                                pd = new ProtectionDomain(cs, permissions);
                            }
                            InputStream resIn = resUrl.openStream();
                            byte[] classData = HexUtils.slurpStream(resIn, 102400);
                            Class clazz = defineClass(name, classData, 0, classData.length, pd);
                            return clazz;
                        } catch(IOException ioe) {
                            throw new ClassNotFoundException("Error loading resource for class '" + name + "'.", ioe);
                        }
                    }
                };

                factory = (WSDLFactory) classLoader.loadClass("com.ibm.wsdl.factory.WSDLFactoryImpl").newInstance();
            }
            catch(MalformedURLException mue) {
                throw new WSDLException(WSDLException.OTHER_ERROR, "Could not load factory.", mue);
            }
            catch(ClassNotFoundException cnfe) {
                throw new WSDLException(WSDLException.OTHER_ERROR, "Could not load factory.", cnfe);            
            }
            catch(InstantiationException ie) {
                throw new WSDLException(WSDLException.OTHER_ERROR, "Could not create factory.", ie);
            }
            catch(IllegalAccessException iae) {
                throw new WSDLException(WSDLException.OTHER_ERROR, "Could not create factory.", iae);            
            }
        }

        return factory;
    }

    /**
     * WSDLException for access control issues.
     */
    public static final class WSDLFactoryNotTrustedException extends WSDLException {
        public WSDLFactoryNotTrustedException(String faultCode, String msg) {
            super(faultCode, msg);
        }

        public WSDLFactoryNotTrustedException(String faultCode, String msg, Throwable t) {
            super(faultCode, msg, t);
        }
    }

    /**
     * Filtering class loader that will delegate to its parent for all but one
     * package prefix.
     */
    private static class FilterClassLoader extends ClassLoader {

        /**
         * Create a filter classloader with the given parent and filter.
         *
         * <p>The parent is not delegated to in the usual manner. Only requests to
         * load classes / resources that are not "under" the filter prefix are delegated
         * to the parent.</p>
         *
         * @param parent the classloader to delegate to for matching classes/resources
         * @param filter the package/resource prefix for non delegated classes/resources
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
            return resourcePrefix != null && !resourcePath.startsWith(resourcePrefix);
        }
    }
}
