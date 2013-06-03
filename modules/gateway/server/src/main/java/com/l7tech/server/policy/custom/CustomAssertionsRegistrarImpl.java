package com.l7tech.server.policy.custom;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.gateway.common.custom.CustomAssertionDescriptor;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.cei.CustomExtensionInterfaceBinding;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.CustomAssertionUI;
import com.l7tech.policy.wsp.ClassLoaderUtil;
import com.l7tech.server.admin.ExtensionInterfaceManager;
import com.l7tech.server.policy.AssertionModule;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.util.ModuleClassLoader;
import com.l7tech.util.Config;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The server side CustomAssertionsRegistrar implementation.
 *
 * @author emil
 * @version 16-Feb-2004
 */
public class CustomAssertionsRegistrarImpl
  extends ApplicationObjectSupport implements CustomAssertionsRegistrar, InitializingBean {

    //- PUBLIC

    public CustomAssertionsRegistrarImpl(ServerAssertionRegistry assertionRegistry, ExtensionInterfaceManager extensionInterfaceManager) {
        if (assertionRegistry == null || extensionInterfaceManager == null) throw new IllegalArgumentException("assertionRegistry and extensionInterfaceManager are required");
        this.assertionRegistry = assertionRegistry;
        this.extensionInterfaceManager = extensionInterfaceManager;
    }

    @Override
    public byte[] getAssertionClass(String className) {
        // ensure a name such as "com.something.MyClass"
        if (className == null || className.lastIndexOf('.') <= className.indexOf('.'))
            return null;

        String classAsResource = className.replace('.','/') + ".class";
        return getAssertionResourceBytes(classAsResource);
    }

    @Override
    public byte[] getAssertionResourceBytes(String path) {
        if (logger.isLoggable(Level.FINEST))
            logger.finest("Serving custom assertion resource: " + path);

        if (customAssertionClassloader == null)
            return null;

        byte[] data = getCustomAssertionResourceBytes( path );
        if ( data != null ) {
            return data;
        }

        // Check for modular assertion resource
        Set<AssertionModule> mods = assertionRegistry.getLoadedModules();
        for (AssertionModule mod : mods) {
            try {
                byte[] resourceBytes = mod.getResourceBytes(path, true);
                if (resourceBytes != null)
                    return resourceBytes;
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error loading resource from module " + mod.getName() + ": " + path + ": " + ExceptionUtils.getMessage(e), e);
            }

        }

        return null;
    }

    @Override
    public AssertionResourceData getAssertionResourceData( final String resourcePath ) {
        if (logger.isLoggable(Level.FINEST))
            logger.finest("Serving custom assertion resource data: " + resourcePath);

        if (customAssertionClassloader == null)
            return null;

        byte[] data = getCustomAssertionResourceBytes( resourcePath );
        if ( data != null ) {
            return new AssertionResourceData(resourcePath, false, data);
        }

        return zip( resourcePath );
    }

    private byte[] getCustomAssertionResourceBytes( final String path ) {
        InputStream classIn = null;
        try {
            if (isCustomAssertionResource(path)) {
                classIn = customAssertionClassloader.getResourceAsStream(path);
                if (classIn != null)
                    return IOUtils.slurpStream(classIn);
            }
        }
        catch(IOException ioe) {
            logger.log(Level.WARNING, "Error loading custom assertion resource: " + path + ": " + ExceptionUtils.getMessage(ioe), ioe);
        }
        finally {
            ResourceUtils.closeQuietly(classIn);
        }

        return null;
    }

    private AssertionResourceData zip( final String resourcePath ) {
        AssertionResourceData data = null;

        final long startTime = System.currentTimeMillis();

        Set<AssertionModule> modules = assertionRegistry.getLoadedModules();
        if ( modules != null ) {

            PoolByteArrayOutputStream baos = null;
            try {
                baos= new PoolByteArrayOutputStream(64*1024);
                ZipOutputStream zipOut = null;
                try {
                    for ( AssertionModule module : modules ) {
                        byte[] resourceData = module.getResourceBytes( resourcePath, true );
                        if ( resourceData != null ) {
                            String[] paths = module.getClientResources();
                            if ( paths != null ) {
                                for ( String path : paths ) {
                                    if ( samePackage( resourcePath, path ) ) {
                                        byte[] resourceBytes = module.getResourceBytes( path, true );
                                        if ( resourceBytes == null ) {
                                            logger.warning( "Missing resource '"+path+"'." );
                                            continue;
                                        }
                                        if ( zipOut == null ) { // zip must have an entry, so creation is lazy
                                            zipOut = new ZipOutputStream( new NonCloseableOutputStream(baos) );

                                        }
                                        zipOut.putNextEntry( new ZipEntry( path ) );
                                        zipOut.write( resourceBytes );
                                        zipOut.closeEntry();
                                    }
                                }
                            }

                            if ( zipOut == null ) {
                                // Resource not indexed, so return single item
                                data = new AssertionResourceData( resourcePath, false, resourceData);
                            }
                            break;
                        }
                    }
                } catch (IOException e) {
                    logger.log( Level.WARNING, "Error creating resource ZIP.", e );
                } finally {
                    ResourceUtils.closeQuietly( zipOut );
                }

                if ( zipOut != null ) {
                    data = new AssertionResourceData( getPackagePath(resourcePath), true, baos.toByteArray());
                }
            } finally {
                ResourceUtils.closeQuietly( baos );
            }
        }

        if ( data != null && logger.isLoggable( Level.FINE )) {
            logger.log( Level.FINE, "Creating " + data.getData().length + " byte assertion zip took " +(System.currentTimeMillis()-startTime)+ "ms." );
        }
        
        return data;
    }

    private boolean samePackage( final String path1, final String path2 ) {
        return getPackagePath( path1 ).equals( getPackagePath( path2 ) );
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

    /**
     * @return the list of all assertions known to the runtime
     */
    @Override
    public Collection getAssertions() {
        Set customAssertionDescriptors = CustomAssertions.getDescriptors();
        return asCustomAssertionHolders(customAssertionDescriptors);
    }

    /**
     * @param c the category to query for
     * @return the list of all assertions known to the runtime
     *         for a give n category
     */
    @Override
    public Collection getAssertions( Category c) {
        final Set customAssertionDescriptors = CustomAssertions.getDescriptors(c);
        return asCustomAssertionHolders(customAssertionDescriptors);
    }

    /**
     * Return the <code>CustomAssertionDescriptor</code> for a given assertion or
     * <b>null<b>
     * Note that this method may not be invoked from management console. Server
     * classes may not deserialize into the ssm envirnoment.
     *
     * @param a the assertion class
     * @return the custom assertion descriptor class or <b>null</b>
     */
    @Override
    public CustomAssertionDescriptor getDescriptor(Class a) {
        return CustomAssertions.getDescriptor(a);
    }

    /**
     * Return the <code>CustomAssertionUI</code> class for a given assertion or
     * <b>null<b>
     *
     * @param assertionClassName the assertion class name
     * @return the custom assertion UI class or <b>null</b>
     */
    @Override
    public CustomAssertionUI getUI(String assertionClassName) {
        return CustomAssertions.getUI(assertionClassName);
    }

    /**
     *
     */
    public void setServerConfig(Config config ) {
        this.config = config;
    }

    /**
     * Perform bean initialization after properties are set.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if ( config == null) {
            throw new IllegalArgumentException("Server Config is required");
        }

        init();
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(CustomAssertionsRegistrar.class.getName());
    private static final String KEY_CONFIG_FILE = "custom.assertions.file";
    private static final String KEY_CUSTOM_MODULES = "custom.assertions.modules";
    private static final String KEY_CUSTOM_MODULES_TEMP = "custom.assertions.temp";

    private boolean initialized = false;
    private Config config;
    private ClassLoader customAssertionClassloader;
    private final ServerAssertionRegistry assertionRegistry;
    private final ExtensionInterfaceManager extensionInterfaceManager;

    private Collection asCustomAssertionHolders(final Set customAssertionDescriptors) {
        Collection result = new ArrayList();
        Iterator it = customAssertionDescriptors.iterator();
        while (it.hasNext()) {
            try {
                CustomAssertionDescriptor cd = (CustomAssertionDescriptor)it.next();
                Class ca = cd.getAssertion();
                CustomAssertionHolder ch = new CustomAssertionHolder();
                final CustomAssertion cas = (CustomAssertion)ca.newInstance();
                ch.setCustomAssertion(cas);
                ch.setCategory(cd.getCategory());
                ch.setDescriptionText(cd.getDescription());
                result.add(ch);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unable to instantiate custom assertion", e);
            }
        }
        return result;
    }

    /**
     * Custom assertion loading stuff
     */
    private void init() {
        if (initialized) return;

        String fileName = config.getProperty( KEY_CONFIG_FILE );
        if (fileName == null) {
            logger.config("'" + KEY_CONFIG_FILE + "' not specified");
            return;
        }

        String moduleDirectory = config.getProperty( KEY_CUSTOM_MODULES );
        String moduleWorkDirectory = config.getProperty( KEY_CUSTOM_MODULES_TEMP );
        ClassLoader caClassLoader = getClass().getClassLoader();
        if (moduleDirectory == null) {
            logger.config("'" + KEY_CUSTOM_MODULES + "' not specified");
        }
        else {
            caClassLoader = new ModuleClassLoader(
                ModuleClassLoader.class.getClassLoader(),
                "customassertion",
                fileName,
                new File(moduleDirectory),
                new File(moduleWorkDirectory),
                caClassLoader);

            ClassLoaderUtil.setClassloader(caClassLoader);
        }

        customAssertionClassloader = caClassLoader;

        try {
            // Support multiple config files, so there can be one per CA Jar
            Enumeration propfileUrls = caClassLoader.getResources(fileName);
            if (propfileUrls.hasMoreElements()) {
                while(propfileUrls.hasMoreElements()) {
                    URL resourceUrl = (URL) propfileUrls.nextElement();
                    if (initFromUrl(resourceUrl, caClassLoader))
                        initialized = true;
                }
            }
            else {
                logger.info("No custom assertions found.");
            }
        }
        catch(IOException ioe) {
            logger.log(Level.WARNING, "I/O error locating config file '" + fileName + "'", ioe);
        }
    }

    private boolean initFromUrl(URL customAssertionConfigUrl, ClassLoader classLoader) {
        boolean loaded = false;
        InputStream in = null;
        try {
            in = customAssertionConfigUrl.openStream();
            loadCustomAssertions(in, classLoader);
            loaded = true;
        }
        catch (FileNotFoundException e) {
            logger.info("Custom assertions config file '" + customAssertionConfigUrl + "'not present\n Custom assertions not loaded");
        }
        catch (IOException e) {
            logger.log(Level.WARNING, "I/O error reading config file '" + customAssertionConfigUrl + "'", e);
        }
        finally {
            ResourceUtils.closeQuietly(in);
        }

        return loaded;
    }

    private void loadCustomAssertions(InputStream in, ClassLoader classLoader) throws IOException {
        Properties props = new Properties();
        props.load(in);
        props.keys();

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            if (contextClassLoader != null) Thread.currentThread().setContextClassLoader(null);
            for (Iterator iterator = props.keySet().iterator(); iterator.hasNext();) {
                Object o = iterator.next();
                String key = o.toString();
                if (key.endsWith(".class")) {
                    loadSingleCustomAssertion(key.substring(0, key.indexOf(".class")), props, classLoader);
                }
            }
        }
        finally {
            if (contextClassLoader != null) Thread.currentThread().setContextClassLoader(contextClassLoader);            
        }
    }

    private void loadSingleCustomAssertion(String baseKey, Properties properties, ClassLoader classLoader) {
        String serverClass = null;
        String assertionClass = null;
        String editorClass = null;
        String securityManagerClass = null;
        String extensionInterfaceClassName = null;
        String optionalDescription = null;
        Category category = Category.UNFILLED;

        assertionClass = (String)properties.get(baseKey + ".class");

        for (Object o : properties.keySet()) {
            String key = (String) o;
            if (key.startsWith(baseKey)) {
                if (key.endsWith(".server")) {
                    serverClass = (String) properties.get(key);
                } else if (key.endsWith(".ui")) {
                    editorClass = (String) properties.get(key);
                } else if (key.endsWith(".security.manager")) {
                    securityManagerClass = (String) properties.get(key);
                } else if (key.endsWith(".extension.interface")) {
                    extensionInterfaceClassName = (String) properties.get(key);
                } else if (key.endsWith(".category")) {
                    Category c = Category.asCategory((String) properties.get(key));
                    if (c != null) {
                        category = c;
                    }
                } else if (key.endsWith(".description")) {
                    optionalDescription = (String) properties.get(key);
                }
            }
        }

        if (serverClass == null || assertionClass == null) {
            StringBuffer sb = new StringBuffer("Incomplete custom assertion, skipping\n");
            sb.append("[ assertion class=" + assertionClass);
            sb.append(",server class=" + serverClass + "]");
            logger.warning(sb.toString());
            return;
        }

        try {
            Class a = Class.forName(assertionClass, true, classLoader);
            Class eClass = null;
            if (editorClass != null && !"".equals(editorClass)) {
                eClass = Class.forName(editorClass, true, classLoader);
            }

            Class sa = Class.forName(serverClass, true, classLoader);
            SecurityManager sm = null;
            if (securityManagerClass != null) {
                sm = (SecurityManager)Class.forName(securityManagerClass, true, classLoader).newInstance();
            }
            CustomAssertionDescriptor eh = new CustomAssertionDescriptor(baseKey, a, eClass, sa, category, optionalDescription, sm);
            CustomAssertions.register(eh);

            if (!StringUtils.isEmpty(extensionInterfaceClassName)) {
                CustomExtensionInterfaceBinding ceiBinding = (CustomExtensionInterfaceBinding) Class.forName(extensionInterfaceClassName, true, classLoader).newInstance();
                extensionInterfaceManager.registerInterface(ceiBinding.getInterfaceClass(), null, ceiBinding.getImplementationObject());
            }

            logger.info("Registered custom assertion " + eh);
        } catch (ClassNotFoundException e) {
            StringBuffer sb = new StringBuffer("Cannot load class(es) for custom assertion, skipping...\n");
            sb.append("[ assertion class=" + assertionClass);
            sb.append(",server class=" + serverClass + "]");
            logger.log(Level.WARNING, sb.toString(), e);
        } catch (Exception e) {
            StringBuffer sb = new StringBuffer("Invalid custom assertion, skipping...\n");
            sb.append("[ assertion class=" + assertionClass);
            sb.append(",server class=" + serverClass + "]");
            logger.log(Level.WARNING, sb.toString(), e);
        }
    }

    /**
     * Check if the given resource is within a known custom assertion package.
     *
     * @param resourcePath The path to check
     * @return true for a CA resource
     */
    private boolean isCustomAssertionResource(final String resourcePath) {
        boolean isCustomAssertionRes = false;

        Set descriptors = CustomAssertions.getAllDescriptors();
        for (Iterator iterator = descriptors.iterator(); iterator.hasNext();) {
            CustomAssertionDescriptor customAssertionDescriptor = (CustomAssertionDescriptor) iterator.next();

            // only check classes relevant to the SSM (not SSB or SSG)
            Class beanClass = customAssertionDescriptor.getAssertion();
            Class uiClass = customAssertionDescriptor.getUiClass();

            if (beanClass != null && resourcePath.startsWith(beanClass.getPackage().getName().replace('.', '/'))) {
                isCustomAssertionRes = true;
                break;
            }
            if (uiClass != null && resourcePath.startsWith(uiClass.getPackage().getName().replace('.', '/'))) {
                isCustomAssertionRes = true;
                break;
            }
        }

        return isCustomAssertionRes;
    }
}
