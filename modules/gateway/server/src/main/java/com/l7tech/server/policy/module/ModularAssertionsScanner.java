package com.l7tech.server.policy.module;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Extend abstract {@link com.l7tech.server.policy.module.ModulesScanner} and implement modular assertions specific logic.
 */
public class ModularAssertionsScanner extends ScheduledModuleScanner<ModularAssertionModule> {
    protected static final Logger logger = Logger.getLogger(ScheduledModuleScanner.class.getName());

    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final boolean useApplicationClasspath = ConfigFactory.getBooleanProperty("ssg.modularAssertions.useApplicationClasspath", false);

    // modules scanner config
    private final ModularAssertionModulesConfig modulesConfig;

    // Quick lookup of module by a (non-empty) package it offers.  In case of dupes, last module loaded wins!
    private final Map<String, ModularAssertionModule> modulesByPackageName = new ConcurrentHashMap<>();

    // flag forcing scan on next run
    private boolean scanNeeded = true;

    // callbacks
    private final ScannerCallbacks.ModularAssertion callbacks;

    public ModularAssertionsScanner(@NotNull final ModularAssertionModulesConfig modulesConfig, @NotNull final ScannerCallbacks.ModularAssertion callbacks) {
        this.modulesConfig = modulesConfig;
        this.callbacks = callbacks;
    }

    private static boolean classExists(ClassLoader classLoader, String classname) {
        try {
            return classLoader.loadClass(classname) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Find the most-recently-loaded loaded module that contains at least one class or resource in the specified package.
     *
     * @param packageName a package name.  Required
     * @return the most-recently-loaded loaded module that offers at least one file in this package, or null if there isn't one.
     */
    public ModularAssertionModule getModuleForPackage(@NotNull String packageName) {
        return modulesByPackageName.get(packageName);
    }

    /**
     * Notify the specified assertion's module loader listener that its module has been loaded, if it
     * declares an appropriate listener classname.
     *
     * @param assclass  the assertion class.  Required
     * @param classname  the name of the listener class to invoke.  Required.
     */
    private void onModuleLoaded(Class<? extends Assertion> assclass, String classname) {
        if (classname == null || classname.length() < 1) return;
        try {
            Class listenerClass = assclass.getClassLoader().loadClass(classname);
            //noinspection unchecked
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

            listenerMethod.invoke(null, callbacks.getApplicationContext());

        } catch (ClassNotFoundException e) {
            logger.log(Level.WARNING, "Modular assertion " + assclass.getName() + " declares a module load listener but the class isn't found: " + ExceptionUtils.getMessage(e));
        } catch (NoSuchMethodException e) {
            logger.log(Level.WARNING, "Modular assertion " + assclass.getName() + " declares a module load listener but the class doesn't have a public static method onModuleLoaded(ApplicationContext): " + ExceptionUtils.getMessage(e));
        } catch (InvocationTargetException | IllegalAccessException e) {
            logger.log(Level.WARNING, "Modular assertion " + assclass.getName() + " declares a module load listener but it could not be invoked: " + ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * The module has been unloaded
     * @param module    the module that was just unloaded.
     */
    private void onModuleUnloaded(ModularAssertionModule module) {
        Background.cancelAllTasksFromClassLoader(module.getModuleClassLoader());
        callbacks.publishEvent(new AssertionModuleUnregistrationEvent(this, module));
        try {
            module.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error while closing module " + module.getName() + ": " + ExceptionUtils.getMessage(e), e);
        }
    }

    private static boolean isPrivateLib(Pattern[] privateLibPatterns, String name) {
        for (Pattern pattern : privateLibPatterns)
            if (pattern.matcher(name).matches())
                return true;
        return false;
    }

    private static Pattern[] getPrivateLibPatterns(String privateLibsStr) {
        String[] patStrs = privateLibsStr == null ? EMPTY_STRING_ARRAY : privateLibsStr.split("\\s+");
        Pattern[] pats = new Pattern[patStrs.length];
        for (int i = 0; i < patStrs.length; i++)
            pats[i] = Pattern.compile("^(?:.*/)?" + TextUtils.globToRegex(patStrs[i]) + '$', Pattern.CASE_INSENSITIVE);
        return pats;
    }

    /**
     * Attempt to register one or more assertions from the specified modular assertion aar-file, which must
     * contain metadata declaring at least one modular assertion.
     * <p/>
     * Fires an AssertionModuleRegistrationEvent if the module is registered successfully.<br/>
     * By each successful module load, skipped modules container is reset via {@link #clearFailedModTimes()}.
     *
     * @see ModulesScanner#onModuleLoad(java.io.File, String, long)
     */
    @NotNull
    @Override
    protected ModuleLoadStatus<ModularAssertionModule> onModuleLoad(final File file, final String sha1, long lastModified) throws ModuleException {
        // sanity check
        if (file == null)
            throw new ModuleException("null module File supplied", new NullPointerException());

        // create module load status with default values to indicate no-changes
        final ModuleLoadStatus<ModularAssertionModule> retValue = new ModuleLoadStatus<>();

        final String filename = file.getName();
        ModularAssertionModule previousVersion = getModule(filename);

        try {
            JarFile jar = new JarFile(file, false);

            Manifest manifest = jar.getManifest();
            Attributes attr = manifest.getMainAttributes();
            String assertionNamesStr = attr.getValue(modulesConfig.getManifestHdrAssertionList());
            String[] assertionClassnames = assertionNamesStr == null ? EMPTY_STRING_ARRAY : assertionNamesStr.split("\\s+");
            if (assertionClassnames.length < 1) {
                logger.log(Level.WARNING, "Modular assertionNames jarfile contains no modular assertions (ignoring it) " + file.getAbsolutePath());
                // skip it
                retValue.setLoadedModule(null);
                return retValue;
            }

            String privateLibsStr = attr.getValue(modulesConfig.getManifestHdrPrivateLibraries());
            Pattern[] privateLibPatterns = getPrivateLibPatterns(privateLibsStr);

            // Save set of exported packages so we can quickly trace future classlaoder queries to the correct module
            Set<String> packages = new HashSet<>();
            Set<NestedZipFile> nestedJarfiles = new HashSet<>();
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.startsWith("AAR-INF/lib/") && name.toLowerCase().endsWith(".jar")) {
                    // Preload nested jar file, and record the packages it offers
                    logger.info("Preloading nested JAR file " + name + " from module " + filename);
                    boolean priv = isPrivateLib(privateLibPatterns, name);
                    if (priv) logger.fine("Marking nested jarfile " + name + " as private");
                    NestedZipFile nestedZipFile = new NestedZipFile(jar, name, priv);
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

            //noinspection ClassLoaderInstantiation
            ModularAssertionClassLoader assloader = new ModularAssertionClassLoader(filename, file.toURI().toURL(), getClass().getClassLoader(), nestedJarfiles, useApplicationClasspath);
            Set<Assertion> protos = new HashSet<>();
            for (String assertionClassname : assertionClassnames) {
                if (!useApplicationClasspath && classExists(getClass().getClassLoader(), assertionClassname))
                    throw new InstantiationException("Declared class already exists in parent classloader: " + assertionClassname);
                Class assclass = assloader.loadClass(assertionClassname);
                if (!Assertion.class.isAssignableFrom(assclass))
                    throw new ClassCastException("Declared assertion class not assignable to Assertion: " + assclass.getName());
                if (previousVersion != null) {
                    // Class must either exist in previous version, or not at all (being new to this version)
                    // Otherwise, it will conflict with an assertion class installed by some other module
                    if (!previousVersion.offersClass(assertionClassname) && callbacks.isAssertionRegistered(assertionClassname))
                        throw new InstantiationException("Declared assertion class is duplicate of assertion from another module: " + assclass.getName());

                } else {
                    // Class must not exist in any other module
                    if (callbacks.isAssertionRegistered(assertionClassname))
                        throw new InstantiationException("Declared assertion class is duplicate of assertion from another module: " + assclass.getName());
                }
                Assertion proto = (Assertion)assclass.newInstance();
                protos.add(proto);
            }

            final ModularAssertionModule module = new ModularAssertionModule(filename, jar, lastModified, sha1, assloader, protos, packages);
            previousVersion = insertModule(module);
            for (String p : packages) {
                modulesByPackageName.put(p, module);
            }
            clearFailedModTimes();

            // set the return variable accordingly, to indicate whether a previous version of the module was unloaded and which module was loaded.
            retValue.setLoadedModule(module);
            retValue.setPrevModuleUnloaded(previousVersion != null);

            try {
                // Register any new cluster properties, in case the assertion initialization requires them
                callbacks.registerClusterProps(protos);

                // Set up class loader delegates first, in case any of the initialization listeners needs them in place
                for (Assertion proto : protos) {
                    ClassLoader dcl = proto.meta().get(AssertionMetadata.MODULE_CLASS_LOADER_DELEGATE_INSTANCE);
                    if (dcl != null)
                        assloader.addDelegate(dcl);
                }

                // Notify any module load listeners declared by any of this module's assertions
                for (Assertion proto : protos) {
                    String listenerClassname = proto.meta().get(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME);
                    if (listenerClassname != null && listenerClassname.length() > 0) {
                        logger.info("Initializing dynamic module " + filename);
                        onModuleLoaded(proto.getClass(), listenerClassname);
                    }
                }

                for (Assertion proto : protos) {
                    String adjective = previousVersion == null ? "newly-registered" : "just-upgraded";
                    logger.info("Registering dynamic assertion " + proto.getClass().getName() + " from " + adjective + " module " + filename + " (module SHA-1 " + sha1 + ')');
                    callbacks.registerAssertion(proto.getClass());
                }
            } finally {
                if (previousVersion != null)
                    onModuleUnloaded(previousVersion);
            }

            callbacks.publishEvent(new AssertionModuleRegistrationEvent(this, module));

            return retValue;
        } catch (IOException e) {
            throw new ModuleException("Unable to read module: " + ExceptionUtils.getMessage(e), e);
        } catch (ClassNotFoundException e) {
            throw new ModuleException("Unable to find declared assertion: " + ExceptionUtils.getMessage(e), e);
        } catch (ClassCastException e) {
            throw new ModuleException("Declared class was not an Assertion: " + ExceptionUtils.getMessage(e), e);
        } catch (IllegalAccessException | InstantiationException e) {
            throw new ModuleException("Unable to instantiate modular assertion: " + ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * Unregister a module, perhaps because we noticed that its file has been deleted.
     * <p/>
     * Fires an AssertionModuleUnregistrationEvent if the module is un-registered successfully.
     *
     * @see com.l7tech.server.policy.module.ModulesScanner#onModuleUnload(com.l7tech.server.policy.module.BaseAssertionModule)
     */
    @Override
    protected boolean onModuleUnload(final ModularAssertionModule module) throws ModuleException {
        // sanity check
        if (module == null)
            throw new ModuleException("module cannot be null", new NullPointerException());

        // remove module from loaded/scanned hash-map
        if (removeModule(module) == null) {
            // module is not loaded, therefore do not send unload notifications.
            return false;
        }

        modulesByPackageName.values().remove(module);
        clearFailedModTimes(); // retry all failures whenever a module is loaded or unloaded

        // Unregister all assertions from this module.
        final Set<? extends Assertion> prototypes = module.getAssertionPrototypes();
        for (Assertion prototype : prototypes) {
            logger.info("Unregistering dynamic assertion " + prototype.getClass().getName() + " from just-unregistered module " + module.getName());
            callbacks.unregisterAssertion(prototype);
        }

        onModuleUnloaded(module);

        return true;
    }

    @Override
    protected boolean isScanNeeded(File dir, long dirLastModified) {
        return isScanNeeded() || super.isScanNeeded(dir, dirLastModified);
    }

    @Override
    public synchronized void destroy() {
        setScanNeeded(false);
        super.destroy();
    }

    @Override
    protected void onScanComplete() {
        // reset force scan flag
        setScanNeeded(false);
    }

    /**
     * Convenient method for calling the base {@link com.l7tech.server.policy.module.ScheduledModuleScanner#scanModules scanModules} with a cached config object.
     */
    public boolean scanModules() {
        return super.scanModules(modulesConfig);
    }

    /**
     * Convenient method for extracting scanned modules.
     */
    public Collection<ModularAssertionModule> getModules() {
        return scannedModules.values();
    }

    /**
     * On license change reset the scanner for force a scan next run.
     */
    public synchronized void onLicenseChange() {
        reset();
        clearFailedModTimes(); // retry all failures whenever the license changes
        setScanNeeded(true);
    }

    public boolean isScanNeeded() {
        return scanNeeded;
    }

    public void setScanNeeded(boolean scanNeeded) {
        this.scanNeeded = scanNeeded;
    }
}
