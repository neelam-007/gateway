package com.l7tech.server.policy;

import com.l7tech.common.LicenseManager;
import com.l7tech.common.util.*;
import com.l7tech.policy.AllAssertions;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.MetadataFinder;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.system.LicenseEvent;
import com.l7tech.cluster.ClusterProperty;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * The Gateway's AssertionRegistry, which extends the default registry with the ability to look for
 * modular ServerConfig properties in the new assertions and register them with ServerConfig.
 */
public class ServerAssertionRegistry extends AssertionRegistry {
    protected static final Logger logger = Logger.getLogger(ServerAssertionRegistry.class.getName());
    private static final Pattern PATTERN_WHITESPACE = Pattern.compile("\\s+");
    private static final String DISABLED_SUFFIX = ".disabled".toLowerCase();

    // Install the default getters that are specific to the Gateway
    private static final AtomicBoolean gatewayMetadataDefaultsInstalled = new AtomicBoolean(false);

    static {
        installGatewayMetadataDefaults();
    }

    private static class ModuleException extends Exception {
        public ModuleException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    //
    //  Instance fields
    //

    private final ServerConfig serverConfig;
    private final LicenseManager licenseManager;
    private final Map<String, AssertionModule> loadedModules = new HashMap<String, AssertionModule>();
    private final Map<String, Long> failModTimes = new HashMap<String, Long>();    // should not be loaded (until mod time changes) because last time we tried it, it failed
    private final Map<String, String[]> newClusterProps = new ConcurrentHashMap<String, String[]>();
    private File lastScannedDir = null;
    private long lastScannedDirModTime = 0;
    private boolean scanNeeded = true;


    /**
     * Construct a new ServerAssertionRegistry that will get its information from the specified serverConfig
     * instance.
     *
     * @param serverConfig a ServerConfig instance that provides information about the module directory
     *                     to search
     * @param licenseManager the licenseManager, for checking to see if scanning the modules directory is enabled
     */
    public ServerAssertionRegistry(ServerConfig serverConfig, LicenseManager licenseManager) {
        if (serverConfig == null) throw new IllegalArgumentException("A non-null serverConfig is required");
        if (licenseManager == null) throw new IllegalArgumentException("A non-null licenseManager is required");
        this.serverConfig = serverConfig;
        this.licenseManager = licenseManager;
        installGatewayMetadataDefaults();
    }

    protected void onApplicationContextSet() {
        for (Assertion assertion : AllAssertions.GATEWAY_EVERYTHING) {
            if (!isAssertionRegistered(assertion.getClass().getName()))
                registerAssertion(assertion.getClass());
        }
    }

    public synchronized Assertion registerAssertion(Class<? extends Assertion> assertionClass) {
        Assertion prototype = super.registerAssertion(assertionClass);
        gatherClusterProps(prototype.meta());
        return prototype;
    }

    private synchronized void gatherClusterProps(AssertionMetadata meta) {
        // Check if the new assertion requires any new serverConfig properties.
        //noinspection unchecked
        Map<String, String[]> newProps = (Map<String, String[]>)meta.get(AssertionMetadata.CLUSTER_PROPERTIES);
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

    /** Scan modular assertions for new cluster properties. */
    private synchronized void checkForNewClusterProperties() {
        Map<String,String> namesToDesc =  serverConfig.getClusterPropertyNames();
        Set<String> knownNames = namesToDesc.keySet();

        List<String[]> toAdd = new ArrayList<String[]>();

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
        if (!toAdd.isEmpty()) serverConfig.registerServerConfigProperties(toAdd.toArray(new String[][] {}));
    }

    private synchronized boolean scanModularAssertions() {
        if (!licenseManager.isFeatureEnabled(GatewayFeatureSets.SERVICE_MODULELOADER))
            return false;


        String extsList = serverConfig.getProperty(ServerConfig.PARAM_MODULAR_ASSERTIONS_FILE_EXTENSIONS);
        if ("-".equals(extsList)) // scanning disabled
            return false;

        File dir = serverConfig.getLocalDirectoryProperty(ServerConfig.PARAM_MODULAR_ASSERTIONS_DIRECTORY, "/ssg/modules/assertions", false).getAbsoluteFile();
        long dirLastModified = dir.lastModified();
        if (!scanNeeded && dir.equals(lastScannedDir) && (dirLastModified == lastScannedDirModTime) && failModTimes.isEmpty()) {
            // No files added/removed since last scan, and no failures to retry
            return false;
        }

        logger.log(Level.FINE, "Scanning assertion modules directory {0}...", dir.getAbsolutePath());

        try {
            return scanModularAssertionsImpl(dir, dirLastModified, extsList);
        } finally {
            scanNeeded = false;
        }
    }

    private boolean scanModularAssertionsImpl(File dir, long dirLastModified, String extsList) {
        boolean changesMade = false;

        lastScannedDir = dir;
        lastScannedDirModTime = dirLastModified;

        final Collection<File> jars;
        try {
            jars = new ArrayList<File>(getMatchingFiles(dir, extsList, DISABLED_SUFFIX));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to scan assertion modules directory: " + ExceptionUtils.getMessage(e), e);
            return false;
        }
        final Set<String> jarnames = new HashSet<String>();
        for (File jar : jars) jarnames.add(jar.getName());

        // Check for disabled modules
        final Set<String> disabled = new HashSet<String>();
        final Iterator<File> it = jars.iterator();
        while (it.hasNext()) {
            File file = it.next();
            final String name = file.getName();
            if (name.toLowerCase().endsWith(DISABLED_SUFFIX)) {
                // Admin wants to disable this module
                it.remove();
                String modname = name.substring(0, name.length() - DISABLED_SUFFIX.length());
                if (logger.isLoggable(Level.FINE)) logger.fine("Pretending module file " + modname + " isn't there, because it is flagged as disabled");
                jarnames.remove(modname);
                disabled.add(modname);
            }
        }

        // Check for removed modules
        Collection<AssertionModule> modules = new ArrayList<AssertionModule>(loadedModules.values());
        for (AssertionModule module : modules) {
            String name = module.getName();
            if (!jarnames.contains(name)) {
                logger.info("Unregistering assertion module that has been removed or disabled: " + name);
                changesMade = true;
                unregisterModule(name);
            }
        }

        // check for removed failed modules
        Set<String> failedNames = failModTimes.keySet();
        for (String failedName : failedNames) {
            if (!jarnames.contains(failedName)) {
                logger.info("Forgetting about failed module that has been removed or disabled: " + failedName);
                failModTimes.remove(failedName);
            }
        }

        // Check for new or changed modules
        for (File file : jars) {
            String filename = file.getName();

            // Ignore disabled flags
            if (disabled.contains(filename))
                continue;

            long lastModified = file.lastModified();
            try {
                AssertionModule previousVersion = loadedModules.get(filename);
                if (previousVersion != null) {
                    if (previousVersion.getJarfileModifiedTime() == lastModified)
                        continue;

                    // A loaded module has changed since the last time we looked at it -- unload it and reload it
                    logger.info("Checking assertion module with updated timestamp: " + filename);

                } else if (new Long(lastModified).equals(failModTimes.get(filename))) {
                    if (logger.isLoggable(Level.FINE))
                    logger.fine("Ignoring module file " + filename + " since its modification time hasn't changed since the last time it failed to load successfully");
                    continue;
                }

                changesMade = true;
                registerModule(file);

            } catch (Throwable e) {
                logger.log(Level.SEVERE, "Unable to load modular assertion jarfile (ignoring it until it changes) " + filename + ": " + ExceptionUtils.getMessage(e), e);
                failModTimes.put(filename, lastModified);
            }
        }

        scanNeeded = false;
        return changesMade;
    }


    /**
     * Runs any needed scan and doesn't return until it has finished.
     * If a scan is needed, and another thread isn't already running one, this will run the scan in
     * the current thread.
     */
    public void runNeededScan() {
        synchronized (this) {
            if (!scanNeeded)
                return;
            scanModularAssertions();
            checkForNewClusterProperties();
        }
    }


    /**
     * Get an array of files in the specified directory that match the specified list of file extensions.
     * All comparisons are case-insensitive.
     *
     * @param dir       the directory to examine.  Must not be null
     * @param extsList  a space-separated list of file extensions that are of interest, including the initial dot.
     *                  if null, a default list will be used that includes ".jar .assertion .ass .assn .aar".
     * @param optionalSuffix if non-null, files will be considered a match if they would otherwise match without this
     *                       suffix.  For example, with extsList of ".foo .bar" and optionalSuffix of ".awesome",
     *                       this would match the filenames "bletch.foo" and "blortch.bar.awesome" but not
     *                       the filename "bloof.awesome".
     * @return a collection of matching files.  May be empty but never null.
     * @throws java.io.IOException if dir isn't a directory or can't be read
     */
    private static Collection<File> getMatchingFiles(File dir, String extsList, String optionalSuffix) throws IOException {
        if (extsList == null || extsList.length() < 1)
            extsList = ".jar .assertion .ass .assn .aar";
        final String[] exts = PATTERN_WHITESPACE.split(extsList.toLowerCase());
        final String[] extsDisabled;
        if (optionalSuffix == null) {
            extsDisabled = exts;
        } else {
            extsDisabled = new String[exts.length];
            for (int i = 0; i < exts.length; ++i)
                extsDisabled[i] = exts[i] + optionalSuffix;
        }

        File[] jars = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String lcname = name.toLowerCase();

                for (int i = 0; i < exts.length; ++i)
                    if (lcname.endsWith(exts[i]) || lcname.endsWith(extsDisabled[i]))
                        return true;

                return false;
            }
        });

        if (jars == null)
            throw new IOException("The directory " + dir.getAbsolutePath() + " cannot be read (or isn't a directory)");

        return Arrays.asList(jars);
    }


    /**
     * Attempt to register one or more assertions from the specified modular asseriton jarfile, which must
     * contain metadata declaring at least one modular assertion.
     * <p/>
     * Fires an AssertionModuleRegistrationEvent if the module is registered successfully.
     *
     * @param file the file containing the module to register.  Must be an existing, readable jarfile in the modules directory.
     * @throws ModuleException if the module could not be loaded or registered
     */
    private synchronized void registerModule(File file) throws ModuleException {
        if (file == null)
            throw new ModuleException("null module File supplied", new NullPointerException());

        String filename = file.getName();
        AssertionModule previousVersion = loadedModules.get(filename);

        try {
            // TODO XXX some annoying race conditions here if the file is changed in between getting timestamp <-> getting sha1 <-> loading jar.. doctor's answer for now
            long modifiedTime = file.lastModified();
            String sha1 = getFileSha1(file);

            if (previousVersion != null && previousVersion.getSha1().equals(sha1)) {
                logger.log(Level.INFO, "Won't reload " + filename + " since its SHA-1 hasn't changed");
                return;
            }

            JarFile jar = new JarFile(file, false);

            Manifest manifest = jar.getManifest();
            Attributes attr = manifest.getMainAttributes();
            String assertionNamesStr = attr.getValue("ModularAssertion-List");
            String[] assertionClassnames = assertionNamesStr == null ? new String[0] : assertionNamesStr.split("\\s+");
            if (assertionClassnames.length < 1) {
                logger.log(Level.WARNING, "Modular assertionNames jarfile contains no modular assertions (ignoring it) " + file.getAbsolutePath());
                return;
            }

            // Save set of exported packages so we can quickly trace future classlaoder queries to the correct module
            Set<String> packages = new HashSet<String>();
            Set<NestedZipFile> nestedJarfiles = new HashSet<NestedZipFile>();
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.startsWith("AAR-INF/lib/") && name.toLowerCase().endsWith(".jar")) {
                    // Preload nested jar file, and record the packages it offers
                    logger.info("Preloading nested JAR file " + name + " from module " + filename);
                    NestedZipFile nestedZipFile = new NestedZipFile(jar, name);
                    nestedJarfiles.add(nestedZipFile);
                    for (String dir : nestedZipFile.getDirectories()) {
                        if (dir == null || !dir.contains("/"))
                            continue;
                        String[] dircomps = dir.split("/");
                        if (dircomps.length < 2)
                            continue;
                        if (dircomps[0].endsWith("-INF"))
                            continue;
                        String packageName = name.replaceAll("/", ".");
                        packageName = ClassUtils.stripSuffix(packageName, ".");
                        packages.add(packageName);
                    }
                    continue;
                }
                
                if (entry.isDirectory()) {
                    // Maybe record directory as potential package
                    if (name == null || !name.contains("/"))
                        continue;
                    String[] components = name.split("/");
                    if (components.length < 2)
                        continue;

                    if (components[0].endsWith("-INF"))
                        continue;

                    String packageName = name.replaceAll("/", ".");
                    packageName = ClassUtils.stripSuffix(packageName, ".");
                    packages.add(packageName);
                }
            }

            AssertionModuleClassLoader assloader = new AssertionModuleClassLoader(filename, file.toURL(), getClass().getClassLoader(), nestedJarfiles);
            Set<Assertion> protos = new HashSet<Assertion>();
            for (String assertionClassname : assertionClassnames) {
                if (classExists(getClass().getClassLoader(), assertionClassname))
                    throw new InstantiationException("Declared class already exists in parent classloader: " + assertionClassname);
                Class assclass = assloader.loadClass(assertionClassname);
                if (!Assertion.class.isAssignableFrom(assclass))
                    throw new ClassCastException("Declared assertion class not assignable to Assertion: " + assclass.getName());
                if (previousVersion != null) {
                    // Class must either exist in previous version, or not at all (being new to this version)
                    // Otherwise, it will conflict with an assertio class installed by some other module
                    if (!previousVersion.offersClass(assertionClassname) && isAssertionRegistered(assertionClassname))
                        throw new InstantiationException("Declared assertion class is duplicate of assertion from another module: " + assclass.getName());

                } else {
                    // Class must not exist in any other module
                    if (getAssertionClassnames().contains(assertionClassname))
                        throw new InstantiationException("Declared assertion class is duplicate of assertion from another module: " + assclass.getName());
                }
                Assertion proto = (Assertion)assclass.newInstance();
                protos.add(proto);
            }


            AssertionModule module = new AssertionModule(filename, jar, modifiedTime, sha1, assloader, protos, packages);
            previousVersion = loadedModules.put(filename, module);
            failModTimes.clear(); // retry all failures whenever a module is loaded or unloaded
            try {
                // Register any new cluster properties, in case the assertion initialization requires them
                for (Assertion proto : protos) {
                    gatherClusterProps(proto.meta());
                }
                checkForNewClusterProperties();

                // Set up class loader delegates first, in case any of the initialization listeners needs them in place
                for (Assertion proto : protos) {
                    ClassLoader dcl = (ClassLoader)proto.meta().get(AssertionMetadata.MODULE_CLASS_LOADER_DELEGATE_INSTANCE);
                    if (dcl != null)
                        assloader.addDelegate(dcl);
                }

                // Notify any module load listeners declared by any of this module's assertions
                for (Assertion proto : protos) {
                    String listenerClassname = (String)proto.meta().get(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME);
                    if (listenerClassname != null && listenerClassname.length() > 0) {
                        logger.info("Initializing dynamic module " + filename);
                        onModuleLoaded(proto.getClass(), listenerClassname);
                    }
                }

                for (Assertion proto : protos) {
                    String adjective = previousVersion == null ? "newly-registered" : "just-upgraded";
                    logger.info("Registering dynamic assertion " + proto.getClass().getName() + " from " + adjective + " module " + filename + " (module SHA-1 " + sha1 + ")");
                    registerAssertion(proto.getClass());
                }
            } finally {
                if (previousVersion != null)
                    onModuleUnloaded(previousVersion);
            }
            publishEvent(new AssertionModuleRegistrationEvent(this, module));

        } catch (IOException e) {
            throw new ModuleException("Unable to read module: " + ExceptionUtils.getMessage(e), e);
        } catch (ClassNotFoundException e) {
            throw new ModuleException("Unable to find declared assertion: " + ExceptionUtils.getMessage(e), e);
        } catch (ClassCastException e) {
            throw new ModuleException("Declared class was not an Assertion: " + ExceptionUtils.getMessage(e), e);
        } catch (IllegalAccessException e) {
            throw new ModuleException("Unable to instantiate modular assertion: " + ExceptionUtils.getMessage(e), e);
        } catch (InstantiationException e) {
            throw new ModuleException("Unable to instantiate modular assertion: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private boolean classExists(ClassLoader classLoader, String classname) {
        try {
            return classLoader.loadClass(classname) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Unregister a module, perhaps because we noticed that its file has been deleted.
     *
     * @param moduleName the name of the module to unload, ie "RateLimitAssertion-3.7.0.jar".  Must not be null or empty.
     * @return true if a module was unloaded (and an event fired).
     */
    public synchronized boolean unregisterModule(String moduleName) {
        AssertionModule module = loadedModules.get(moduleName);
        if (module == null)
            return false;

        final String name = module.getName();
        loadedModules.remove(name);
        failModTimes.clear(); // retry all failures whenever a module is loaded or unloaded

        // Unregister all assertions from this module
        Set<? extends Assertion> protos = module.getAssertionPrototypes();
        for (Assertion proto : protos) {
            logger.info("Unregistering dynamic assertion " + proto.getClass().getName() + " from just-unregistered module " + name);
            unregisterAssertion(proto);
        }

        onModuleUnloaded(module);

        return true;
    }

    /**
     * Notify the specified assertion's module loader listener that its module has been loaded, if it
     * declares an appropriate listener classname.
     *
     * @param assclass  the assertion class.  required
     * @param classname  the name of the listener class to invoke.  Required.
     */
    private void onModuleLoaded(Class<? extends Assertion> assclass, String classname) {
        if (classname == null || classname.length() < 1) return;
        try {
            Class listenerClass = assclass.getClassLoader().loadClass(classname);
            Method listenerMethod = listenerClass.getMethod("onModuleLoaded", ApplicationContext.class);
            int mods = listenerMethod.getModifiers();
            if (!Modifier.isStatic(mods)) {
                logger.log(Level.WARNING, "Modular assertion " + assclass.getName() + " declares a module load listener but its onModuleLoaded listenerMethod isn't static");
                return;
            }
            if (!Modifier.isPublic(mods)) {
                logger.log(Level.WARNING, "Modular assertion " + assclass.getName() + " declares a module load listener but its onModuleLoaded listenerMethod isn't public");
                return;
            }

            listenerMethod.invoke(null, getApplicationContext());

        } catch (ClassNotFoundException e) {
            logger.log(Level.WARNING, "Modular assertion " + assclass.getName() + " declares a module load listener but the class isn't found: " + ExceptionUtils.getMessage(e));
        } catch (NoSuchMethodException e) {
            logger.log(Level.WARNING, "Modular assertion " + assclass.getName() + " declares a module load listener but the class doesn't have a public static method onModuleLoaded(ApplicationContext): " + ExceptionUtils.getMessage(e));
        } catch (InvocationTargetException e) {
            logger.log(Level.WARNING, "Modular assertion " + assclass.getName() + " declares a module load listener but it could not be invoked: " + ExceptionUtils.getMessage(e), e);
        } catch (IllegalAccessException e) {
            logger.log(Level.WARNING, "Modular assertion " + assclass.getName() + " declares a module load listener but it could not be invoked: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void onModuleUnloaded(AssertionModule module) {
        Background.cancelAllTasksFromClassLoader(module.getModuleClassLoader());
        publishEvent(new AssertionModuleUnregistrationEvent(this, module));
        try {
            module.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error while closing module " + module.getName() + ": " + ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * Find the assertion module, if any, that owns the specified class loader.
     *
     * @param classLoader the class loader to check.  Any code that suspects it may be running as a modular
     *                    assertion can just pass as this argument the result of <tt>getClass().getClassLoader()</tt>.
     * @return the {@link AssertionModule} that provides this class loader, or null if no currently registered AssertionModule owns
     *         the specified ClassLoader.
     */
    public AssertionModule getModuleForClassLoader(ClassLoader classLoader) {
        for (AssertionModule module : loadedModules.values())
            if (classLoader == module.getModuleClassLoader())
                return module;
        return null;
    }

    /**
     * @param file the file to digest.  Must not be null.
     * @return the hex dump of the SHA-1 digest of the specified file.
     * @throws java.io.IOException if there is a problem reading the file
     */
    static String getFileSha1(File file) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            byte[] buf = new byte[8192];
            int got;
            while ((got = fis.read(buf)) > 0)
                digest.update(buf, 0, got);
            return HexUtils.hexDump(digest.digest());

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // can't happen, misconfigured VM
        } finally {
            ResourceUtils.closeQuietly(fis);
        }
    }

    /**
     * @return a view of all assertion modules which are currently loaded.  May be empty but never null.
     */
    public synchronized Set<AssertionModule> getLoadedModules() {
        return new HashSet<AssertionModule>(loadedModules.values());
    }

    /**
     * Get info about the specified module, if it is loaded.
     *
     * @param moduleFilename  the module name to check.  Required.
     * @return the AssertionModule instance describing this module if it is loaded, or null if no such module is loaded.
     */
    public synchronized AssertionModule getModule(String moduleFilename) {
        return loadedModules.get(moduleFilename);
    }

    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        scanModularAssertions();
        checkForNewClusterProperties();
        long rescanMillis = serverConfig.getLongProperty(ServerConfig.PARAM_MODULAR_ASSERTIONS_RESCAN_MILLIS, 4523);
        Background.scheduleRepeated(new TimerTask() {
            public void run() {
                scanModularAssertions();
                checkForNewClusterProperties();
            }
        }, rescanMillis, rescanMillis);
    }

    private static void installGatewayMetadataDefaults() {
        if (gatewayMetadataDefaultsInstalled.get())
            return;

        DefaultAssertionMetadata.putDefaultGetter(AssertionMetadata.CLUSTER_PROPERTIES, new MetadataFinder() {
            public Object get(AssertionMetadata meta, String key) {
                return DefaultAssertionMetadata.cache(meta, key, new HashMap());
            }
        });

        gatewayMetadataDefaultsInstalled.set(true);
    }

    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        super.onApplicationEvent(applicationEvent);

        if (applicationEvent instanceof LicenseEvent) {
            // License has changed.  Ensure that module rescan occurs.
            synchronized (this) {
                lastScannedDirModTime = 0;
                failModTimes.clear(); // retry all failures whenever the license changes
                scanNeeded = true;
            }
        }
    }
}
