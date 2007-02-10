package com.l7tech.server.policy;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.Background;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.MetadataFinder;
import com.l7tech.server.ServerConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Gateway's AssertionRegistry, which extends the default registry with the ability to look for
 * modular ServerConfig properties in the new assertions and register them with ServerConfig.
 */
public class ServerAssertionRegistry extends AssertionRegistry {
    protected static final Logger logger = Logger.getLogger(ServerAssertionRegistry.class.getName());
    private static final long MODULE_RESCAN_DELAY = Long.getLong("com.l7tech.server.assertionModuleDirRescanMillis", 4523L);

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

    private final ServerConfig serverConfig;
    private final Map<String, AssertionModule> loadedModules = new HashMap<String, AssertionModule>();
    private final Map<String, Long> failModTimes = new HashMap<String, Long>();    // should not be loaded (until mod time changes) because last time we tried it, it failed
    private final Map<String, Long> removedModTimes = new HashMap<String, Long>(); // should not be loaded (until mod time changes) because they were manually unregistered
    private Map<String, String[]> newClusterProps; // do not initialize -- clobbers info collected during super c'tor
    private File lastScannedDir = null;
    private long lastScannedDirModTime = 0;

    public ServerAssertionRegistry(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        installGatewayMetadataDefaults();
    }

    public synchronized Assertion registerAssertion(Class<? extends Assertion> assertionClass) {
        Assertion prototype = super.registerAssertion(assertionClass);

        // Check if the new assertion requires any new serverConfig properties.
        AssertionMetadata meta = prototype.meta();
        //noinspection unchecked
        Map<String, String[]> newProps = (Map<String, String[]>)meta.get(AssertionMetadata.CLUSTER_PROPERTIES);
        if (newProps != null) {
            // We may be called during superclass c'tor, so may need to initialize our own field here
            if (newClusterProps == null) newClusterProps = new ConcurrentHashMap<String, String[]>();

            for (Map.Entry<String, String[]> entry : newProps.entrySet()) {
                final String name = entry.getKey();
                final String[] tuple = entry.getValue();
                String desc = tuple != null && tuple.length > 0 ? tuple[0] : null;
                String dflt = tuple != null && tuple.length > 1 ? tuple[1] : null;
                newClusterProps.put(name, new String[] { desc, dflt });
            }
        }

        return prototype;
    }

    private static final Pattern findMidDots = Pattern.compile("\\.([a-zA-Z0-9_])");

    /**
     * Converts a cluster property name like "foo.bar.blatzBloof.bargleFoomp" into a ServerConfig property
     * root like "fooBarBlatzBlofBargleFoomp".
     *
     * @param clusterPropertyName the cluster property name to convert
     * @return the corresponding serverConfig property name.  Never null.
     */
    private static String makeServerConfigPropertyName(String clusterPropertyName) {
        Matcher matcher = findMidDots.matcher(clusterPropertyName);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
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
                String serverConfigName = makeServerConfigPropertyName(clusterPropertyName);

                toAdd.add(new String[] { serverConfigName, clusterPropertyName, desc, dflt });
                logger.info("Dynamically registering cluster property " + clusterPropertyName);
            }
        }
        if (!toAdd.isEmpty()) serverConfig.registerServerConfigProperties(toAdd.toArray(new String[][] {}));
    }

    private synchronized boolean scanModularAssertions() {
        boolean changesMade = false;

        File dir = serverConfig.getLocalDirectoryProperty(ServerConfig.PARAM_MODULAR_ASSERTIONS_DIRECTORY, "/ssg/modules/assertions", false).getAbsoluteFile();
        long dirLastModified = dir.lastModified();
        if (dir.equals(lastScannedDir) && dirLastModified == lastScannedDirModTime && failModTimes.isEmpty()) {
            // No files added/removed since last scan, and no failures to retry
            return false;
        }

        logger.info("Scanning assertion modules directory " + dir.getAbsolutePath() + "...");

        lastScannedDir = dir;
        lastScannedDirModTime = dirLastModified;

        File[] jars = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jar") || name.toLowerCase().endsWith(".jar.disable");
            }
        });

        // Check for new or changed modules
        Set<String> seenNames = new HashSet<String>();
        for (File file : jars) {
            String filename = file.getName();

            if (filename.toLowerCase().endsWith(".jar.disable")) {
                // Admin wants to disable this module
                String name = filename.substring(0, filename.length() - 8);
                if (loadedModules.containsKey(name)) {
                    logger.info("Unregistering module " + name + " because the flag file " + filename + "exists");
                    unregisterModule(name);
                }
                continue;
            }

            long lastModified = file.lastModified();
            try {
                seenNames.add(filename);

                AssertionModule previousVersion = loadedModules.get(filename);
                if (previousVersion != null) {
                    if (previousVersion.getJarfileModifiedTime() == lastModified)
                        continue;

                    // A loaded module has changed since the last time we looked at it -- unload it and reload it
                    logger.info("Reloading changed assertion module: " + filename);

                } else if (new Long(lastModified).equals(failModTimes.get(filename))) {
                    if (logger.isLoggable(Level.FINE))
                    logger.fine("Ignoring module file " + filename + " since its modification time hasn't changed since the last time it failed to load successfully");
                    continue;
                } else if (new Long(lastModified).equals(removedModTimes.get(filename))) {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("Ignoring module file " + filename + " since its modification time hasn't changed since it was manually unregistered");
                    continue;
                }

                changesMade = true;
                registerModule(file);

            } catch (ModuleException e) {
                logger.log(Level.SEVERE, "Unable to load modular assertion jarfile (ignoring it until it changes) " + filename + ": " + ExceptionUtils.getMessage(e), e);
                failModTimes.put(filename, lastModified);
                removedModTimes.remove(filename);
            }
        }

        // Check for removed modules
        for (AssertionModule module : loadedModules.values()) {
            String name = module.getName();
            if (!seenNames.contains(name)) {
                logger.info("Unregistering assertion module that has been removed: " + name);
                changesMade = true;
                unregisterModule(name);
            }
        }

        // check for removed failed modules
        for (String failedName : failModTimes.keySet()) {
            if (!seenNames.contains(failedName)) {
                logger.info("Forgetting about failed module that has been removed: " + failedName);
                failModTimes.remove(failedName);
            }
        }

        return changesMade;
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

        JarFile jar = null;
        try {
            long modifiedTime = file.lastModified();
            String sha1 = getFileSha1(file);
            // TODO is there any way to fix the race condition here (if file is replaced in between these two lines)?
            jar = new JarFile(file, false);

            Manifest manifest = jar.getManifest();
            Attributes attr = manifest.getMainAttributes();
            String assertionNamesStr = attr.getValue("ModularAssertion-List");
            String[] assertionNames = assertionNamesStr == null ? new String[0] : assertionNamesStr.split("\\s+");
            if (assertionNames.length < 1) {
                logger.log(Level.WARNING, "Modular assertionNames jarfile contains no modular assertions (ignoring it) " + file.getAbsolutePath());
                return;
            }
            jar.close();
            jar = null;

            ClassLoader assloader = new URLClassLoader(new URL[] { file.toURL() }, getClass().getClassLoader());
            Set<Assertion> protos = new HashSet<Assertion>();
            for (String assertionName : assertionNames) {
                String assertionClassname = attr.getValue(assertionName + "-Class");
                if (assertionClassname == null)
                    throw new ClassNotFoundException("Couldn't find attribute: " + assertionName + "-Class");
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

            AssertionModule module = new AssertionModule(file, modifiedTime, sha1, protos);
            loadedModules.put(filename, module);
            failModTimes.remove(filename);
            removedModTimes.remove(filename);
            if (previousVersion != null) publishEvent(new AssertionModuleUnregistrationEvent(this, previousVersion));
            for (Assertion proto : protos) {
                logger.info("Registering dynamic assertion " + proto.getClass().getName() + " from newly-registered module " + filename + " (module SHA-1 " + sha1 + ")");
                registerAssertion(proto.getClass());
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
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Unable to close jarfile", e);
                }
            }
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
        failModTimes.remove(name);
        removedModTimes.put(name, module.getJarfileModifiedTime());

        publishEvent(new AssertionModuleUnregistrationEvent(this, module));

        // Unregister all assertions from this module
        Set<? extends Assertion> protos = module.getAssertionPrototypes();
        for (Assertion proto : protos) {
            logger.info("Unregistering dynamic assertion " + proto.getClass().getName() + " from just-unregistered module " + name);
            unregisterAssertion(proto);
        }

        return true;
    }

    /**
     * @param file the file to digest.  Must not be null.
     * @return the hex dump of the SHA-1 digest of the specified file.
     * @throws java.io.IOException if there is a problem reading the file
     */
    private String getFileSha1(File file) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            byte[] buf = new byte[8192];
            int got;
            while ((got = fis.read()) > 0)
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

    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        scanModularAssertions();
        checkForNewClusterProperties();
        Background.scheduleRepeated(new TimerTask() {
            public void run() {
                scanModularAssertions();
                checkForNewClusterProperties();
            }
        }, MODULE_RESCAN_DELAY, MODULE_RESCAN_DELAY);
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
}
