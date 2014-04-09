package com.l7tech.server.policy.custom;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.gateway.common.custom.CustomAssertionDescriptor;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.*;
import com.l7tech.policy.assertion.ext.action.CustomTaskActionUI;
import com.l7tech.policy.assertion.ext.cei.CustomExtensionInterfaceBinding;
import com.l7tech.policy.assertion.ext.entity.CustomEntitySerializer;
import com.l7tech.policy.assertion.ext.licensing.CustomFeatureSetName;
import com.l7tech.policy.wsp.ClassLoaderUtil;
import com.l7tech.server.admin.ExtensionInterfaceManager;
import com.l7tech.server.policy.CustomKeyValueStoreManager;
import com.l7tech.server.policy.SecurePasswordServicesImpl;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.policy.ServiceFinderImpl;
import com.l7tech.server.policy.module.*;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.store.KeyValueStoreServicesImpl;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.support.ApplicationObjectSupport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
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
public class CustomAssertionsRegistrarImpl extends ApplicationObjectSupport implements CustomAssertionsRegistrar, InitializingBean, DisposableBean {

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
        Set<ModularAssertionModule> mods = assertionRegistry.getLoadedModules();
        for (ModularAssertionModule mod : mods) {
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

        Set<ModularAssertionModule> modules = assertionRegistry.getLoadedModules();
        if ( modules != null ) {

            PoolByteArrayOutputStream baos = null;
            try {
                baos= new PoolByteArrayOutputStream(64*1024);
                ZipOutputStream zipOut = null;
                try {
                    for ( ModularAssertionModule module : modules ) {
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
    @Override
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

    @Override
    public CustomEntitySerializer getExternalEntitySerializer(final String extEntitySerializerClassName) {
        return CustomAssertions.getExternalEntitySerializer(extEntitySerializerClassName);
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

        // create custom assertion modules scanner config
        final CustomAssertionModulesConfig modulesConfig = new CustomAssertionModulesConfig(config);

        // create custom assertion callbacks
        final ScannerCallbacks.CustomAssertion customAssertionCallbacks = new ScannerCallbacks.CustomAssertion() {
            @Override
            public void registerAssertion(@NotNull final CustomAssertionDescriptor descriptor) throws ModuleException {
                try {
                    CustomAssertions.register(descriptor);
                    registerCustomExtensionInterface(descriptor.getExtensionInterfaceClass());
                } catch ( IllegalAccessException | InstantiationException e) {
                    throw new ModuleException("Error while registering custom assertion [" + descriptor.getName() + "] extension interface, for module [" + descriptor.getModuleFileName() + "]", e);
                } catch (Throwable e) {
                    throw new ModuleException("Error while registering custom assertion [" + descriptor.getName() + "] for module [" + descriptor.getModuleFileName() + "]", e);
                }
            }

            @Override
            public void unregisterAssertion(@NotNull final CustomAssertionDescriptor descriptor) {
                CustomAssertions.unregister(descriptor);
            }

            @Override
            public ServiceFinder getServiceFinder() {
                return CustomAssertionsRegistrarImpl.this.getServiceFinderInstance();
            }

            @Override
            public void publishEvent(@NotNull final ApplicationEvent event) {
                CustomAssertionsRegistrarImpl.this.publishEvent(event);
            }
        };

        // create the custom assertion scanner
        assertionsScanner = new CustomAssertionsScanner(modulesConfig, customAssertionCallbacks);

        // do initial scan ones, ignoring the result, before starting the timer.
        assertionsScanner.scanModules();

        // construct the custom assertion class loader after the initial scan is complete.
        ClassLoader caClassLoader = getClass().getClassLoader();
        if (modulesConfig.getModuleDir() != null) {
            caClassLoader = new AllCustomAssertionClassLoader(
                    AllCustomAssertionClassLoader.class.getClassLoader(),
                    assertionsScanner.getModules(),
                    caClassLoader);
            ClassLoaderUtil.setClassloader(caClassLoader);
        }
        customAssertionClassloader = caClassLoader;

        // start the timer
        assertionsScanner.startTimer(modulesConfig.getRescanPeriodMillis());
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(CustomAssertionsRegistrar.class.getName());

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

    /**
     * Register Custom assertion extension interface.
     *
     * @param extensionInterfaceClass    extension interface class.
     * @throws IllegalAccessException    if the class, located from the specified class name, or its nullary constructor is not accessible.
     * @throws InstantiationException    if the class, located from the specified class name, has no nullary constructor or if the instantiation fails for some other reason.
     */
    protected final void registerCustomExtensionInterface(
            @Nullable final Class<? extends CustomExtensionInterfaceBinding> extensionInterfaceClass
    ) throws IllegalAccessException, InstantiationException {
        if (CustomExtensionInterfaceBinding.getServiceFinder() == null) {
            CustomExtensionInterfaceBinding.setServiceFinder(getServiceFinderInstance());
        }

        if (extensionInterfaceClass != null) {
            final CustomExtensionInterfaceBinding ceiBinding = extensionInterfaceClass.newInstance();
            extensionInterfaceManager.registerInterface(ceiBinding.getInterfaceClass(), null, ceiBinding.getImplementationObject(), true);
        }
    }

    @Deprecated
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

    /**
     * A helper function to publish application events using the {@link ApplicationContext}.
     * @param event    the event to publish.
     */
    private void publishEvent(@NotNull final ApplicationEvent event) {
        final ApplicationContext context = getApplicationContext();
        if (context != null) {
            context.publishEvent(event);
        }
    }

    // the assertion module jars scanner
    private CustomAssertionsScanner assertionsScanner = null;

    @Override
    public void destroy() throws Exception {
        assertionsScanner.destroy();
    }

    // singleton custom assertions service finder implementation
    private ServiceFinderImpl serviceFinder = null;

    /**
     * Obtain a singleton instance of the custom assertions {@link ServiceFinder}.
     */
    private ServiceFinderImpl getServiceFinderInstance() {
        if (securePasswordManager == null) throw new IllegalStateException("securePasswordManager is not initialized");
        if (customKeyValueStoreManager == null) throw new IllegalStateException("customKeyValueStoreManager is not initialized");

        if (serviceFinder == null) {
            // Set ServiceFinder used in CustomExtensionInterfaceBinding and CustomExternalReferenceResolver.
            // The service finder is a singleton, all SSM will use the same instance of service finder in the SSG.
            // Ensure that the services added to the service finder is thread-safe.
            // SecurePasswordServicesImpl only does thread-safe read operations.
            // TODO: verify that CustomKeyValueStoreManager does only thread-safe operations.

            serviceFinder = new ServiceFinderImpl();
            serviceFinder.setSecurePasswordServicesImpl(new SecurePasswordServicesImpl(securePasswordManager));
            serviceFinder.setKeyValueStoreImpl(new KeyValueStoreServicesImpl(customKeyValueStoreManager));
        }
        return serviceFinder;
    }
}
