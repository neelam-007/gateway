package com.l7tech.server.policy.module;

import com.l7tech.gateway.common.module.ModuleDigest;
import com.l7tech.gateway.common.module.ModuleLoadingException;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A base abstract class for scanning modules changes.<br/>
 * If needed extend the class and override needed functions to provide custom functionality.
 * <p/>
 * Currently the scanner checks whether the modules folder timestamp has changed in order to start processing actual module files,
 * therefore modules loading and unloading behaviour depends on that.<br/>
 * If this is changed in anyway consider revisiting modules loading and unloading functionality.
 *
 * @param <T>    Custom implementation of {@link BaseAssertionModule}
 */
public abstract class ModulesScanner<T extends BaseAssertionModule> {
    // the logger for this class
    private static final Logger logger = Logger.getLogger(ModulesScanner.class.getName());

    // which folder was last scanned
    protected File lastScanDir = null;
    // modification timestamp of the last scanned folder
    protected long lastScanDirModTime = 0;

    /**
     * The container of currently scanned i.e. loaded modules
     */
    protected final Map<String, T> scannedModules = new ConcurrentHashMap<>();

    /**
     * Container of modules which failed to load since last time we've scanned.
     * They are going to be reloaded next scheduled scan (this is how the original logic for modular assertions was).
     */
    protected final Map<String, Long> failModTimes = new ConcurrentHashMap<>();

    /**
     * Container of modules which did not load or unload since .
     * This is
     */
    protected final Map<String, Long> skipModTimes = new ConcurrentHashMap<>();


    /**
     * Helper class defining the status of {@link #onModuleLoad(java.io.File, String, long) onModuleLoad} function,
     * i.e. indicating whether a previous version of the module was unloaded or not and whether the new module was successfully loaded or not.<br/>
     * It's used to properly set the <code>changes-made</code> flag i.e. the return of {@link #scanModules(ModulesConfig) scanModules} function.
     *
     * @param <T>    Custom implementation of {@link BaseAssertionModule}
     */
    protected static class ModuleLoadStatus<T extends BaseAssertionModule> {
        /**
         * Was any previous module version unloaded?
         */
        private boolean isPrevModuleUnloaded;

        /**
         * Variable holding any newly loaded module.<br/>
         * <code>Null</code> indicates this module was not loaded and should be added to skipped modules list.
         */
        private T loadedModule;

        /**
         * Default constructor.<br/>
         * Set default values to indicate no-changes i.e. sets {@link #isPrevModuleUnloaded} to <code>false</code> and {@link #loadedModule} to <code>null</code>.
         */
        public ModuleLoadStatus() {
            this(false, null);
        }

        public ModuleLoadStatus(final boolean isPrevModuleUnloaded, @Nullable final T loadedModule) {
            this.isPrevModuleUnloaded = isPrevModuleUnloaded;
            this.loadedModule = loadedModule;
        }

        // getters and setters

        public boolean isPrevModuleUnloaded() {
            return isPrevModuleUnloaded;
        }

        public void setPrevModuleUnloaded(boolean prevModuleUnloaded) {
            isPrevModuleUnloaded = prevModuleUnloaded;
        }

        public T getLoadedModule() {
            return loadedModule;
        }

        public void setLoadedModule(@Nullable final T loadedModule) {
            this.loadedModule = loadedModule;
        }
    }

    /**
     * Implement this method to provide targeted functionality for modular or custom assertions.<br/>
     * When this method is called it indicates that the specified module file is either new or changed.
     * <p/>
     * The actual addition or deletion of the module from the container must happen in the implementation,
     * by calling {@link #insertModule(BaseAssertionModule)} or {@link #removeModule(BaseAssertionModule)} methods, respectfully.<br/>
     * <p/>
     * <code>ModulesScanner</code> assumes that returning <code>true</code> for {@link ModuleLoadStatus#isPrevModuleUnloaded unloaded module}
     * means that the previous version of the module has been successfully removed from the container,
     * and returning <code>non-null</code> for {@link ModuleLoadStatus#loadedModule loaded module} means that the module has been successfully added into the container.
     * Returning the correct result is essential in order to propagate the correct state upwards.<br/>
     * The following table show how the <code>changes-made</code> flag will be set based on the returned {@link ModuleLoadStatus ModuleLoadStatus} object,
     * i.e. depending whether previous version of the module was unloaded and whether the new module was successfully loaded:
     *
     * <table border="1">
     *     <tr>
     *         <th scope=col>Unloaded </th>
     *         <th scope=col>Loaded </th>
     *         <th scope=col>changes-made </th>
     *     </tr>
     *     <tr>
     *         <td align="center">Yes </td>
     *         <td align="center">Yes </td>
     *         <td align="center">Yes </td>
     *     </tr>
     *     <tr>
     *         <td align="center">Yes </td>
     *         <td align="center">No </td>
     *         <td align="center">Yes </td>
     *     </tr>
     *     <tr>
     *         <td align="center">No </td>
     *         <td align="center">Yes </td>
     *         <td align="center">Yes </td>
     *     </tr>
     *     <tr>
     *         <td align="center">No </td>
     *         <td align="center">No </td>
     *         <td align="center">No </td>
     *     </tr>
     * </table>
     * <br/>
     * <p>Notes:</p>
     * <ul>
     *     <li>returning <code>null</code> value for {@link ModuleLoadStatus#loadedModule loaded module} in general will indicate <i>skip module load</i>.</li>
     *     <li>Throwing exception will <i>not</i> set the <i>changes-made</i> flag.<br/>
     *     Additionally, the module will be added in the failed modules container (i.e. {@link #failModTimes}) and will be ignored until a new version
     *     of the file is dropped in the modules folder, unless the failed modules container is reset by calling {@link #clearFailedModTimes()}.</li>
     * </ul>
     *
     * @param file            the file containing the module to register.  Must be an existing, readable jar-file in the modules directory.
     * @param digest          file checksum (currently SHA256).
     * @param lastModified    file last modified time.
     * @return an instance of {@link ModuleLoadStatus ModuleLoadStatus} containing the status of the scan.
     * @throws ModuleException if an error happens during registration process, in which case the module will be added in the
     * failed modules container {@link #failModTimes} and will be ignored until a new version of the file is dropped in the modules folder.
     */
    @NotNull
    protected abstract ModuleLoadStatus<T> onModuleLoad(File file, String digest, long lastModified) throws ModuleException;

    /**
     * Implement this method to provide targeted functionality for modular or custom assertions.<br/>
     * When this method is called it indicates that the specified module is removed from the filesystem.
     * Its the responsibility of the specific implementation to do actual deletion from the modules container (i.e. {@link #scannedModules}).
     * <ul>
     *     <li>
     *         Returning <code>non-null</code> will set the <i>changes-made</i> flag to true.
     *     </li>
     *     <li>
     *         Returning <code>null</code> will <i>not</i> set the <i>changes-made</i> flag.<br/>
     *         Returning <code>null</code> in general will indicate <i>skip module unload</i>.<br/>
     *         The module will be ignored until the modules folder timestamp has changed, i.e. by either adding or removing files.
     *     </li>
     *     <li>
     *         Throwing exception will <i>not</i> set the <i>changes-made</i> flag.<br/>
     *         The module will be ignored until the modules folder timestamp has changed, i.e. by either adding or removing files.</li>
     * </ul>
     * The actual removal of the module from the container must happen in the implementation, by calling {@link #removeModule(BaseAssertionModule)} methods.<br/>
     * <code>ModulesScanner</code> assumes returning <code>non-null</code> means that the module has been successfully removed from the container.<br/>
     * Returning the correct result is essential in order to propagate the correct state upwards.
     *
     * @param module    the assertion module to unload.
     * @return <code>true</code> if the module was successfully unloaded, <code>false</code> otherwise
     * @throws ModuleException throw this exception to indicate that an error happen during unload process, so that the module can be re-unloaded on next change.
     */
    protected abstract boolean onModuleUnload(T module) throws ModuleException;

    /**
     * Called when a certain scan either forced or due to change made to the modules folder is about to finish,
     * regardless whether it was successful or not.<br/>
     * Override this method to receive notification when certain scan is done.
     */
    protected void onScanComplete(boolean changesMade) {
        // nothing to do.
    }

    /**
     * Retrieve loaded module with the specified module filename.
     *
     * @param moduleFilename    the module filename.
     * @return the loaded module object associated with the specified filename.
     */
    public T getModule(final String moduleFilename) {
        return scannedModules.get(moduleFilename);
    }

    /**
     * Insert a module into scanned container.
     *
     * @param module    the module to be inserted.
     * @return the previous version of the module.
     */
    protected T insertModule(@NotNull final T module) {
        return scannedModules.put(module.getName(), module);
    }

    /**
     * Remove the specified module from the scanned container.
     *
     * @param module    the module to be removed.
     * @return the module which was just removed.
     */
    protected T removeModule(@NotNull final T module) {
        return scannedModules.remove(module.getName());
    }

    /**
     * Remove all scammed modules.
     */
    protected void clearScannedModules() {
        scannedModules.clear();
    }

    /**
     * Remove all failed modules.
     */
    protected void clearFailedModTimes() {
        failModTimes.clear();
    }

    /**
     * Remove all skipped modules.
     */
    protected void clearSkippedModTimes() {
        skipModTimes.clear();
    }

    /**
     * Convenient method for extracting scanned modules.
     *
     * @return a concurrent and read-only view of the values contained with {@link #scannedModules}
     */
    public Collection<T> getModules() {
        return Collections.unmodifiableCollection(scannedModules.values());
    }

    /**
     * Checks if the custom assertion modules folder or its content has changed.<br/>
     * In other words, checks for files that have been added, removed or disabled since the last scan.
     * In addition, the method checks for failed modules to retry loading.
     * <br/>
     * In order to optimize things, the logic relies on a folder timestamp i.e. depending on the filesystem to set the
     * folder modification time accordingly. In most cases this means that in order to flag certain module as modified,
     * delete and afterwards add the new module file in the same location, otherwise, overwriting the file directly will
     * most likely cause the folder timestamp to remain the same i.e. causing the logic not to detect this change.
     * <p/>
     * Override this method to provide extended functionality.
     *
     * @param dir                the custom assertion modules dir.
     * @param dirLastModified    the custom assertion modules dir last modified timestamp.
     * @return true to indicate that a scan is needed, false otherwise.
     */
    protected boolean isScanNeeded(final File dir, long dirLastModified) {
        return !dir.equals(lastScanDir) ||
                (dirLastModified != lastScanDirModTime) ||
                !failModTimes.isEmpty();
    }

    /**
     * Go through all scanned modules and check whether they have been either removed or disabled.
     * <br/>
     * The logic will loop through the current loaded modules compering them against this scan modules list.
     * If certain modules doesn't exist in this scan, the module will be unloaded.
     *
     * @param moduleFileNames    the list of current modules file names
     * @return true if there were removed or disabled modules, false otherwise.
     */
    protected boolean processRemovedModules(@NotNull final Set<String> moduleFileNames) {
        boolean changesMade = false;
        // removing a module while still iterating is ok here, since scannedModules is a ConcurrentHashMap
        // if scannedModules is changed to a different type e.g. HashMap, then the logic below
        // must be redesigned to use Iterator.remove instead, so that ConcurrentModificationException is avoided.
        for (T module : scannedModules.values()) {
            final String name = module.getName();
            if (!moduleFileNames.contains(name)) {
                logger.fine("Processing module that has been removed or disabled: " + name);
                try {
                    // if this module is not skipped (i.e. marked as non-unloadable)
                    if (skipModTimes.get(name) == null) {
                        // try to unload the module
                        if (onModuleUnload(module)) {
                            // the module was successfully unloaded
                            changesMade = true;
                        } else {
                            // the module was marked as non-unloadable, add it to the skipped list,
                            // so that it won't be unloaded next time we scan
                            skipModTimes.put(module.getName(), module.getModifiedTime());
                        }
                    }
                } catch (ModuleException e) {
                    logger.log(Level.WARNING, "Failed to unload module " + module.getName() + ", module will be retried during next change.");
                    logger.log(Level.WARNING, "Error: " + ExceptionUtils.getMessage(e), e);
                }
            }
        }
        return changesMade;
    }

    /**
     * Clear all removed or disabled modules from the failed modules list.
     *
     * @param moduleNames    the list of current modules file names
     */
    protected void cleanRemovedFailedModules(@NotNull Set<String> moduleNames) {
        final Set<String> failedNames = failModTimes.keySet();
        for (String failedName : failedNames) {
            if (!moduleNames.contains(failedName)) {
                logger.info("Forgetting about failed module that has been removed or disabled: " + failedName);
                failModTimes.remove(failedName);
            }
        }
    }

    /**
     * A utility builder class for convenient way of calculating module digest (currently SHA256) string from file object .
     */
    public static class DigestBuilder {
        private File file = null;
        private boolean closeFile = true; // by default close the file

        public DigestBuilder file(@NotNull final File file) {
            this.file = file;
            return this;
        }

        @SuppressWarnings("UnusedDeclaration")
        public DigestBuilder closeFile(final boolean closeFile) {
            this.closeFile = closeFile;
            return this;
        }

        public String build() throws IOException {
            if (file == null) {
                throw new IllegalArgumentException("file cannot be null");
            }
            return ModuleDigest.digest(new FileInputStream(file), closeFile);
        }
    }

    /**
     * Processes changed or new modules.<br/>
     * Deleted or disabled modules have previously been handled.
     *
     * @param jarModules                 the list of module files found in the configured modules folder.
     * @param disabledModuleFileNames    the list of disabled module files to skip.
     * @return true we found new or changed modules, false otherwise.
     */
    protected boolean processNewOrChangedModules(@NotNull final File[] jarModules, @NotNull final Set<String> disabledModuleFileNames) {
        // indicates whether changes were made
        boolean changesMade = false;

        // loop through all jar files
        for (final File module : jarModules)
        {
            // check if this is indeed a file and not a folder ending with .jar
            if (!module.isFile()) {
                if (logger.isLoggable(Level.FINE)) { // avoid unnecessary call to getAbsolutePath()
                    logger.info("This is not an regular file [" + module.getAbsolutePath() + "], skipping it...");
                }
                continue;
            }

            // get the module filename
            final String fileName = module.getName();

            // Ignore disabled flags
            if (disabledModuleFileNames.contains(fileName)) {
                continue;
            }

            // get the last modified timestamp
            //
            // TODO XXX some annoying race conditions here if the file is changed in between getting timestamp <-> getting digest <-> loading jar.
            // doctor's answer for now, until we find a way to get all the info from a FileDescriptor instead of a File
            long lastModified = module.lastModified();

            // if this module was marked as skipped, check if it was modified since the last time it was skipped.
            if (new Long(lastModified).equals(skipModTimes.get(fileName))) {
                logger.finer("Ignoring module file \"" + fileName + "\" since its modification time hasn't changed since the last time it was skipped.");
                continue;
            }

            try {
                // find previous loaded module with the same name
                final T previousModule = scannedModules.get(fileName);
                if (previousModule == null) {
                    // if this module has not been previously loaded, check if it was modified since the last time it failed.
                    if (new Long(lastModified).equals(failModTimes.get(fileName))) {
                        logger.fine("Ignoring module file \"" + fileName + "\" since its modification time hasn't changed since the last time it failed to load successfully.");
                        continue;
                    }
                } else if (previousModule.getModifiedTime() == lastModified) {
                    // this jar file was not changed
                    logger.fine("Skipped module file \"" + fileName + "\" was not changed.");
                    continue;
                }

                logger.info("Checking module with updated timestamp: " + fileName);

                // verify module checksum (if not specified DigestBuilder closeFile defaults to true)
                //
                // TODO XXX some annoying race conditions here if the file is changed in between getting timestamp <-> getting moduleDigest <-> loading jar..
                // doctor's answer for now, until we find a way to get all the info from a FileDescriptor instead of a File
                final String moduleDigest = new DigestBuilder().file(module).build();
                if (previousModule != null && previousModule.getDigest().equals(moduleDigest)) {
                    logger.info("Won't reload module \"" + fileName + "\", since its checksum hasn't changed");
                    continue;
                }

                // load and create the new or modified module
                //final T newModule = onModuleLoad(module, moduleDigest, lastModified);
                final ModuleLoadStatus loadStatus = onModuleLoad(module, moduleDigest, lastModified);

                // set the changes-made flag
                changesMade = changesMade || (loadStatus.isPrevModuleUnloaded() || (loadStatus.getLoadedModule() != null));

                // should we skip this module
                if (loadStatus.getLoadedModule() == null) {
                    logger.warning("Module \"" + fileName + "\" marked as skipped, will not be loaded at this time.");
                    skipModTimes.put(fileName, lastModified);
                    continue;
                }

                // once successfully loaded remove if previously skipped
                skipModTimes.remove(loadStatus.getLoadedModule().getName());
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Unable to read assertion module [" + fileName + "] (ignoring it until it changes): " + ExceptionUtils.getMessage(e), e);
                failModTimes.put(fileName, lastModified);
            } catch (ModuleException e) {
                logger.log(Level.SEVERE, "Unable to load assertion module [" + fileName + "] (ignoring it until it changes): " + ExceptionUtils.getMessage(e), e);
                failModTimes.put(fileName, lastModified);
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "Unhandled exception while loading assertion module [" + fileName + "] (ignoring it until it changes): " + ExceptionUtils.getMessage(e), e);
                failModTimes.put(fileName, lastModified);
            }
        }

        return changesMade;
    }

    /**
     * Load a {@code ServerModuleFile} specified with its {@code stagedFile} and {@code moduleDigest}.
     *
     * @param stagedFile      the module staging file.  Required and cannot be {@code null}.
     * @param moduleDigest    the module digest, currently SHA-256.  Required and cannot be {@code null}.
     * @throws ModuleLoadingException if an error happens while loading the {@code ServerModuleFile}.
     */
    public void loadServerModuleFile(@NotNull final File stagedFile, @NotNull final String moduleDigest) throws ModuleLoadingException {
        // get the module filename
        final String fileName = stagedFile.getName();
        // get module last modified timestamp
        final long lastModified = stagedFile.lastModified();

        // find previous loaded module with the same name
        final T previousModule = scannedModules.get(fileName);
        if (previousModule != null && moduleDigest.equals(previousModule.getDigest())) {
            logger.info("Won't reload module \"" + fileName + "\", since its checksum hasn't changed");
            return;
        }

        try {
            // load and create the new or modified module
            //final T newModule = onModuleLoad(module, moduleDigest, lastModified);
            final ModuleLoadStatus loadStatus = onModuleLoad(stagedFile, moduleDigest, lastModified);

            // should we skip this module
            if (loadStatus.getLoadedModule() == null) {
                logger.warning("Module \"" + fileName + "\" marked as skipped, will not be loaded at this time.");
                throw new ModuleLoadingException("Module \"" + fileName + "\" marked as skipped, will not be loaded at this time.");
            }
        } catch (final ModuleException e) {
            throw new ModuleLoadingException(e);
        }
    }

    public void unloadServerModuleFile(@NotNull final File stagedFile, @NotNull final String moduleDigest) throws ModuleLoadingException {
        // get the module filename
        final String fileName = stagedFile.getName();

        // get the module
        final T module = getModule(fileName);
        if (module == null) {
            throw new ModuleLoadingException("Cannot unload module which is not loaded; staged file-name \"" + stagedFile + "\"");
        }
        if (!moduleDigest.equals(module.getDigest())) {
            throw new ModuleLoadingException("Cannot unload module as digest of loaded and staged file mismatched; staged file-name \"" + stagedFile + "\"");
        }
        try {
            if (!onModuleUnload(module)) {
                throw new ModuleLoadingException("Failed to unload module; staged file-name \"" + stagedFile + "\"");
            }
        } catch (final ModuleException e) {
            throw new ModuleLoadingException(e);
        }
    }

    /**
     * Scan for any changes in the modules folder, which includes new modules, modules being changed, deleted or disabled.
     *
     * @param config    The modules configuration, providing path to the modules folder etc.
     * @return <code>true</code> if we found modules either new, changed or deleted, <code>false</code> otherwise.<br/>
     * In other words return whether current loaded modules list was changed or not during this scan.
     */
    protected synchronized boolean scanModules(@NotNull final ModulesConfig config) {

        // check if there is a Gateway feature for executing modules
        if (!config.isFeatureEnabled()) {
            return false;
        }

        // check if scanning is enabled
        if (!config.isScanningEnabled()) {
            return false;
        }

        // read the custom assertions modules installation dir
        final File moduleDir = config.getModuleDir();

        // do we need to scan
        long moduleDirLastModified = moduleDir.lastModified();
        if (!isScanNeeded(moduleDir, moduleDirLastModified)) {
            logger.finest("No assertion module files added, removed or disables since our last scan, and no failures to retry");
            return false;
        }

        logger.log(Level.FINE, "Scanning custom assertion modules directory {0}...", moduleDir.getAbsolutePath());

        // a flag indicating if there are new, deleted or modified modules
        boolean changesMade = false;

        try {
            lastScanDir = moduleDir;
            lastScanDirModTime = moduleDirLastModified;

            // hash-set holding all module file names, for easier search
            final Set<String> moduleFileNames = new HashSet<>();

            // hash-set holding all disabled module file names, used for file locking workaround on Windows platform
            final Set<String> disabledModuleFileNames = new HashSet<>();

            // get all jar files from custom assertion modules directory
            final File[] jars = moduleDir.listFiles(new FilenameFilter() {
                private final List<String> allowedExtensions = config.getModulesExt();

                ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
                // Workaround for mandatory file locking, which prevents modules from being unloaded on Windows platform only.
                // To control whether to enable or disable the workaround implement the getDisabledSuffix method separately
                // for modular and custom assertions.
                // Returning null or empty-string will disable this functionality.
                // To force disable or enable for both modular or custom assertions hard-code the value directly into disabledSuffix variable.
                private final String disabledSuffix = config.getDisabledSuffix();
                ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

                @Override
                public boolean accept(File dir, String name) {
                    // get the name in lower case
                    final String lcName = name.toLowerCase();
                    for (String extension : allowedExtensions) {
                        if (lcName.endsWith(extension.trim().toLowerCase())) {
                            // add it to the jar file names set
                            moduleFileNames.add(name);
                            return true; // accept the file
                        }
                    }

                    // check if file locking workaround for Windows platform is enabled or not
                    if (disabledSuffix != null && !disabledSuffix.trim().isEmpty()) {
                        if (lcName.endsWith(disabledSuffix.trim().toLowerCase())) {
                            final String moduleName = name.substring(0, name.length() - disabledSuffix.length());
                            disabledModuleFileNames.add(moduleName);
                            logger.fine("Pretending module file \"" + moduleName + "\" isn't there, because it is flagged as disabled");
                        }
                    }

                    logger.finer("Skipping unsupported module \"" + name + "\"");
                    return false;
                }
            });
            if (jars == null) {
                // failed to read directory files
                logger.log(Level.WARNING, "Failed to get the file list for modules dir: " + moduleDir.getName());
                return false;
            }

            // Ensure files are always processed in sorted order, regardless of the ordering policy of File.listFiles() in this environment
            // We will always process the files in Unicode lexical order, case sensitively, regardless of OS.
            Arrays.sort( jars, new Comparator<File>() {
                @Override
                public int compare( File a, File b ) {
                    return a.getName().compareTo( b.getName() );
                }
            } );

            // loop through all disabled modules and remove them from moduleFileNames
            for (final String disabledModuleName : disabledModuleFileNames) {
                moduleFileNames.remove(disabledModuleName);
            }

            // Unregister removed or disabled custom assertion modules
            if (processRemovedModules(moduleFileNames)) {
                // mark that some modules have been deleted
                changesMade = true;
            }

            // check for removed or disabled failed modules
            cleanRemovedFailedModules(moduleFileNames);

            // process new and modified custom assertions
            if (processNewOrChangedModules(jars, disabledModuleFileNames)) {
                // mark that some modules have been added or replaced
                changesMade = true;
            }

            return changesMade;
        } finally {
            onScanComplete(changesMade);
        }
    }

    /**
     * Resets the variables containing last scan state.<br/>
     * Useful for forcing next scan even if the modules folder have not been changed.
     */
    protected void reset() {
        lastScanDir = null;
        lastScanDirModTime = 0;
    }

    /**
     * Sets the scanner to its initial state.
     * Call this method on SSG shutdown.
     */
    public synchronized void destroy() {
        for (T module : scannedModules.values()) {
            try {
                onModuleUnload(module);
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Error while un-registering module " + module.getName() + ": " + ExceptionUtils.getMessage(t), t);
            }
        }

        clearScannedModules();
        clearFailedModTimes();
        clearSkippedModTimes();

        reset();
    }
}
