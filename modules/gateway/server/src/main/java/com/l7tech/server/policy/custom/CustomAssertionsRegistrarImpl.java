package com.l7tech.server.policy.custom;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.gateway.common.custom.CustomAssertionDescriptor;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.*;
import com.l7tech.policy.assertion.ext.action.CustomTaskActionUI;
import com.l7tech.policy.assertion.ext.cei.CustomExtensionInterfaceBinding;
import com.l7tech.policy.assertion.ext.licensing.CustomFeatureSetName;
import com.l7tech.policy.wsp.ClassLoaderUtil;
import com.l7tech.server.admin.ExtensionInterfaceManager;
import com.l7tech.server.store.KeyValueStoreServicesImpl;
import com.l7tech.server.policy.*;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.util.ModuleClassLoader;
import com.l7tech.util.Config;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
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
public class CustomAssertionsRegistrarImpl extends ApplicationObjectSupport implements CustomAssertionsRegistrar, InitializingBean {

    //- PUBLIC

    public CustomAssertionsRegistrarImpl(ServerAssertionRegistry assertionRegistry) {
        if (assertionRegistry == null) {
            throw new IllegalArgumentException("assertionRegistry is required");
        }
        this.assertionRegistry = assertionRegistry;
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
     * Get the known registered assertion for a given class name.
     * @param customAssertionClassName the custom assertion class name
     * @return the custom assertion holder
     */
    public CustomAssertionHolder getAssertion(final String customAssertionClassName) {
        return asCustomAssertionHolder(CustomAssertions.getDescriptor(customAssertionClassName));
    }

    /**
     * @return the list of all assertions known to the runtime
     */
    @Override
    public Collection<CustomAssertionHolder> getAssertions() {
        Set customAssertionDescriptors = CustomAssertions.getDescriptors();
        return asCustomAssertionHolders(customAssertionDescriptors);
    }

    /**
     * @param c the category to query for
     * @return the list of all assertions known to the runtime
     *         for a give n category
     */
    @Override
    public Collection<CustomAssertionHolder> getAssertions( Category c) {
        final Set customAssertionDescriptors = CustomAssertions.getDescriptors(c);
        return asCustomAssertionHolders(customAssertionDescriptors);
    }

    /**
     * Checks if there is a CustomAssertion registered which either, implements the
     * {@link com.l7tech.policy.assertion.ext.CustomCredentialSource CustomCredentialSource} interface and returns <code>true</code> for
     * {@link com.l7tech.policy.assertion.ext.CustomCredentialSource#isCredentialSource() CustomCredentialSource.isCredentialSource()} method,
     * or is placed into {@link Category#ACCESS_CONTROL ACCESS_CONTROL} category.
     *
     * @return true if there is a CustomAssertion registered which is credential source, false otherwise.
     */
    @Override
    public boolean hasCustomCredentialSource() {
        try
        {
            //noinspection unchecked
            Set<CustomAssertionDescriptor> descriptors = CustomAssertions.getDescriptors();
            for (CustomAssertionDescriptor descriptor : descriptors) {
                if (descriptor.hasCategory(Category.ACCESS_CONTROL)) {
                    return true;
                } else if (CustomCredentialSource.class.isAssignableFrom(descriptor.getAssertion())) {
                    final CustomCredentialSource customAssertion = (CustomCredentialSource)descriptor.getAssertion().newInstance();
                    if (customAssertion.isCredentialSource()) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error while determining if there is a CustomAssertion registered which is credential source.", e);
        }

        return false;
    }

        /**
        * Return the <code>CustomAssertionDescriptor</code> for a given assertion or <b>null<b>.
        * Note that this method may not be invoked from management console.
        * Server classes may not de-serialize into the ssm environment.
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

    @Override
    public CustomTaskActionUI getTaskActionUI(String assertionClassName) {
        return CustomAssertions.getTaskActionUI(assertionClassName);
    }

    /**
     *
     */
    public void setServerConfig(Config config ) {
        this.config = config;
    }

    public void setExtensionInterfaceManager(ExtensionInterfaceManager extensionInterfaceManager) {
        this.extensionInterfaceManager = extensionInterfaceManager;
    }

    public void setSecurePasswordManager(SecurePasswordManager securePasswordManager) {
        this.securePasswordManager = securePasswordManager;
    }

    public void setCustomKeyValueStoreManager(CustomKeyValueStoreManager customKeyValueStoreManager) {
        this.customKeyValueStoreManager = customKeyValueStoreManager;
    }

    /**
     * Perform bean initialization after properties are set.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if ( config == null) {
            throw new IllegalArgumentException("Server Config is required");
        }

        if (extensionInterfaceManager == null) {
            throw new IllegalArgumentException("Extension Interface Manager is required");
        }

        if (securePasswordManager == null) {
            throw new IllegalArgumentException("Secure Password Manager is required");
        }

        if (customKeyValueStoreManager == null) {
            throw new IllegalArgumentException("Custom Key Value Store Manager is required");
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
    private ExtensionInterfaceManager extensionInterfaceManager;
    private SecurePasswordManager securePasswordManager;
    private CustomKeyValueStoreManager customKeyValueStoreManager;

    private Collection<CustomAssertionHolder> asCustomAssertionHolders(final Set customAssertionDescriptors) {
        Collection<CustomAssertionHolder> result = new ArrayList<>();
        for (Object customAssertionDescriptor : customAssertionDescriptors) {
            CustomAssertionHolder customAssertionHolder = asCustomAssertionHolder((CustomAssertionDescriptor) customAssertionDescriptor);
            if (customAssertionHolder != null) {
                result.add(customAssertionHolder);
            }
        }
        return result;
    }

    private CustomAssertionHolder asCustomAssertionHolder(final CustomAssertionDescriptor customAssertionDescriptor) {
        CustomAssertionHolder customAssertionHolder = null;
        try {
            Class ca = customAssertionDescriptor.getAssertion();
            customAssertionHolder = new CustomAssertionHolder();
            final CustomAssertion cas = (CustomAssertion) ca.newInstance();
            customAssertionHolder.setCustomAssertion(cas);
            customAssertionHolder.setCategories(customAssertionDescriptor.getCategories());
            customAssertionHolder.setDescriptionText(customAssertionDescriptor.getDescription());
            customAssertionHolder.setPaletteNodeName(customAssertionDescriptor.getPaletteNodeName());
            customAssertionHolder.setPolicyNodeName(customAssertionDescriptor.getPolicyNodeName());
            customAssertionHolder.setIsUiAutoOpen(customAssertionDescriptor.getIsUiAutoOpen());
            customAssertionHolder.setModuleFileName(customAssertionDescriptor.getModuleFileName());
            if (cas instanceof CustomFeatureSetName) {
                CustomFeatureSetName customFeatureSetName = (CustomFeatureSetName) cas;
                customAssertionHolder.setRegisteredCustomFeatureSetName(customFeatureSetName.getFeatureSetName());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to instantiate custom assertion", e);
        }
        return customAssertionHolder;
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
                    if (initFromUrl(resourceUrl, caClassLoader, parseModuleFileName(resourceUrl.getPath(), fileName)))
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

    private boolean initFromUrl(URL customAssertionConfigUrl, ClassLoader classLoader, String moduleFileName) {
        boolean loaded = false;
        InputStream in = null;
        try {
            in = customAssertionConfigUrl.openStream();
            loadCustomAssertions(in, classLoader, moduleFileName);
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

    private void loadCustomAssertions(InputStream in, ClassLoader classLoader, String moduleFileName) throws IOException {
        Properties props = new Properties();
        props.load(in);

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            if (contextClassLoader != null) Thread.currentThread().setContextClassLoader(null);
            for (Object o : props.keySet()) {
                String key = o.toString();
                if (key.endsWith(".class")) {
                    loadSingleCustomAssertion(key.substring(0, key.indexOf(".class")), props, classLoader, moduleFileName);
                }
            }
        }
        finally {
            if (contextClassLoader != null) Thread.currentThread().setContextClassLoader(contextClassLoader);            
        }
    }

    private void loadSingleCustomAssertion(String baseKey, Properties properties, ClassLoader classLoader, String moduleFileName) {
        String assertionClass = (String)properties.get(baseKey + ".class");
        String serverClass = (String) properties.get(baseKey + ".server");

        if (serverClass == null || assertionClass == null) {
            StringBuilder sb = new StringBuilder("Incomplete custom assertion, skipping\n");
            sb.append("[ assertion class=").append(assertionClass);
            sb.append(",server class=").append(serverClass).append("]");
            logger.warning(sb.toString());
            return;
        }

        try {
            Class a = Class.forName(assertionClass, true, classLoader);
            Class sa = Class.forName(serverClass, true, classLoader);

            // extract categories
            final Set<Category> categories = Category.asCategorySet((String) properties.get(baseKey + ".category"));
            if (categories.isEmpty()) {
                categories.add(Category.UNFILLED); // it must have a category
            }

            CustomAssertionDescriptor eh = new CustomAssertionDescriptor(baseKey, a, sa, categories);

            String editorClass = (String) properties.get(baseKey + ".ui");
            if (editorClass != null && !"".equals(editorClass)) {
                eh.setUiClass(Class.forName(editorClass, true, classLoader));
            }

            String taskActionClass = (String) properties.get(baseKey + ".task.action.ui");
            if (taskActionClass != null && !"".equals(taskActionClass)) {
                eh.setTaskActionUiClass(Class.forName(taskActionClass, true, classLoader));
            }

            eh.setDescription((String) properties.get(baseKey + ".description"));
            eh.setUiAutoOpen(Boolean.parseBoolean((String) properties.get(baseKey + ".ui.auto.open")));
            eh.setUiAllowedPackages((String) properties.get(baseKey + ".ui.allowed.packages"));
            eh.setUiAllowedResources((String) properties.get(baseKey + ".ui.allowed.resources"));
            eh.setPaletteNodeName((String) properties.get(baseKey + ".palette.node.name"));
            eh.setPolicyNodeName((String) properties.get(baseKey + ".policy.node.name"));
            eh.setModuleFileName(moduleFileName);
            CustomAssertions.register(eh);

            registerCustomExtensionInterface((String) properties.get(baseKey + ".extension.interface"), classLoader);

            logger.info("Registered custom assertion " + eh.getAssertion().getName() + " from module " + moduleFileName);
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Custom assertion " + eh);
            }

        } catch (ClassNotFoundException e) {
            StringBuilder sb = new StringBuilder("Cannot load class(es) for custom assertion, skipping...\n");
            sb.append("[ assertion class=").append(assertionClass);
            sb.append(",server class=").append(serverClass).append("]");
            logger.log(Level.WARNING, sb.toString(), e);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder("Invalid custom assertion, skipping...\n");
            sb.append("[ assertion class=").append(assertionClass);
            sb.append(",server class=").append(serverClass).append("]");
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
        for (Object descriptor : descriptors) {
            CustomAssertionDescriptor customAssertionDescriptor = (CustomAssertionDescriptor) descriptor;

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

            // check if the resourcePath is in the UI allowed package list.
            String[] uiAllowedPackages = customAssertionDescriptor.getUiAllowedPackages();
            for (String currentUiAllowedPackage : uiAllowedPackages) {
                if (resourcePath.startsWith(currentUiAllowedPackage)) {
                    isCustomAssertionRes = true;
                    break;
                }
            }
            if (isCustomAssertionRes) {
                break;
            }

            // check if the resourcePath is in the UI allowed resources list.
            Set<String> uiAllowedResources = customAssertionDescriptor.getUiAllowedResources();
            if (uiAllowedResources.contains(resourcePath)) {
                isCustomAssertionRes = true;
                break;
            }
        }

        return isCustomAssertionRes;
    }

    protected final void registerCustomExtensionInterface(String extensionInterfaceClassName, ClassLoader classLoader) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (CustomExtensionInterfaceBinding.getServiceFinder() == null) {
            // Set ServiceFinder in CustomExtensionInterfaceBinding.
            // The service finder is a static variable. All SSM will use the same instance of service
            // finder in the SSG. Ensure that the services added to the service finder is thread-safe.
            // SecurePasswordServicesImpl only does thread-safe read operations.
            //
            ServiceFinderImpl serviceFinder = new ServiceFinderImpl();
            serviceFinder.setSecurePasswordServicesImpl(new SecurePasswordServicesImpl(securePasswordManager));
            serviceFinder.setKeyValueStoreImpl(new KeyValueStoreServicesImpl(customKeyValueStoreManager));
            CustomExtensionInterfaceBinding.setServiceFinder(serviceFinder);
        }

        if (!StringUtils.isEmpty(extensionInterfaceClassName)) {
            CustomExtensionInterfaceBinding ceiBinding = (CustomExtensionInterfaceBinding) Class.forName(extensionInterfaceClassName, true, classLoader).newInstance();
            extensionInterfaceManager.registerInterface(ceiBinding.getInterfaceClass(), null, ceiBinding.getImplementationObject());
        }
    }

    protected final String parseModuleFileName(@NotNull final String configFileUrlPath, @NotNull final String configFileName) {
        String moduleFileName = configFileUrlPath;
        String fileSeparator = File.separator;
        int index = moduleFileName.indexOf("!" + fileSeparator + configFileName);
        if (index < 0) {
            // handle Cygwin development environment
            fileSeparator = "/";
            index = moduleFileName.indexOf("!" + fileSeparator + configFileName);
        }
        moduleFileName = moduleFileName.substring(0, index);
        moduleFileName = moduleFileName.substring(moduleFileName.lastIndexOf(fileSeparator) + 1);
        return moduleFileName;
    }
}
