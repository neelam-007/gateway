package com.l7tech.server.policy;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.policy.AllAssertions;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.*;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.admin.ExtensionInterfaceManager;
import com.l7tech.server.event.system.LicenseChangeEvent;
import com.l7tech.server.policy.module.*;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Gateway's AssertionRegistry, which extends the default registry with the ability to look for
 * modular ServerConfig properties in the new assertions and register them with ServerConfig.
 * @noinspection ContinueStatement,ContinueStatement
 */
public class ServerAssertionRegistry extends AssertionRegistry implements DisposableBean {
    /** @noinspection FieldNameHidesFieldInSuperclass*/
    protected static final Logger logger = Logger.getLogger(ServerAssertionRegistry.class.getName());

    // Install the default getters that are specific to the Gateway
    private static final AtomicBoolean gatewayMetadataDefaultsInstalled = new AtomicBoolean(false);

    static {
        installGatewayMetadataDefaults();
    }

    //
    //  Instance fields
    //

    private final ServerConfig serverConfig;
    private final LicenseManager licenseManager;
    private final ExtensionInterfaceManager extensionInterfaceManager;
    private final Map<String, String[]> newClusterProps = new ConcurrentHashMap<>();

    // the assertion module jars scanner
    private ModularAssertionsScanner assertionsScanner;


    /**
     * Construct a new ServerAssertionRegistry that will get its information from the specified serverConfig
     * instance.
     *
     * @param serverConfig a ServerConfig instance that provides information about the module directory to search
     * @param licenseManager the licenseManager, for checking to see if scanning the modules directory is enabled
     */
    public ServerAssertionRegistry(ServerConfig serverConfig, LicenseManager licenseManager, ExtensionInterfaceManager extensionInterfaceManager) {
        if (serverConfig == null) throw new IllegalArgumentException("A non-null serverConfig is required");
        if (licenseManager == null) throw new IllegalArgumentException("A non-null licenseManager is required");
        this.serverConfig = serverConfig;
        this.licenseManager = licenseManager;
        this.extensionInterfaceManager = extensionInterfaceManager;
        installGatewayMetadataDefaults();
    }

    @Override
    protected void onApplicationContextSet() {
        for (Assertion assertion : AllAssertions.GATEWAY_EVERYTHING) {
            if (!isAssertionRegistered(assertion.getClass().getName()))
                registerAssertion(assertion.getClass());
        }

        ApplicationEventMulticaster eventMulticaster = getApplicationContext().getBean( "applicationEventMulticaster", ApplicationEventMulticaster.class );
        eventMulticaster.addApplicationListener(new ApplicationListener() {
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                ServerAssertionRegistry.this.onApplicationEvent(event);
            }
        });
    }

    @Override
    public synchronized Assertion registerAssertion(Class<? extends Assertion> assertionClass) {
        Assertion prototype = super.registerAssertion(assertionClass);
        final AssertionMetadata meta = prototype.meta();
        gatherClusterProps(meta);
        registerExtensionInterfaces(meta);
        return prototype;
    }

    private synchronized void gatherClusterProps(AssertionMetadata meta) {
        // Check if the new assertion requires any new serverConfig properties.
        //noinspection unchecked
        Map<String, String[]> newProps = meta.get(AssertionMetadata.CLUSTER_PROPERTIES);
        if (newProps != null) {
            for (Map.Entry<String, String[]> entry : newProps.entrySet()) {
                final String name = entry.getKey();
                final String[] tuple = entry.getValue();
                String desc = tuple != null && tuple.length > 0 ? tuple[0] : null;
                String dflt = tuple != null && tuple.length > 1 ? tuple[1] : null;
                newClusterProps.put(name, new String[] { desc, dflt });
            }
        }
    }

    private void registerExtensionInterfaces(AssertionMetadata meta) {
        Functions.Unary< Collection<ExtensionInterfaceBinding>, ApplicationContext > factory = meta.get(AssertionMetadata.EXTENSION_INTERFACES_FACTORY);
        if (factory != null) {
            Collection<ExtensionInterfaceBinding> bindings = factory.call(getApplicationContext());
            if (bindings != null) {
                for (ExtensionInterfaceBinding<?> binding : bindings) {
                    try {
                        logger.log(Level.INFO, "Registering admin extension interface: " + binding.getInterfaceClass());
                        extensionInterfaceManager.registerInterface(binding);
                    } catch (Exception e) {
                        //noinspection ThrowableResultOfMethodCallIgnored
                        logger.log(Level.SEVERE, "Unable to register admin extension interface " + binding.getInterfaceClass() + " for modular assertion " + meta.getAssertionClass() +
                                ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    }
                }
            }
        }
    }

    /** Scan modular assertions for new cluster properties. */
    private synchronized void scanForNewClusterProperties() {
        Map<String,String> namesToDesc =  serverConfig.getClusterPropertyNames();
        Set<String> knownNames = namesToDesc.keySet();

        List<String[]> toAdd = new ArrayList<>();

        //noinspection ConstantConditions
        if (newClusterProps == null)
            return;

        for (Map.Entry<String, String[]> entry : newClusterProps.entrySet()) {
            String clusterPropertyName = entry.getKey();
            String[] tuple = entry.getValue();
            if (!knownNames.contains(clusterPropertyName)) {
                // Dynamically register this new cluster property
                String desc = tuple[0];
                String dflt = tuple[1];
                String serverConfigName = ClusterProperty.asServerConfigPropertyName(clusterPropertyName);

                toAdd.add(new String[] { serverConfigName, clusterPropertyName, desc, dflt });
                logger.info("Dynamically registering cluster property " + clusterPropertyName);
            }
        }
        if (!toAdd.isEmpty()) {
            //noinspection deprecation
            serverConfig.registerServerConfigProperties(toAdd.toArray(new String[toAdd.size()][]));
        }
    }

    private synchronized boolean scanModularAssertions() {
        return assertionsScanner.scanModules();
    }

    /**
     * Runs any needed scan and doesn't return until it has finished.
     * If a scan is needed, and another thread isn't already running one, this will run the scan in
     * the current thread.
     */
    public void runNeededScan() {
        synchronized (this) {
            if (!assertionsScanner.isScanNeeded())
                return;
            if (scanModularAssertions()) {
                scanForNewClusterProperties();
            }
        }
    }

    /**
     * Find the assertion module, if any, that owns the specified class loader.
     *
     * @param classLoader the class loader to check.  Any code that suspects it may be running as a modular
     *                    assertion can just pass as this argument the result of <tt>getClass().getClassLoader()</tt>.
     * @return the {@link com.l7tech.server.policy.module.ModularAssertionModule} that provides this class loader, or null if no currently registered AssertionModule owns
     *         the specified ClassLoader.
     */
    public ModularAssertionModule getModuleForClassLoader(ClassLoader classLoader) {
        for (ModularAssertionModule module : assertionsScanner.getModules())
            if (classLoader == module.getModuleClassLoader())
                return module;
        return null;
    }

    /**
     * @see ModularAssertionsScanner#getModuleForPackage(String)
     */
    public ModularAssertionModule getModuleForPackage(@NotNull String packageName) {
        return assertionsScanner.getModuleForPackage(packageName);
    }

    /**
     * @return a view of all assertion modules which are currently loaded.  May be empty but never null.
     */
    public Set<ModularAssertionModule> getLoadedModules() {
        return new HashSet<>(assertionsScanner.getModules());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        // create the modules scanner config object
        final ModularAssertionModulesConfig modulesConfig = new ModularAssertionModulesConfig(serverConfig, licenseManager);

        // create modular assertions callback
        final ScannerCallbacks.ModularAssertion modularAssertionCallbacks = new ScannerCallbacks.ModularAssertion() {
            @Override
            public void publishEvent(@NotNull final ApplicationEvent applicationEvent) {
                if (!isShuttingDown()) {
                    ServerAssertionRegistry.this.publishEvent(applicationEvent);
                }
            }

            @Override
            public void registerClusterProps(@NotNull final Set<? extends Assertion> assertionPrototypes) {
                for (Assertion proto : assertionPrototypes) {
                    gatherClusterProps(proto.meta());
                }
                scanForNewClusterProperties();
            }

            @Override
            public void registerAssertion(@NotNull final Class<? extends Assertion> aClass) {
                ServerAssertionRegistry.this.registerAssertion(aClass);
            }

            @Override
            public void unregisterAssertion(@NotNull final Assertion prototype) {
                ServerAssertionRegistry.this.unregisterAssertion(prototype);
            }

            @NotNull
            @Override
            public ApplicationContext getApplicationContext() {
                return ServerAssertionRegistry.this.getApplicationContext();
            }

            @Override
            public boolean isAssertionRegistered(@NotNull final String assClassName) {
                return ServerAssertionRegistry.this.isAssertionRegistered(assClassName);
            }
        };

        // create the custom assertion scanner
        assertionsScanner = new ModularAssertionsScanner(modulesConfig, modularAssertionCallbacks);

        // do initial scan ones, ignoring the result, before starting the timer.
        scanModularAssertions();

        scanForNewClusterProperties();

        // start the timer
        assertionsScanner.startTimer(
                modulesConfig.getRescanPeriodMillis(),
                new Runnable() {
                    @Override
                    public void run() {
                        // skip while shutting down
                        if (isShuttingDown()) {
                            return;
                        }

                        scanModularAssertions();
                        scanForNewClusterProperties();
                    }
                }
        );
    }

    private static void installGatewayMetadataDefaults() {
        if (gatewayMetadataDefaultsInstalled.get())
            return;

        DefaultAssertionMetadata.putDefaultGetter(AssertionMetadata.CLUSTER_PROPERTIES, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return DefaultAssertionMetadata.cache(meta, key, new HashMap());
            }
        });

        gatewayMetadataDefaultsInstalled.set(true);
    }

    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (isShuttingDown())
            return; // Ignore events once shutdown starts

        if (applicationEvent instanceof LicenseChangeEvent) {
            // License has changed.  Ensure that module rescan occurs.
            assertionsScanner.onLicenseChange();
        }
    }

    @Override
    public synchronized void destroy() throws Exception {
        super.destroy();

        // destroy the scanner.
        // will close the rescan timer, unload all scannedModules and clear failModTimes maps.
        assertionsScanner.destroy();
    }

}
