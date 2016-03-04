package com.l7tech.server.policy.custom;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.gateway.common.custom.CustomAssertionDescriptor;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.module.AssertionModuleInfo;
import com.l7tech.gateway.common.module.ModuleLoadingException;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.*;
import com.l7tech.policy.assertion.ext.action.CustomTaskActionUI;
import com.l7tech.policy.assertion.ext.cei.CustomExtensionInterfaceBinding;
import com.l7tech.policy.assertion.ext.entity.CustomEntitySerializer;
import com.l7tech.policy.assertion.ext.licensing.CustomFeatureSetName;
import com.l7tech.policy.wsp.ClassLoaderUtil;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.admin.ExtensionInterfaceManager;
import com.l7tech.server.module.AssertionModuleFinder;
import com.l7tech.server.policy.CustomKeyValueStoreManager;
import com.l7tech.server.policy.SecurePasswordServicesImpl;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.policy.ServiceFinderImpl;
import com.l7tech.server.policy.module.*;
import com.l7tech.server.security.SignerServicesImpl;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.store.KeyValueStoreServicesImpl;
import com.l7tech.util.*;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.core.io.ContextResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

/**
 * The server side CustomAssertionsRegistrar implementation.
 *
 * @author emil
 * @version 16-Feb-2004
 */
public class CustomAssertionsRegistrarImpl extends ApplicationObjectSupport implements CustomAssertionsRegistrar, InitializingBean, DisposableBean, AssertionModuleFinder<CustomAssertionModule> {

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
    public byte[] getAssertionResourceDataAsBytes(Collection<String> names) {
        long start = System.currentTimeMillis();
        ByteArrayOutputStream baos = null;
        try {
            Collection<Pair<String,AssertionResourceData>> result = new ArrayList<>(names.size());
            for (String name : names) {
                result.add(new Pair<>(name, getAssertionResourceData(name)));
            }
            baos = new ByteArrayOutputStream();
            GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
            ObjectOutputStream objectOut = new ObjectOutputStream(gzipOut);
            objectOut.writeObject(result);
            objectOut.flush();
            objectOut.close();
            byte[] bytes = baos.toByteArray();

            if (logger.isLoggable( Level.FINE )) {
                logger.log( Level.FINE, "Assertion resource data byte length: " + bytes.length);
            }
            return bytes;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error streaming assertion resource as bytes: " + ExceptionUtils.getMessage(e), e);
        } finally {
            ResourceUtils.closeQuietly(baos);
            if (logger.isLoggable( Level.FINE )) {
                long end = System.currentTimeMillis();
                double total = ((double)end - (double)start) / 1000;
                logger.log(Level.FINE,String.format("%6.2f sec for: %s\n", total, "get assertion resource data as bytes."));
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

        return gzipTar( resourcePath );
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

    private AssertionResourceData gzipTar( @NotNull final String resourcePath ) {
        AssertionResourceData data = null;

        final long startTime = System.currentTimeMillis();

        Set<ModularAssertionModule> modules = assertionRegistry.getLoadedModules();

        PoolByteArrayOutputStream baos = null;
        try {
            baos= new PoolByteArrayOutputStream(64*1024);
            TarArchiveOutputStream tarOut = null;
            try {
                for ( ModularAssertionModule module : modules ) {
                    byte[] resourceData = module.getResourceBytes( resourcePath, true );
                    if ( resourceData != null ) {
                        HashSet<String> paths = getPaths(resourcePath, module.getClientResources(), module);

                        if (logger.isLoggable( Level.FINE )) {
                            logger.log( Level.FINE, "Path size: " + paths.size());
                        }

                        for (String path : paths) {
                            if ( isSameOrSubPackage(resourcePath, path) ) {
                                byte[] resourceBytes = module.getResourceBytes( path, true );
                                if ( resourceBytes == null ) {
                                    logger.warning( "Missing resource '"+path+"'." );
                                    continue;
                                }
                                if ( tarOut == null ) { // assume tar must have an entry, so creation is lazy
                                    tarOut = new TarArchiveOutputStream( new GZIPOutputStream(new NonCloseableOutputStream(baos)) );
                                    tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
                                }
                                TarArchiveEntry entry = new TarArchiveEntry( path );
                                entry.setSize(resourceBytes.length);
                                tarOut.putArchiveEntry( entry );
                                tarOut.write( resourceBytes );
                                tarOut.closeArchiveEntry();
                            }
                        }

                        if ( tarOut == null ) {
                            // Resource not indexed, so return single item
                            data = new AssertionResourceData( resourcePath, false, resourceData);
                        }
                        break;
                    }
                }
            } catch (IOException e) {
                logger.log( Level.WARNING, "Error creating resource GZIP TAR.", e );
            } finally {
                ResourceUtils.closeQuietly( tarOut );
            }

            if ( tarOut != null ) {
                data = new AssertionResourceData( getPackagePath(resourcePath), true, baos.toByteArray());
            }
        } finally {
            ResourceUtils.closeQuietly( baos );
        }

        if ( data != null && logger.isLoggable( Level.FINE )) {
            logger.log( Level.FINE, "Creating " + data.getData().length + " byte assertion gzip tar for " + data.getResourceName() + " took " +(System.currentTimeMillis()-startTime)+ "ms." );
        }

        return data;
    }

    @NotNull
    private HashSet<String> getPaths(@NotNull final String resourcePath, @Nullable final String[] paths, @NotNull final ModularAssertionModule module) throws IOException {
        Resource[] resources = null;
        try {
            final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(module.getModuleClassLoader());
            resources = resolver.getResources("/" + getPackagePath(resourcePath) + "/**/*.class");
        } catch (IOException e) {
            // class from dependent jar library (AAR-INF/lib/) loaded later (e.g. org/json/JSONException in AAR-INF/lib/json-java-1.0-l7p2.jar)
            logger.log(Level.FINE, "Unable to resolve assertion resource " + resourcePath + " for optimization.  Will resolve later, likely part of dependent jar library under AAR-INF/lib/." , ExceptionUtils.getDebugException(e));
        }

        final HashSet<String> pathSet = new HashSet<>((paths == null ? 0 : paths.length) + (resources == null ? 0 : resources.length));

        if (paths != null) {
            Collections.addAll(pathSet, paths);
        }

        if (resources != null) {
            for (Resource resource : resources) {
                pathSet.add(((ContextResource) resource).getPathWithinContext());
            }
        }

        return pathSet;
    }

    private boolean isSameOrSubPackage(@NotNull final String path1, @NotNull final String path2) {
        return getPackagePath( path2 ).startsWith(getPackagePath( path1 ));
    }

    @NotNull
    private String getPackagePath( @NotNull final String path ) {
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
     * {@inheritDoc}
     */
    @Override
    public Collection<AssertionModuleInfo> getAssertionModuleInfo() {
        final Collection<AssertionModuleInfo> ret = new ArrayList<>();
        final Collection<CustomAssertionModule> modules = getLoadedModules();
        for (final CustomAssertionModule module : modules) {
            ret.add(new AssertionModuleInfo(module.getName(), module.getEntityName(), module.getDigest(), getAssertionClasses(module)));
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AssertionModuleInfo getModuleInfoForAssertionClass(final String className) {
        if (className == null) {
            throw new IllegalArgumentException("className cannot be null!");
        }
        final CustomAssertionModule module = getModuleForAssertion(className);
        if (module != null) {
            return new AssertionModuleInfo(module.getName(), module.getEntityName(), module.getDigest(), getAssertionClasses(module));
        }
        return null;
    }

    /**
     * Convenient method for gathering all assertions (their class names) registered by the specified {@code module}.
     *
     * @param module    the module to gather assertions for.
     * @return Read-only view of module assertions class names, never {@code null}.
     */
    private Collection<String> getAssertionClasses(@NotNull final CustomAssertionModule module) {
        final Collection<String> assertions = new ArrayList<>();
        for (final CustomAssertionDescriptor descriptor : module.getDescriptors()) {
            assertions.add(descriptor.getAssertion().getName());
        }
        return Collections.unmodifiableCollection(assertions);
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

    public void setSsgKeyStoreManager(SsgKeyStoreManager ssgKeyStoreManager) {
        this.ssgKeyStoreManager = ssgKeyStoreManager;
    }

    public void setDefaultKey(DefaultKey defaultKey) {
        this.defaultKey = defaultKey;
    }

    /**
     * Utility method for creating our {@link CustomAssertionsScanner} instance.<br/>
     * Useful for unit testing in order to inject mockup scanner.
     */
    public CustomAssertionsScanner createScanner(
            @NotNull final CustomAssertionModulesConfig modulesConfig,
            @NotNull final ScannerCallbacks.CustomAssertion customAssertionCallbacks
    ) {
        return new CustomAssertionsScanner(modulesConfig, customAssertionCallbacks);
    }

    /**
     * Utility method for creating our {@link CustomAssertionModulesConfig} instance.<br/>
     * Useful for unit testing in order to inject mockup scanner config.
     */
    public CustomAssertionModulesConfig createScannerConfig(@NotNull final Config config) {
        return new CustomAssertionModulesConfig(config);
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

        if (ssgKeyStoreManager == null) {
            throw new IllegalArgumentException("SSG KeyStore Manager is required");
        }

        if (defaultKey == null) {
            throw new IllegalArgumentException("Default Key is required");
        }

        // create custom assertion modules scanner config
        final CustomAssertionModulesConfig modulesConfig = createScannerConfig(config);

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
        assertionsScanner = createScanner(modulesConfig, customAssertionCallbacks);

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
    private SsgKeyStoreManager ssgKeyStoreManager;
    private DefaultKey defaultKey;

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
        if (ssgKeyStoreManager == null) throw new IllegalStateException("ssgKeyStoreManager is not initialized");
        if (defaultKey == null) throw new IllegalStateException("defaultKey is not initialized");

        if (serviceFinder == null) {
            // Set ServiceFinder used in CustomExtensionInterfaceBinding and CustomExternalReferenceResolver.
            // The service finder is a singleton, all SSM will use the same instance of service finder in the SSG.
            // Ensure that the services added to the service finder is thread-safe.
            // SecurePasswordServicesImpl only does thread-safe read operations.
            // TODO: verify that CustomKeyValueStoreManager does only thread-safe operations.

            serviceFinder = new ServiceFinderImpl();
            serviceFinder.setSecurePasswordServicesImpl(new SecurePasswordServicesImpl(securePasswordManager));
            serviceFinder.setKeyValueStoreImpl(new KeyValueStoreServicesImpl(customKeyValueStoreManager));
            serviceFinder.setSignerServicesImpl(new SignerServicesImpl(ssgKeyStoreManager, defaultKey));
        }
        return serviceFinder;
    }

    @Override
    public void loadModule(@NotNull final File stagedFile, @NotNull final ServerModuleFile moduleEntity) throws ModuleLoadingException {
        assertionsScanner.loadServerModuleFile(stagedFile, moduleEntity.getModuleSha256(), moduleEntity.getName());
    }

    @Override
    public void updateModule(@NotNull final File stagedFile, @NotNull final ServerModuleFile moduleEntity) {
        assertionsScanner.updateServerModuleFile(stagedFile, moduleEntity.getModuleSha256(), moduleEntity.getName());
    }

    @Override
    public void unloadModule(@NotNull final File stagedFile, @NotNull final ServerModuleFile moduleEntity) throws ModuleLoadingException {
        assertionsScanner.unloadServerModuleFile(stagedFile, moduleEntity.getModuleSha256(), moduleEntity.getName());
    }

    @Override
    public boolean isModuleLoaded(@NotNull final File stagedFile, @NotNull final ServerModuleFile moduleEntity) {
        return assertionsScanner.isServerModuleFileLoaded(stagedFile, moduleEntity.getModuleSha256(), moduleEntity.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public CustomAssertionModule getModuleForAssertion(@NotNull final String className) {
        for (final CustomAssertionModule module : assertionsScanner.getModules()) {
            if (module.offersClass(className)) {
                return module;
            }
        }
        return null;
    }

    /**
     * Find the assertion module, if any, that owns the specified class loader.
     *
     * @param classLoader    the class loader to check.  Required and cannot be {@code null}.
     * @return The module that provides this {@code classLoader}, or {@code null} if no currently registered
     * assertion modules owns the specified {@code ClassLoader}.
     */
    @Nullable
    @Override
    public CustomAssertionModule getModuleForClassLoader(final ClassLoader classLoader) {
        if (classLoader == null) {
            throw new IllegalArgumentException("classLoader cannot be null!");
        }
        for (final CustomAssertionModule module : assertionsScanner.getModules()) {
            if (classLoader == module.getModuleClassLoader()) {
                return module;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Note that, this method is not supported for Custom Assertions and will fail with {@code UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException always
     */
    @Nullable
    @Override
    public CustomAssertionModule getModuleForPackage(@NotNull final String packageName) {
        throw new UnsupportedOperationException("Method not supported for Custom Assertions");
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public Set<CustomAssertionModule> getLoadedModules() {
        return new HashSet<>(assertionsScanner.getModules());
    }
}
