package com.l7tech.console.util;

import com.l7tech.common.io.IOUtils;
import com.l7tech.common.io.NullOutputStream;
import com.l7tech.wsdl.Wsdl;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionDeserializer;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.ExtensionSerializer;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.wsdl.xml.WSDLWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Enumeration;
import java.util.PropertyPermission;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.xml.sax.InputSource;

/**
 * WSDL utility methods for the console. 
 *
 * @author Steve Jones
 */
public class WsdlUtils {

    //- PUBLIC

    /**
     * Get a WSDLFactoryBuilder that delegates to {@code getWSDLFactory}
     *
     * @return The WSDLFactoryBuilder
     * @see #getWSDLFactory
     */
    public static Wsdl.WSDLFactoryBuilder getWSDLFactoryBuilder() {
        return new Wsdl.WSDLFactoryBuilder() {
            public WSDLFactory getWSDLFactory(final boolean writeEnabled) throws WSDLException {
                try {
                    return WsdlUtils.getWSDLFactory();
                } catch (WSDLException we) {
                    if ( !writeEnabled ) {
                        // then fallback to "standard" implementation. This is useful in an untrusted environment.
                        return WSDLFactory.newInstance();
                    } else {
                        throw we;
                    }
                }
            }
        };
    }

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
     *         (see {@link WSDLFactory#newInstance()}).
     */
    @SuppressWarnings({"unchecked"})
    public static WSDLFactory getWSDLFactory() throws WSDLException {
        WSDLFactory factory;

        if(!TopComponents.getInstance().isApplet()) {
            factory = WSDLFactory.newInstance();
        }
        else {
            // See if we are trusted
            try {
                AccessController.doPrivileged(new PrivilegedAction(){
                    public Object run() {
                        AccessController.checkPermission(new PropertyPermission("file.encoding", "read"));
                        AccessController.checkPermission(new PropertyPermission("line.separator", "read"));
                        return null;
                    }
                });
            }
            catch(AccessControlException ace) {
                throw new WSDLFactoryNotTrustedException(WSDLException.OTHER_ERROR, "Insufficient permissions for factory use.");
            }

            // Load classes with permission to access required property
            Object result = AccessController.doPrivileged(new PrivilegedAction(){
                public Object run() {
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
                                    byte[] classData = IOUtils.slurpStream(resIn, 102400);
                                    return defineClass(name, classData, 0, classData.length, pd);
                                } catch(IOException ioe) {
                                    throw new ClassNotFoundException("Error loading resource for class '" + name + "'.", ioe);
                                }
                            }
                        };

                        WSDLFactory factory = new AppletSafeWSDLFactory((WSDLFactory) classLoader.loadClass("com.ibm.wsdl.factory.WSDLFactoryImpl").newInstance());

                        // Process a test WSDL to ensure that classes are loaded here in a trusted section.
                        // If this is not done then issues occur when using the factory from untrusted code
                        try {
                            Definition testDefinition = factory.newWSDLReader().readWSDL("urn:test", new InputSource(new StringReader("<wsdl:definitions xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\"/>")));
                            factory.newWSDLWriter().writeWSDL( testDefinition, new NullOutputStream() );
                        } catch (WSDLException we) {
                            logger.log(Level.WARNING, "Error processing test WSDL", we);    
                        }

                        return factory;
                    }
                    catch(MalformedURLException mue) {
                        return mue;
                    }
                    catch(ClassNotFoundException cnfe) {
                        return cnfe;
                    }
                    catch(InstantiationException ie) {
                        return ie;
                    }
                    catch(IllegalAccessException iae) {
                        return iae;            
                    }
                }
            });

            if (result instanceof Exception) {
                throw new WSDLException(WSDLException.OTHER_ERROR, "Could not load factory.", (Exception) result);
            } else {
                factory = (WSDLFactory) result;
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

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(WsdlUtils.class.getName());

    /**
     * Delegates all WSDLFactory methods except newPopulatedExtensionRegistry. This is prepoulated with safe-for-applet
     * versions of the DefaultSerializers.
     */
    private static final class AppletSafeWSDLFactory extends WSDLFactory {
        private WSDLFactory delegate;

        public AppletSafeWSDLFactory(WSDLFactory delegate) {
            this.delegate = delegate;
        }

        public Definition newDefinition() {
            return delegate.newDefinition();
        }

        public WSDLReader newWSDLReader() {
            return delegate.newWSDLReader();
        }

        public WSDLWriter newWSDLWriter() {
            return delegate.newWSDLWriter();
        }

        /**
         * Creates a new populated ExtensionRegistry and then registers a default Serializer and Deserializer that have
         * been safely loaded (for trusted and non trusted applets).
         * @return an extension registry that's safe for use in the applet.
         *
         */
        public ExtensionRegistry newPopulatedExtensionRegistry() {
            ExtensionRegistry reg = delegate.newPopulatedExtensionRegistry();

            ExtensionSerializer ser;
            ExtensionDeserializer deser;
            try {
                ser = getDefaultSerializer(reg);
                deser = getDefaultDeserializer(reg);
                reg.setDefaultSerializer(ser);
                reg.setDefaultDeserializer(deser);
            } catch (WSDLException e) {
                throw new IllegalStateException("A configuration error has occured", e);
            }

            return reg;
        }

        private ExtensionDeserializer getDefaultDeserializer(ExtensionRegistry registry) throws WSDLException {
            if (registry == null)
                throw new IllegalArgumentException("A valid ExtensionRegistry must be supplied");

            ExtensionDeserializer deserializer;

            // See if we are trusted
            try {
                AccessController.checkPermission(new PropertyPermission("file.encoding", "read"));
                AccessController.checkPermission(new PropertyPermission("line.separator", "read"));
            }
            catch(AccessControlException ace) {
                throw new WSDLFactoryNotTrustedException(WSDLException.OTHER_ERROR, "Insufficient permissions for extension serializer use.");
            }

            // Load classes with permission to access required property
            try {
                final ClassLoader resourceLoader = registry.getClass().getClassLoader();
                final CodeSource cs = new CodeSource(new URL("file:/resource/wsdl/extensiondeserializer"), (Certificate[])null);
                final Permissions permissions = new Permissions();
                permissions.add(new PropertyPermission("file.encoding", "read"));
                permissions.add(new PropertyPermission("line.separator", "read"));

                final ClassLoader classLoader = new SecureClassLoader(new FilterClassLoader(resourceLoader, "javax.wsdl.extensions.UnknownExtensionSerializer")){
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
                            byte[] classData = IOUtils.slurpStream(resIn, 102400);
                            return defineClass(name, classData, 0, classData.length, pd);
                        } catch(IOException ioe) {
                            throw new ClassNotFoundException("Error loading resource for class '" + name + "'.", ioe);
                        }
                    }
                };

                deserializer = (ExtensionDeserializer) classLoader.loadClass("javax.wsdl.extensions.UnknownExtensionDeserializer").newInstance();
            }
            catch(MalformedURLException mue) {
                throw new WSDLException(WSDLException.OTHER_ERROR, "Could not load extension serializer.", mue);
            }
            catch(ClassNotFoundException cnfe) {
                throw new WSDLException(WSDLException.OTHER_ERROR, "Could not load extension serializer.", cnfe);
            }
            catch(InstantiationException ie) {
                throw new WSDLException(WSDLException.OTHER_ERROR, "Could not get extension serializer.", ie);
            }
            catch(IllegalAccessException iae) {
                throw new WSDLException(WSDLException.OTHER_ERROR, "Could not get extension serializer.", iae);
            }

            return deserializer;
        }

        private ExtensionSerializer getDefaultSerializer(ExtensionRegistry registry) throws WSDLException {
            if (registry == null)
                throw new IllegalArgumentException("A valid ExtensionRegistry must be supplied");

            ExtensionSerializer serializer;

            // See if we are trusted
            try {
                AccessController.checkPermission(new PropertyPermission("file.encoding", "read"));
                AccessController.checkPermission(new PropertyPermission("line.separator", "read"));
            }
            catch(AccessControlException ace) {
                throw new WSDLFactoryNotTrustedException(WSDLException.OTHER_ERROR, "Insufficient permissions for extension serializer use.");
            }

            // Load classes with permission to access required property
            try {
                final ClassLoader resourceLoader = registry.getClass().getClassLoader();
                final CodeSource cs = new CodeSource(new URL("file:/resource/wsdl/extensionserializer"), (Certificate[])null);
                final Permissions permissions = new Permissions();
                permissions.add(new PropertyPermission("file.encoding", "read"));
                permissions.add(new PropertyPermission("line.separator", "read"));

                final ClassLoader classLoader = new SecureClassLoader(new FilterClassLoader(resourceLoader, "javax.wsdl.extensions.UnknownExtensionSerializer")){
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
                            byte[] classData = IOUtils.slurpStream(resIn, 102400);
                            return defineClass(name, classData, 0, classData.length, pd);
                        } catch(IOException ioe) {
                            throw new ClassNotFoundException("Error loading resource for class '" + name + "'.", ioe);
                        }
                    }
                };

                serializer = (ExtensionSerializer) classLoader.loadClass("javax.wsdl.extensions.UnknownExtensionSerializer").newInstance();
            }
            catch(MalformedURLException mue) {
                throw new WSDLException(WSDLException.OTHER_ERROR, "Could not load extension serializer.", mue);
            }
            catch(ClassNotFoundException cnfe) {
                throw new WSDLException(WSDLException.OTHER_ERROR, "Could not load extension serializer.", cnfe);
            }
            catch(InstantiationException ie) {
                throw new WSDLException(WSDLException.OTHER_ERROR, "Could not get extension serializer.", ie);
            }
            catch(IllegalAccessException iae) {
                throw new WSDLException(WSDLException.OTHER_ERROR, "Could not get extension serializer.", iae);
            }

            return serializer;
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
            return Collections.enumeration(Collections.<URL>emptyList());
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
