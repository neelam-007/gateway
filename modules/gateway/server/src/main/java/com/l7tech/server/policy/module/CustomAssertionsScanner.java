package com.l7tech.server.policy.module;

import com.l7tech.gateway.common.custom.CustomAssertionDescriptor;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.util.Background;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extend abstract {@link com.l7tech.server.policy.module.ModulesScanner} and implement custom assertions specific logic.
 */
public class CustomAssertionsScanner extends ScheduledModuleScanner<CustomAssertionModule> {

    private static final Logger logger = Logger.getLogger(CustomAssertionsScanner.class.getName());

    // modules scanner config
    private final CustomAssertionModulesConfig modulesConfig;

    // for convenience there are two versions of the property file name
    // one without the leading path separator, the other without
    private String caPropFileNS;
    private String caPropFileS;

    // flag indicating SSG shutdown, actually it's when the bean is destroyed.
    private boolean isShuttingDown = false;

    // Acts as a cache for all modules (module names) being loaded since SSG startup.
    // Note that this is ever-growing list, therefore potentially it can consume a lot of memory,
    // though in order to become noticeable on memory imprint, hundreds of thousands perhaps millions of
    // different modules need to be loaded at some point.
    // This is highly unlikely to happen (at this point we do not have that many different modules),
    // but in case it becomes an issue then, the set below should be redesign to use RAM as well as own file,
    // similar to HybridStashManager.
    private final Set<String> loadedModulesCache = new HashSet<>();

    /**
     * Indicates whether this is the initial scan or not (<code>true</code> by default).<br/>
     * Custom assertions, not implementing {@link com.l7tech.policy.assertion.ext.CustomDynamicLoader CustomDynamicLoader} interface,
     * can only be loaded during SSG start i.e. the initial scan, and cannot be loaded or reloaded during any consecutive scans.<br/>
     * This flag is used to distinguish between initial and consecutive scans.
     * <p/>
     * This field is private and cannot be modified outside from this class.<br/>
     * The field is initially set to <code>true</code> and will be automatically set to <code>false</code> after a scan is finished.
     */
    private boolean isInitialScan = true;

    // callbacks object
    private final ScannerCallbacks.CustomAssertion callbacks;

    /**
     * CustomAssertionsScanner default constructor
     *
     * @param modulesConfig    object containing all config values.
     * @param callbacks        object containing all custom assertion callbacks.
     */
    public CustomAssertionsScanner(@NotNull final CustomAssertionModulesConfig modulesConfig, @NotNull final ScannerCallbacks.CustomAssertion callbacks) {

        this.modulesConfig = modulesConfig;
        this.callbacks = callbacks;

        // for convenience create two versions of the property file name
        // one without the leading path separator, the other without
        final String caPropFileName = modulesConfig.getCustomAssertionPropertyFileName();
        caPropFileNS = caPropFileName;
        caPropFileS = caPropFileName;
        if (caPropFileName != null) {
            if (caPropFileName.startsWith("/")) {
                caPropFileNS = caPropFileName.substring(1);
            } else {
                caPropFileS = "/" + caPropFileName;
            }
        }
    }

    /**
     * Creates a <code>ClassLoader</code> for the specified module jar file.<br/>
     * This will:
     * <ul>
     *   <li>Extract libs for each module to the working directory</li>
     *   <li>Create a {@link com.l7tech.server.policy.module.CustomAssertionClassLoader}, which in essence is an {@link java.net.URLClassLoader URLClassLoader}
     *   with the original jar and all the lib jars</li>
     * </ul>
     *
     * @param moduleJar        the module jar file.
     * @param parent           the parent {@link ClassLoader}.
     * @param workDirectory    the custom assertions working directory for expanded modules.
     * @param id               the identifier for this module loader.
     * @throws com.l7tech.server.policy.module.ModuleException if an error occur during <code>ClassLoader</code> creation.
     * @return if the module file is a genuine custom assertion jar, meaning the jar contains at least one assertion
     * resource properties file, then an instance of {@link com.l7tech.server.policy.module.CustomAssertionClassLoader}, <code>null</code> otherwise.
     */
    private CustomAssertionClassLoader buildModuleClassLoader(@NotNull final File moduleJar,
                                                              @NotNull final ClassLoader parent,
                                                              @NotNull final File workDirectory,
                                                              @NotNull final String id) throws ModuleException {
        final File moduleLibDirectory = new File(workDirectory, modulesConfig.getExpandedModuleDirPrefix() + id + "-" + moduleJar.getName());
        moduleLibDirectory.deleteOnExit();

        // ensure any old expanded modules are cleaned
        if (moduleLibDirectory.exists() && moduleLibDirectory.isDirectory()) {
            final File[] libs = moduleLibDirectory.listFiles();
            if (libs != null) {
                for (final File lib : libs) {
                    //noinspection ResultOfMethodCallIgnored
                    lib.delete();
                }
            }
            //noinspection ResultOfMethodCallIgnored
            moduleLibDirectory.delete();
        }

        boolean resourceFound = caPropFileNS == null || caPropFileS == null;

        final List<File> moduleJarFiles = new ArrayList<>();
        moduleJarFiles.add(moduleJar);

        JarFile jarFile = null;
        try {
            // may throw IOException if there is a problem reading the content of the file
            jarFile = new JarFile(moduleJar);
            if (!resourceFound) {
                // check if we should use this module
                final Enumeration<JarEntry> jarEntries = jarFile.entries();
                // loop through all files inside the jar, and look for the custom assertion properties file
                while (jarEntries.hasMoreElements()) {
                    final JarEntry entry = jarEntries.nextElement();
                    if (entry.getName().startsWith(caPropFileNS) || entry.getName().startsWith(caPropFileS)) {
                        resourceFound = true;
                        break;
                    }
                }
            }

            if (resourceFound) {
                final Enumeration<JarEntry> jarEntries = jarFile.entries();
                while (jarEntries.hasMoreElements()) {
                    final JarEntry entry = jarEntries.nextElement();
                    // skip if null or directory
                    if (entry == null || entry.isDirectory()) {
                        continue;
                    }

                    final String entryName = entry.getName();
                    if (entryName.startsWith(modulesConfig.getLibPrefix()) && modulesConfig.isSupportedLibrary(entryName.toLowerCase()))
                    {
                        if (!moduleLibDirectory.exists()) {
                            if (!moduleLibDirectory.mkdirs()) {
                                throw new ModuleException("Could not create module temp directory '" + moduleLibDirectory.getAbsolutePath() + "'.");
                            }
                        }

                        final File moduleLib = new File(moduleLibDirectory, entryName.substring(modulesConfig.getLibPrefix().length()));
                        if (!moduleLib.getParentFile().equals(moduleLibDirectory)) {
                            logger.info("Skipping lib in subdirectory '" + entryName + "'.");
                            continue;
                        }

                        moduleLib.deleteOnExit();
                        moduleJarFiles.add(moduleLib);

                        InputStream entryIn = null;
                        OutputStream entryOut = null;
                        try {
                            entryIn = jarFile.getInputStream(entry);
                            entryOut = new BufferedOutputStream(new FileOutputStream(moduleLib));
                            IOUtils.copyStream(entryIn, entryOut);
                            entryOut.flush();
                        } catch (IOException ioe) {
                            throw new ModuleException("Could not expand module lib '" + entryName + "' to temp directory '" + moduleLibDirectory.getAbsolutePath() + "'.", ioe);
                        } finally {
                            ResourceUtils.closeQuietly(entryIn);
                            ResourceUtils.closeQuietly(entryOut);
                        }
                    }
                }
            }
        } catch (IOException ioe) {
            throw new ModuleException("Could not load module jar '" + moduleJar.getAbsolutePath() + "'.", ioe);
        } finally {
            if (jarFile != null) {
                try{
                    jarFile.close();
                } catch(IOException ioe) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Error closing JarFile '" + moduleJar.getAbsolutePath() + "'.", ioe);
                    }
                }
            }
        }

        return !resourceFound ? null : createClassLoader(parent, moduleJarFiles);
    }

    /**
     * Create custom assertion ClassLoader based on the list of jar files.<br/>
     * In general the class loader created here is actually a {@link java.net.URLClassLoader URLClassLoader},
     * where {@link com.l7tech.server.policy.module.CustomAssertionClassLoader} wraps around it.
     *
     * @param parent      the parent ClassLoader.
     * @param jarFiles    a list of jar files to be accessed by the ClassLoader.
     * @throws com.l7tech.server.policy.module.ModuleException if an error occur during <code>ClassLoader</code> creation.
     * @return on success an instance of {@link com.l7tech.server.policy.module.CustomAssertionClassLoader} or <code>null</code> on failure.
     */
    private CustomAssertionClassLoader createClassLoader(final ClassLoader parent, final List<File> jarFiles) throws ModuleException {
        CustomAssertionClassLoader loader;
        try {
            final List<URL> urlList = new ArrayList<>();
            for (final File jarFile : jarFiles) {
                urlList.add(jarFile.toURI().toURL());
            }

            final URL[] urls = urlList.toArray(new URL[urlList.size()]);
            if (parent == null) {
                loader = new CustomAssertionClassLoader(urls);
            } else {
                loader = new CustomAssertionClassLoader(urls, parent);
            }
        } catch(MalformedURLException e) {
            throw new ModuleException("Could not create custom assertion module class loader", e);
        }
        return loader;
    }

    /**
     * Create all custom assertion descriptors from the specified module file and it's <code>ClassLoader</code>.
     *
     * @param moduleFile     the module file.
     * @param classLoader    the module <code>ClassLoader</code>
     * @return a set of {@link com.l7tech.gateway.common.custom.CustomAssertionDescriptor} associated with the specified <tt>moduleFile</tt> and <tt>classLoader</tt>.
     * Could be empty but never never <code>null</code>.
     * @throws com.l7tech.server.policy.module.ModuleException
     */
    private Set<CustomAssertionDescriptor> buildDescriptors(@NotNull final File moduleFile, @NotNull final ClassLoader classLoader) throws ModuleException {
        final Set<CustomAssertionDescriptor> descriptors = new HashSet<>();
        InputStream in = null;
        try {
            final Enumeration propFileUrls = classLoader.getResources(modulesConfig.getCustomAssertionPropertyFileName());
            if (propFileUrls.hasMoreElements()) {
                while(propFileUrls.hasMoreElements()) {
                    final URL resourceUrl = (URL)propFileUrls.nextElement();

                    // open an input stream for reading the custom assertion properties file content.
                    in = resourceUrl.openStream();

                    // load all properties
                    final Properties props = new Properties();
                    props.load(in);

                    // get the thread context class loader
                    final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                    try {
                        // remove any previous thread context class loader
                        if (contextClassLoader != null) {
                            Thread.currentThread().setContextClassLoader(null);
                        }

                        // loop through all properties
                        for (final Object prop : props.keySet()) {
                            final String key = prop.toString();
                            if (key.endsWith(".class")) {
                                try {
                                    descriptors.add(createDescriptor(
                                            key.substring(0, key.indexOf(".class")),
                                            props,
                                            classLoader,
                                            moduleFile.getName()
                                    ));
                                } catch (ModuleException e) {
                                    logger.log(Level.WARNING, "Creating custom assertion descriptor failed:", e);
                                }
                            }
                        }
                    } finally {
                        if (contextClassLoader != null) {
                            Thread.currentThread().setContextClassLoader(contextClassLoader);
                        }
                    }
                }
            } else {
                logger.info("No custom assertions found.");
            }
        } catch (FileNotFoundException e) {
            throw new ModuleException("Module \"" + moduleFile.getName() + "\", doesn't contain custom assertions config file \"" + modulesConfig.getCustomAssertionPropertyFileName() + "\". Custom assertion not loaded", e);
        } catch (IOException e) {
            throw new ModuleException("Module \"" + moduleFile.getName() + "\" I/O error reading config file \"" + modulesConfig.getCustomAssertionPropertyFileName() + "\". Custom assertion not loaded", e);
        } finally {
            ResourceUtils.closeQuietly(in);
        }

        return Collections.unmodifiableSet(descriptors);
    }

    /**
     * Create {@link com.l7tech.gateway.common.custom.CustomAssertionDescriptor} object with the specified parameters.
     *
     * @param baseKey           custom assertion name as specified in the properties file.
     * @param properties        custom assertion properties container.
     * @param classLoader       custom assertion <code>ClassLoader</code>.
     * @param moduleFileName    custom assertion module filename.
     * @return an {@link com.l7tech.gateway.common.custom.CustomAssertionDescriptor} object containing the custom assertion runtime information.  Never <code>null</code>.
     * @throws com.l7tech.server.policy.module.ModuleException if an error happens while creating the descriptor
     * @see com.l7tech.gateway.common.custom.CustomAssertionDescriptor
     */
    private CustomAssertionDescriptor createDescriptor(final String baseKey,
                                                       final Properties properties,
                                                       final ClassLoader classLoader,
                                                       final String moduleFileName) throws ModuleException {
        final String assertionClass = (String)properties.get(baseKey + ".class");
        final String serverClass = (String) properties.get(baseKey + ".server");

        if (serverClass == null || assertionClass == null) {
            final StringBuilder sb = new StringBuilder("Incomplete custom assertion, skipping\n");
            sb.append("[ assertion class=").append(assertionClass);
            sb.append(",server class=").append(serverClass).append("]");
            throw new ModuleException(sb.toString());
        }

        try {
            final Class a = Class.forName(assertionClass, true, classLoader);
            final Class sa = Class.forName(serverClass, true, classLoader);

            // extract categories
            final Set<Category> categories = Category.asCategorySet((String) properties.get(baseKey + ".category"));
            if (categories.isEmpty()) {
                categories.add(Category.UNFILLED); // it must have a category
            }

            final CustomAssertionDescriptor eh = new CustomAssertionDescriptor(baseKey, a, sa, categories);

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
            eh.setExtensionInterface((String) properties.get(baseKey + ".extension.interface"));
            eh.setModuleFileName(moduleFileName);

            return eh;
        } catch (ClassNotFoundException e) {
            StringBuilder sb = new StringBuilder("Cannot load class(es) for custom assertion, skipping...\n");
            sb.append("[ assertion class=").append(assertionClass);
            sb.append(",server class=").append(serverClass).append("]");
            throw new ModuleException(sb.toString(), e);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder("Invalid custom assertion, skipping...\n");
            sb.append("[ assertion class=").append(assertionClass);
            sb.append(",server class=").append(serverClass).append("]");
            throw new ModuleException(sb.toString(), e);
        }
    }

    /**
     * Helper class to close the custom assertion class loader if an error occurs.
     */
    private class CloseClassLoaderOnError {
        private CustomAssertionClassLoader classLoader;

        /**
         * Set the class loader.<br/>
         * Call when the class loader is created.
         * @param classLoader    instance of {@link com.l7tech.server.policy.module.CustomAssertionClassLoader CustomAssertionClassLoader}.
         */
        void set(final CustomAssertionClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        /**
         * Clears the associated custom assertion class loader.<br/>
         * Call when the module has been successfully loaded.
         */
        void clear() {
            this.classLoader = null;
        }

        /**
         * Will try to close the associated custom assertion class loader.<br/>
         * Call on function exit.
         */
        void close() {
            if (classLoader != null) {
                try {
                    classLoader.close();
                } catch (Throwable ex) {
                    logger.log(Level.WARNING, "Failed to close custom assertion class loader.", ex);
                }
            }
        }
    }

    /**
     * Attempt to register custom assertion from the specified jar-file, which must
     * contain custom_assertions.properties declaring at the custom assertion classes.
     * <p/>
     * Fires an AssertionModuleRegistrationEvent if the module is registered successfully.<br/>
     *
     * @see com.l7tech.server.policy.module.ModulesScanner#onModuleLoad(java.io.File, String, long)
     */
    @NotNull
    @Override
    protected ModuleLoadStatus<CustomAssertionModule> onModuleLoad(final File file, final String sha1, long lastModified) throws ModuleException {
        // sanity check
        if (file == null)
            throw new ModuleException("null module File supplied", new NullPointerException());

        // the left/key indicates whether a previous module was unloaded or not (Boolean)
        // right/value indicates whether the new module was loaded or not, it actually holds the newly loaded module (CustomAssertionModule).
        final ModuleLoadStatus<CustomAssertionModule> retValue = new ModuleLoadStatus<>();

        // unload any previous modules
        try {
            // get the previous version of this module, if any
            final CustomAssertionModule previousVersion = getModule(file.getName());
            if (previousVersion != null) {
                // unload the previous version
                // set the return variable accordingly, to indicate whether the previous module was unloaded successfully or not.
                retValue.setPrevModuleUnloaded(onModuleUnload(previousVersion));
            }
        } catch (Throwable e) {
            // log the error and continue loading the new one
            logger.log(Level.WARNING, "Failed to unload previous version of the module \"" + file.getName() + "\":", e);
        }

        // object which will close the URLClassLoader if error occurs.
        final CloseClassLoaderOnError classLoaderOnError = new CloseClassLoaderOnError();

        // load the new module
        try {
            // try to create custom assertion ClassLoader
            final CustomAssertionClassLoader classLoader = buildModuleClassLoader(
                    file,
                    AllCustomAssertionClassLoader.class.getClassLoader(),
                    new File(modulesConfig.getModuleWorkDirectory()),
                    modulesConfig.getExpandedModuleDirId()
            );
            // this probably means the jar file is not really a custom assertion jar
            if (classLoader == null) {
                logger.warning("Module \"" + file.getName() + "\" is missing \"" + modulesConfig.getCustomAssertionPropertyFileName() + "\" descriptor. The module will be reloaded once modified.");
                // skip it
                retValue.setLoadedModule(null);
                return retValue;
            }

            // set the class loader
            classLoaderOnError.set(classLoader);

            // try to create custom assertion descriptors
            final Set<CustomAssertionDescriptor> descriptors = buildDescriptors(file, classLoader);
                if (descriptors.isEmpty()) {
                    logger.warning("Module \"" + file.getName() + "\" doesn't contain any valid assertions. The module will be reloaded once modified.");
                    // skip it
                    retValue.setLoadedModule(null);
                    return retValue;
                }

            // create a new module.
            final CustomAssertionModule module = new CustomAssertionModule(
                    file.getName(),
                    lastModified,
                    sha1,
                    classLoader,
                    descriptors,
                    callbacks.getServiceFinder()
            );

            // all assertions are going to be loaded during initial scan, so if this is not the initial load then
            // check if we can load this module i.e. if all assertions inside the module are implementing CustomDynamicLoader interface.
            if (!isInitialScan && !module.isCustomDynamicLoader() && loadedModulesCache.contains(module.getName())) {
                logger.warning("Module \"" + module.getName() + "\" doesn't support dynamic loading and it was already loaded once. This version of the module will be loaded on next SSG start.");
                // skip it
                retValue.setLoadedModule(null);
                return retValue;
            }

            // load the module assertions
            module.onAssertionLoad();

            // insert our new or modified module
            insertModule(module);

            // mark module as loaded, add it to the loaded modules cache
            loadedModulesCache.add(module.getName());

            // set the return variable accordingly, to indicate which module was loaded.
            retValue.setLoadedModule(module);

            // retry all failures whenever a module is loaded or unloaded
            clearFailedModTimes();

            // now register all custom assertion descriptors for this module
            for (CustomAssertionDescriptor descriptor : descriptors) {
                try {
                    callbacks.registerAssertion(descriptor, classLoader);
                    logger.info("Registered custom assertion " + descriptor.getAssertion().getName() + " from module " + file.getName());
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.finest("Custom assertion " + descriptor);
                    }
                } catch (ModuleException e) {
                    logger.log(Level.WARNING, e.getMessage(), e);
                }
            }

            // publish AssertionModuleRegistrationEvent.
            if (!isShuttingDown) {
                callbacks.publishEvent(new AssertionModuleRegistrationEvent(this, module));
            }

            // indicate module successfully loaded.
            classLoaderOnError.clear();

            return retValue;
        } catch (ModuleException e) {
            // just rethrow our exception
            throw e;
        } catch (Throwable e) {
            // anything else throw ModuleException
            throw new ModuleException("Error while loading custom assertion module \"" + file.getName() + "\":", e);
        } finally {
            // close the class loader if set
            classLoaderOnError.close();
        }
    }

    /**
     * Unregister a custom assertion module, perhaps because we noticed that its file has been deleted.
     * <p/>
     * Fires an AssertionModuleUnregistrationEvent if the module is un-registered successfully.
     *
     * @see com.l7tech.server.policy.module.ModulesScanner#onModuleUnload(com.l7tech.server.policy.module.BaseAssertionModule)
     */
    @Override
    protected boolean onModuleUnload(final CustomAssertionModule module) throws ModuleException {
        // sanity check
        if (module == null)
            throw new ModuleException("module cannot be null", new NullPointerException());

        // remove module from loaded/scanned hash-map
        if (removeModule(module) == null) {
            // module is not loaded, therefore do not send unload notifications.
            return false;
        }

        clearFailedModTimes(); // retry all failures whenever a module is loaded or unloaded

        try {
            // unregister all module custom assertion descriptors
            for (CustomAssertionDescriptor descriptor : module.getDescriptors()) {
                callbacks.unregisterAssertion(descriptor);
            }

            // we indeed do not expose the Background service to custom assertions, but it shouldn't cause any issues
            // to call cancelAllTasksFromClassLoader method, though it may have some performance impact, since the method
            // call is synchronized.
            Background.cancelAllTasksFromClassLoader(module.getModuleClassLoader());

            // publish module un-registration event
            if (!isShuttingDown) {
                callbacks.publishEvent(new AssertionModuleUnregistrationEvent(this, module));
            }

            try {
                // close the module
                module.close();
                logger.info("Module \""  + module.getName() + "\" is successfully unloaded!");
            } catch (IOException e) {
                logger.log(Level.WARNING, "Exception while closing custom assertion module \"" + module.getName() + "\"", e);
            }
        } catch (Throwable e) {
            // log the error and continue loading the new one
            logger.log(Level.WARNING, "Failed to unload module \"" + module.getName() + "\":", e);
        }

        return true;
    }

    @Override
    public synchronized void destroy() {
        isShuttingDown = true;
        super.destroy();
    }

    public void startTimer(long rescanMillis) {
        super.startTimer(
                rescanMillis,
                new Runnable() {
                    @Override
                    public void run() {
                        // skip while shutting down
                        if (isShuttingDown) {
                            return;
                        }
                        scanModules();
                    }
                });
    }

    @Override
    protected void onScanComplete(boolean changesMade) {
        // reset initial scan flag
        isInitialScan = false;
    }

    /**
     * Convenient method for calling the base {@link com.l7tech.server.policy.module.ModulesScanner#scanModules(ModulesConfig) scanModules}
     * with our cached config object.
     *
     * @return <code>true</code> if changes has been detected (i.e. there is a new, deleted or modified module) in the modules list,
     * <code>false</code> otherwise.
     *
     * @see com.l7tech.server.policy.module.ModulesScanner#scanModules(ModulesConfig)
     */
    public boolean scanModules() {
        return super.scanModules(modulesConfig);
    }
}
