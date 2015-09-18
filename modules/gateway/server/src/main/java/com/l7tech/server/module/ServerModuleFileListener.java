package com.l7tech.server.module;

import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.module.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.LicenseChangeEvent;
import com.l7tech.server.event.system.ServerModuleFileSystemEvent;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.security.signer.SignatureVerifier;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.*;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.*;
import java.security.SignatureException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.util.Eithers.isSuccess;

/**
 * Server Module File event listener.<br/>
 * Listens for {@link ServerModuleFile} {@link EntityInvalidationEvent}'s.
 */
public class ServerModuleFileListener implements ApplicationContextAware, PostStartupApplicationListener {
    private static final Logger logger = Logger.getLogger(ServerModuleFileListener.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle(ServerModuleFileListener.class.getName());

    // Logger friendly names for EntityInvalidationEvent operation
    private static final String EVENT_OPERATION_CREATE_UFN = "CREATE";
    private static final String EVENT_OPERATION_UPDATE_UFN = "UPDATE";
    private static final String EVENT_OPERATION_DELETE_UFN = "DELETE";

    @NotNull protected final ServerModuleFileManager serverModuleFileManager;
    protected final PlatformTransactionManager transactionManager;
    @NotNull protected final Config config;
    @NotNull protected final ServerAssertionRegistry modularAssertionRegistrar;
    @NotNull protected final CustomAssertionsRegistrar customAssertionRegistrar;
    @NotNull protected final SignatureVerifier signatureVerifier;
    private ApplicationContext applicationContext;

    /**
     * Volatile flag indicating whether the {@link Started Gateway started event} was processed.<br/>
     * Other events will only be handled after {@code Started} event is processed i.e. only when this flag is set to {@code true}.
     */
    private volatile boolean gatewayStarted = false;

    /**
     * Single worker executor to guarantee that there will be no file-system racing conditions
     */
    @NotNull private final ExecutorService eventHandlerExecutor;

    /**
     * Represents a copy of {@link ServerModuleFile} without bytes and state i.e. {@code copyFrom(module, false, true, false)}.<br/>
     * Optionally it holds the {@link #stagingFile} after the content is downloaded.
     *
     * @see #copyFrom(com.l7tech.gateway.common.module.ServerModuleFile, boolean, boolean, boolean)
     */
    @SuppressWarnings("serial")
    static class StagedServerModuleFile extends ServerModuleFile {
        /**
         * The Module File inside the staging folder.  Optional and can be {@code null}
         */
        @Nullable
        private File stagingFile;

        /**
         * Default constructor.
         * @param module    the module to copy from.  Required and cannot be {@code null}.
         */
        StagedServerModuleFile(@NotNull final ServerModuleFile module) {
            this(module, null);
        }

        /**
         * Construct a copy of the specified {@code module} with optional staging file.
         *
         * @param module         the module to copy from.  Required and cannot be {@code null}.
         * @param stagingFile    the module staging file, after successfully downloading module content into the staging folder.  Optional and can be {@code null}.
         */
        StagedServerModuleFile(@NotNull final ServerModuleFile module, @Nullable final File stagingFile) {
            this.stagingFile = stagingFile;
            copyFrom(module, false, true, false);
        }

        /**
         * Getter for {@link #stagingFile staging file}.
         */
        @Nullable
        File getStagingFile() {
            return stagingFile;
        }

        /**
         * Setter for {@link #stagingFile staging file}.
         */
        void setStagingFile(@Nullable final File stagingFile) {
            this.stagingFile = stagingFile;
        }

        /**
         * Utility method for deleting the {@link #stagingFile staging file} associated with this module.
         */
        void deleteStagingFile() {
            if (stagingFile != null) {
                try {
                    FileUtils.delete(stagingFile);
                } catch (final IOException ex) {
                    logger.log(Level.WARNING, "Failed to remove staged module file: " + stagingFile, ExceptionUtils.getDebugException(ex));
                }
            }
        }
    }

    /**
     * Contains cached server module files copies, having only meta-data (i.e. without bytes and state).
     */
    @NotNull protected final Map<Goid, StagedServerModuleFile> knownModuleFiles = new ConcurrentHashMap<>();

    /**
     * Contains the root staging folder i.e. the value of
     * {@link com.l7tech.server.ServerConfigParams#PARAM_SERVER_MODULE_FILE_STAGING_FOLDER} system property.
     * Default is <code>${ssg.var}/modstaging</code>.
     */
    private File stagingRootDir;

    /**
     * Default constructor.
     *
     * @param serverModuleFileManager    {@link ServerModuleFile} entity manager.  Required and cannot be {@code null}.
     * @param transactionManager         Transaction manager.
     * @param config                     Server Config.  Required and cannot be {@code null}.
     * @param modularAssertionRegistrar  Modular Assertions Registrar.  Required and cannot be {@code null}.
     * @param customAssertionRegistrar   Custom Assertions Registrar.  Required and cannot be {@code null}.
     */
    public ServerModuleFileListener(
            @NotNull final ServerModuleFileManager serverModuleFileManager,
            @Nullable final PlatformTransactionManager transactionManager,
            @NotNull final Config config,
            @NotNull final ServerAssertionRegistry modularAssertionRegistrar,
            @NotNull final CustomAssertionsRegistrar customAssertionRegistrar,
            @NotNull final SignatureVerifier signatureVerifier
    ) {
        this.serverModuleFileManager = serverModuleFileManager;
        this.transactionManager = transactionManager;
        this.config = config;
        this.modularAssertionRegistrar = modularAssertionRegistrar;
        this.customAssertionRegistrar = customAssertionRegistrar;
        this.signatureVerifier = signatureVerifier;
        // create a single worker to guarantee that there will be no racing conditions
        this.eventHandlerExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * Handle Application events, specifically {@link com.l7tech.server.event.EntityInvalidationEvent} for
     * {@link com.l7tech.gateway.common.module.ServerModuleFile} entity.
     *
     * @param event    the {@link ApplicationEvent event} that occurred.
     */
    @Override
    public void onApplicationEvent(final ApplicationEvent event) {
        handleEvent(event); // do not wait for the future here
    }

    /**
     * Handle Application events, specifically {@link com.l7tech.server.event.EntityInvalidationEvent} for
     * {@link com.l7tech.gateway.common.module.ServerModuleFile} entity.
     *
     * @param event    the {@link ApplicationEvent event} that occurred.  Required and cannot be {@code null}.
     * @return a {@link Future} representing pending completion of the task, or {@code null} if no task is performed/created,
     * e.g. if the {@code EntityInvalidationEvent} is for a entity other then {@code ServerModuleFile}, then the event is
     * ignored, thus {@code null} is returned.<br/>
     * Another example is if the event is other then {@code Started} and {@code EntityInvalidationEvent}.
     */
    Future<?> handleEvent(@NotNull final ApplicationEvent event) {
        try {
            // once the Gateway is up and running, create staging folders for custom and modular modules.
            if (event instanceof Started) {
                knownModuleFiles.clear();
                final Future<?> future = eventHandlerExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            processGatewayStartedEvent();
                        } catch (final RuntimeException e) {
                            logger.log(Level.SEVERE, "Unhandled exception while Processing Gateway Startup event: " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
                            final Thread t = Thread.currentThread();
                            final Thread.UncaughtExceptionHandler exceptionHandler = t.getUncaughtExceptionHandler();
                            if (exceptionHandler != null) {
                                exceptionHandler.uncaughtException(t, e);
                            }
                        }
                    }
                });
                // Important:
                // at the moment the events handler executor is single threaded (meaning events will be processed in order)
                // therefore it is ok to set the gatewayStarted flag without waiting for the task to complete
                // as consecutive events tasks will execute after this one (thus the single threaded design)
                //
                // If the events handler executor threading model is changed in the future then consider waiting for
                // this task to complete before setting the gatewayStarted flag to true
                gatewayStarted = true;
                return future;
            }

            // do not process any events until SSG is started (making sure all modules are loaded).
            if (!gatewayStarted) {
                return null;
            }

            // check for license change
            if (event instanceof LicenseChangeEvent) {
                return eventHandlerExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            processLicenseChangeEvent();
                        } catch (final RuntimeException e) {
                            logger.log(Level.SEVERE, "Unhandled exception while Processing Gateway Startup event: " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
                            final Thread t = Thread.currentThread();
                            final Thread.UncaughtExceptionHandler exceptionHandler = t.getUncaughtExceptionHandler();
                            if (exceptionHandler != null) {
                                exceptionHandler.uncaughtException(t, e);
                            }
                        }
                    }
                });
            }

            if (event instanceof EntityInvalidationEvent) {
                // we are only interested in ServerModuleFile entity changes
                final EntityInvalidationEvent entityInvalidationEvent = (EntityInvalidationEvent) event;
                if (!ServerModuleFile.class.equals(entityInvalidationEvent.getEntityClass())) {
                    return null;
                }
                return eventHandlerExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            processServerModuleFileInvalidationEvent(entityInvalidationEvent);
                        } catch (final RuntimeException e) {
                            logger.log(Level.SEVERE, "Unhandled exception while Processing Entity Invalidation event: " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
                            final Thread t = Thread.currentThread();
                            final Thread.UncaughtExceptionHandler exceptionHandler = t.getUncaughtExceptionHandler();
                            if (exceptionHandler != null) {
                                exceptionHandler.uncaughtException(t, e);
                            }
                        }
                    }
                });
            }
        } catch (final Throwable e) {
            logger.log(Level.SEVERE, "Unhandled exception while handling Server Module Files events: " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
        }

        return null;
    }

    /**
     * Handles {@link Started Gateway started event}.<br/>
     * Loops through all server module file entities in the DB, adds each module into the {@link #knownModuleFiles known module cache}
     * and finally, if modules upload is enabled, tries to load each module.
     */
    void processGatewayStartedEvent() {
        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "processGatewayStartedEvent...");
        }

        // log that ServerModuleFiles are about to be loaded
        logger.info("Loading Server Module Files from Database.");

        // delete any previous staging files
        try {
            if (!FileUtils.deleteDirContents(getStagingDir())) {
                logger.log(Level.WARNING, "Failed to delete previous staging files.");
            }
        } catch (final ConfigException e) {
            logger.log(Level.WARNING, "Failed to get Staging Folder: " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
        }

        // get all modules from the database
        try {
            final Collection<ServerModuleFile> modules = Eithers.extract(
                    readOnlyTransaction(new Functions.Nullary<Either<FindException, Collection<ServerModuleFile>>>() {
                        @Override
                        public Either<FindException, Collection<ServerModuleFile>> call() {
                            try {
                                return Either.right(serverModuleFileManager.findAll());
                            } catch (final FindException e) {
                                return Either.left(e);
                            }
                        }
                    })
            );
            final boolean isModuleUploadEnabled = serverModuleFileManager.isModuleUploadEnabled();
            for (final ServerModuleFile moduleFile : modules) {
                final StagedServerModuleFile moduleInfo = addToKnownModules(moduleFile);
                if (isModuleUploadEnabled) {
                    loadModule(moduleInfo);
                }
            }
        } catch (final FindException e) {
            logger.log(Level.WARNING, "Failed to find all Server Module Files: " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
        }
    }

    /**
     * Handles {@link LicenseChangeEvent} event.<br/>
     * Loops through all server module file entities in the DB, adds each module into the {@link #knownModuleFiles known module cache}
     * and finally, if modules upload is enabled, tries to load each module.
     */
    void processLicenseChangeEvent() {
        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "processLicenseChangeEvent...");
        }

        // log that ServerModuleFiles are about to be reloaded
        logger.info("Gateway license changed. Reloading Server Module Files from Database.");

        // scan all modules from DB
        // since this is a license change event all modules from DB are assumed to have been updated.
        scanModules(null);
    }

    /**
     * Handles {@link ServerModuleFile} invalidation events (i.e. {@link EntityInvalidationEvent} event).<br/>
     *
     * @param entityInvalidationEvent    {@code ServerModuleFile} entity invalidation event.
     */
    void processServerModuleFileInvalidationEvent(@NotNull final EntityInvalidationEvent entityInvalidationEvent) {
        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "processServerModuleFileInvalidationEvent...");
        }

        // get ops and goids
        final Goid[] goids = entityInvalidationEvent.getEntityIds();
        final char[] ops = entityInvalidationEvent.getEntityOperations();

        assert goids != null && ops != null;
        assert goids.length == ops.length;

        // get updated Goids
        // these are the ones we are going to reload if not loaded yet
        final Set<Goid> potentialUpdates = new HashSet<>();
        for (int ix = 0; ix < goids.length; ++ix) {
            final char op = ops[ix];
            final Goid goid = goids[ix];
            assert goid != null;
            // skip deleted goids
            if (op == EntityInvalidationEvent.CREATE || op == EntityInvalidationEvent.UPDATE) {
                potentialUpdates.add(goid);
            }
        }

        // scan all modules from DB
        scanModules(Collections.unmodifiableSet(potentialUpdates));
    }

    /**
     * Handles {@link ServerModuleFile} invalidation events.<br/>
     * This handler will install or uninstall the {@code ServerModuleFile} entity associated with the {@link EntityInvalidationEvent}
     *
     * @param entityInvalidationEvent    {@code ServerModuleFile} entity invalidation event.
     */
    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    void legacyProcessServerModuleFileInvalidationEvent(@NotNull final EntityInvalidationEvent entityInvalidationEvent) {
        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "doProcessServerModuleFileInvalidationEvent...");
        }

        // get ops and goids
        final Goid[] goids = entityInvalidationEvent.getEntityIds();
        final char[] ops = entityInvalidationEvent.getEntityOperations();

        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "EntityInvalidationEvent ops \"" + Arrays.toString(ops) + "\", goids \"" + Arrays.toString(goids) + "\"");
        }

        // flag indicating whether module upload is enabled
        final boolean isModuleUploadEnabled = serverModuleFileManager.isModuleUploadEnabled();

        for (int ix = 0; ix < goids.length; ++ix) {
            final char op = ops[ix];
            @NotNull final Goid goid = goids[ix];

            // for debug purposes
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Processing operation \"" + operationToString(op) + "\", for goid \"" + goid + "\"");
            }

            // on error continue with the next operation and log the failure
            if (op == EntityInvalidationEvent.CREATE || op == EntityInvalidationEvent.UPDATE) {
                // extract server module file
                final ServerModuleFile moduleFile;
                try {
                    moduleFile = Eithers.extract(
                            readOnlyTransaction(new Functions.Nullary<Either<FindException, Option<ServerModuleFile>>>() {
                                @Override
                                public Either<FindException, Option<ServerModuleFile>> call() {
                                    try {
                                        return Either.rightOption(serverModuleFileManager.findByPrimaryKey(goid));
                                    } catch (final FindException e) {
                                        return Either.left(e);
                                    }
                                }
                            })
                    ).toNull();
                } catch (final FindException e) {
                    logger.log(Level.WARNING, "Failed to find Server Module File with \"" + goid + "\", operation was \"" + operationToString(op) + "\": " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
                    continue;
                }

                if (moduleFile != null) {
                    // add it to the known modules cache
                    final StagedServerModuleFile moduleInfo = addToKnownModules(moduleFile);
                    // threat create or updated events as create (see SSG-10351 for reference)
                    // todo: correct logic once EntityVersionChecker (see SSG-10351) is properly fixed
                    if (isModuleUploadEnabled && !ModuleState.LOADED.equals(getModuleState(moduleFile))) {
                        loadModule(moduleInfo);
                    } else if (isModuleUploadEnabled) {
                        updateModule(moduleInfo);
                    }
                } else {
                    // non-existent module goid, remove it from known cache
                    knownModuleFiles.remove(goid);
                }
            } else if (op == EntityInvalidationEvent.DELETE) {
                // extract server module file
                final StagedServerModuleFile moduleFile;
                try {
                    moduleFile = removeFromKnownModules(goid);
                } catch (FindException e) {
                    // for debug purposes should be ignored otherwise
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Unable to find Server Module File with \"" + goid + "\", from known modules cache. Ignoring Module Delete Event.", e);
                    }
                    return;
                }

                // process removed module only if upload is enabled
                if (isModuleUploadEnabled) {
                    unloadModule(moduleFile);
                }
            } else {
                logger.log(Level.WARNING, "Unexpected operation \"" + operationToString(op) + "\" for goid \"" + goid + "\". Ignoring...");
            }
        }
    }

    /**
     * Called during license change and {@code ServerModuleFile} entity invalidation event.
     * <p/>
     * Get current {@code ServerModuleFile}'s list from DB (i.e. {@link ServerModuleFileManager#findAll()})
     * and compare against known modules cache (i.e. {@link #knownModuleFiles}).<br/>
     * First unload all deleted modules and afterwards load or update any new or updated modules, respectively.
     *
     * @param potentialUpdates    A read-only set of {@code ServerModuleFile} {@code Goid}'s that might have been updated.
     *                            Optional and can be {@code null}, in which case it's assumed that all {@code ServerModuleFile}'s
     *                            from DB have been updated (e.g. during {@link LicenseChangeEvent} event).
     */
    private void scanModules(@Nullable final Set<Goid> potentialUpdates) {
        // first gather all SMFs from DB
        try {
            final Collection<ServerModuleFile> modules = Collections.unmodifiableCollection(Eithers.extract(
                    readOnlyTransaction(new Functions.Nullary<Either<FindException, Collection<ServerModuleFile>>>() {
                        @Override
                        public Either<FindException, Collection<ServerModuleFile>> call() {
                            try {
                                return Either.right(serverModuleFileManager.findAll());
                            } catch (final FindException e) {
                                return Either.left(e);
                            }
                        }
                    })
            ));

            // get the goids from DB modules
            final Set<Goid> allModules = getGoids(modules);

            // first process removed modules
            processRemovedModules(allModules);

            // finally process created or updated modules
            processNewOrUpdatedModules(modules, potentialUpdates != null ? potentialUpdates : allModules);
        } catch (final FindException e) {
            logger.log(Level.WARNING, "Failed to find all Server Module Files: " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
        }
    }

    /**
     *
     * @param dbModuleFileGoids    A Read-Only set of current {@code ServerModuleFile}'s {@code Goid}'s from the DB.
     */
    private void processRemovedModules(@NotNull final Set<Goid> dbModuleFileGoids) {
        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "processRemovedModules...");
        }

        // flag indicating whether module upload is enabled
        final boolean isModuleUploadEnabled = serverModuleFileManager.isModuleUploadEnabled();

        // first loop through known modules from cache i.e. knownModuleFiles
        for (final StagedServerModuleFile module : knownModuleFiles.values()) {
            assert module != null;
            final Goid goid = module.getGoid();
            assert goid != null;

            // if this module is not in the DB, then it has been deleted
            if (!dbModuleFileGoids.contains(goid)) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Processing module that has been removed; goid \"" + module.getGoid() + "\", type \"" + module.getModuleType() + "\", name \"" + module.getName() + "\", stage-file \"" + ObjectUtils.defaultIfNull(module.getStagingFile(), "(null)") + "\"");
                }

                // extract server module file to remove
                final ServerModuleFile moduleToRemove = knownModuleFiles.remove(goid);
                assert moduleToRemove != null;

                // process removed module only if upload is enabled
                if (isModuleUploadEnabled) {
                    unloadModule(module);
                }
            }
        }
    }

    /**
     *
     * @param dbModuleFiles    A Read-Only collection of current {@code ServerModuleFile}'s from the DB.
     * @param potentialUpdates A read-only set of {@code ServerModuleFile} {@code Goid}'s that might have been updated. Required and cannot be {@code null}.
     */
    private void processNewOrUpdatedModules(
            @NotNull final Collection<ServerModuleFile> dbModuleFiles,
            @NotNull final Set<Goid> potentialUpdates
    ) {
        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "processNewOrUpdatedModules...");
        }

        // flag indicating whether module upload is enabled
        final boolean isModuleUploadEnabled = serverModuleFileManager.isModuleUploadEnabled();

        // first loop through modules from DB
        for (final ServerModuleFile module : dbModuleFiles) {
            // extract the module goid first
            final Goid goid = module.getGoid();
            assert goid != null;

            // get the previous module (if any) from known modules cache
            final StagedServerModuleFile prevModule = knownModuleFiles.get(goid);

            // add the module into known modules cache if missing, or update if present
            final StagedServerModuleFile updatedModule = addToKnownModules(module);
            assert goid.equals(updatedModule.getGoid());

            // continue if modules upload is disabled
            if (!isModuleUploadEnabled) {
                continue;
            }

            // modules upload is enabled so process the module

            // test for create or update
            if (prevModule == null) { // this is a create
                // for debug purposes
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Processing module that has been created; goid \"" + updatedModule.getGoid() + "\", type \"" + updatedModule.getModuleType() + "\", name \"" + updatedModule.getName() + "\", stage-file \"" + ObjectUtils.defaultIfNull(updatedModule.getStagingFile(), "(null)") + "\"");
                }

                // module state should be uploaded, as the module if not known to this listener i.e. not found inside knownModuleFiles
                assert ModuleState.UPLOADED.equals(getModuleState(module));

                // load the module only if module upload is enabled
                loadModule(updatedModule);

                // job done move to the next one
                continue;
            }

            // this is a potential update

            // first check if the module name has changed
            // get both module names (from DB and staged)
            final String moduleName = module.getName();
            assert StringUtils.isNotBlank(moduleName);
            final String prevModuleName = prevModule.getName();
            assert StringUtils.isNotBlank(prevModuleName);
            // if they are different
            if (!moduleName.equals(prevModuleName)) {
                updateModule(updatedModule);
            }

            // next check if the module content have changed
            // so get both modules (from DB and staged) digest (sha256)
            final String moduleDigest = module.getModuleSha256();
            assert StringUtils.isNotBlank(moduleDigest);
            final String prevModuleDigest = prevModule.getModuleSha256();
            assert StringUtils.isNotBlank(prevModuleDigest);
            // to make sure content have been updated, check if both digests are different
            if (!moduleDigest.equals(prevModuleDigest)) {
                // for debug purposes
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Processing module which content has been updated...");
                    logger.log(Level.FINE, "Updated module; goid \"" + updatedModule.getGoid() + "\", type \"" + updatedModule.getModuleType() + "\", name \"" + updatedModule.getName() + "\", stage-file \"" + ObjectUtils.defaultIfNull(updatedModule.getStagingFile(), "(null)") + "\"");
                    logger.log(Level.FINE, "Previous module; goid \"" + prevModule.getGoid() + "\", type \"" + prevModule.getModuleType() + "\", name \"" + prevModule.getName() + "\", stage-file \"" + ObjectUtils.defaultIfNull(prevModule.getStagingFile(), "(null)") + "\"");
                }

                // first unload the module
                unloadModule(prevModule);

                // finally load the module with new content
                loadModule(updatedModule);

                // job done move to the next one
                continue;
            }

            // finally we need to check if the module failed to load last time
            // get module state
            final ModuleState moduleState = getModuleState(module);
            // check if this module didn't load last time and load it only if assumed updated.
            if (!ModuleState.LOADED.equals(moduleState) && potentialUpdates.contains(goid)) {
                // for debug purposes
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Processing module that is known but not yet loaded...");
                    logger.log(Level.FINE, "Updated module; goid \"" + updatedModule.getGoid() + "\", type \"" + updatedModule.getModuleType() + "\", name \"" + updatedModule.getName() + "\", stage-file \"" + ObjectUtils.defaultIfNull(updatedModule.getStagingFile(), "(null)") + "\"");
                    logger.log(Level.FINE, "Previous module; goid \"" + prevModule.getGoid() + "\", type \"" + prevModule.getModuleType() + "\", name \"" + prevModule.getName() + "\", stage-file \"" + ObjectUtils.defaultIfNull(prevModule.getStagingFile(), "(null)") + "\"");
                }

                // if the module have just been uploaded then unload previous module first
                if (isModuleLoaded(prevModule)) {
                    unloadModule(prevModule);
                }

                // if module is known but not loaded, then load the module
                loadModule(updatedModule);
            }
        }
    }

    /**
     * Tries to load the specified entity.<br/>
     * First the module content is downloaded into the configured staging folder.<br/>
     * Next the module signature is checked against the downloaded file.<br/>
     * Finally downloaded module file is send to the appropriate modules scanner to load and register its assertions.
     *
     * @param module    the module entity to load.  Required and cannot be {@code null}.
     */
    void loadModule(@NotNull final StagedServerModuleFile module) {
        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Loading module; goid \"" + module.getGoid() + "\", type \"" + module.getModuleType() + "\", name \"" + module.getName() + "\"");
        }

        // audit installation start
        logAndAudit(ServerModuleFileSystemEvent.Action.INSTALLING, module);

        // initial module state unknown
        ModuleState moduleState = null;
        String moduleStateError = null;

        try {
            // download the module data and signature from DB
            final Pair<InputStream, String> moduleDataAndSignature = downloadModuleDataAndSignature(module);
            try {
                // stage module data
                module.setStagingFile(stageModule(module, moduleDataAndSignature.left, getStagingDir()));

                // staging file shouldn't be null after staging
                assert module.getStagingFile() != null;

                // verify module signature
                verifySignature(module, moduleDataAndSignature.right);
            } finally {
                // close module data stream
                ResourceUtils.closeQuietly(moduleDataAndSignature.left);
            }

            // no exceptions => signature verified; module state is ACCEPTED
            moduleState = ModuleState.ACCEPTED;
            // audit module was accepted
            logAndAudit(ServerModuleFileSystemEvent.Action.INSTALL_ACCEPTED, module);

            // send module staging file to modules scanner to be loaded
            final ModuleType moduleType = module.getModuleType();
            if (ModuleType.MODULAR_ASSERTION == moduleType) {
                modularAssertionRegistrar.loadModule(module.getStagingFile(), module);
            } else if (ModuleType.CUSTOM_ASSERTION == moduleType) {
                customAssertionRegistrar.loadModule(module.getStagingFile(), module);
            } else {
                throw new UnsupportedModuleTypeException(moduleType);
            }

            // no exceptions => module loaded; module state is LOADED
            moduleState = ModuleState.LOADED;
            // audit module was successfully loaded
            logAndAudit(ServerModuleFileSystemEvent.Action.LOADED, module);

            // for debug purposes
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Module loaded successfully; goid \"" + module.getGoid() + "\", type \"" + module.getModuleType() + "\", name \"" + module.getName() + "\", staging file-name \"" + ObjectUtils.defaultIfNull(module.getStagingFile(), "(null)") + "\"");
            }
        } catch (final ModuleLoadingException e) {
            // set the module state accordingly
            if (e instanceof ModuleRejectedException) {
                moduleState = ModuleState.REJECTED;
                logAndAudit(ServerModuleFileSystemEvent.Action.INSTALL_REJECTED, module);
            } else {
                moduleState = ModuleState.ERROR;
                moduleStateError = StringUtils.isNotBlank(e.getMessage())
                        ? e.getMessage()
                        : (e instanceof ModuleSignatureException)
                            ? resources.getString("error.signature.verification")
                            : resources.getString("error.module.load.failed");
                // audit installation failure
                logAndAudit(ServerModuleFileSystemEvent.Action.INSTALL_FAIL, module);
            }
            // delete the staging file
            module.deleteStagingFile();
            // log the error
            logger.log(Level.WARNING, "Error while Loading Module \"" + module.getGoid() + "\", name \"" + module.getName() + "\": " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
        } catch (final RuntimeException e) {
            moduleState = ModuleState.ERROR;
            moduleStateError = StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : resources.getString("error.module.load.unhandled");
            // audit installation failure
            logAndAudit(ServerModuleFileSystemEvent.Action.INSTALL_FAIL, module);
            // delete the staging file
            module.deleteStagingFile();
            // log the error
            logger.log(Level.SEVERE, "Unhandled exception while Loading Module \"" + module.getGoid() + "\", name \"" + module.getName() + "\": " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
        } finally {
            // finally set the module state accordingly
            if (StringUtils.isNotBlank(moduleStateError)) {
                //noinspection ConstantConditions
                updateModuleState(module.getGoid(), moduleStateError);
            } else if (moduleState != null && !ModuleState.ERROR.equals(moduleState)) {
                updateModuleState(module.getGoid(), moduleState);
            } else {
                logger.log(Level.WARNING, "Invalid module state; State is \"" + moduleState + "\", error message is \"" + moduleStateError + "\".");
            }
        }
    }

    /**
     * Downloads the module data and signature properties from the Database.
     *
     * @param module    the module to download.  Required and cannot be {@code null}.
     * @return a {@code Pair} of {@code InputStream}, that delivers the module content, and a {@code String},
     * that contains the module signature properties.
     * @throws ModuleDownloadException if an error happens while downloading module data and signature properties.
     * @see com.l7tech.server.module.ServerModuleFileManager#getModuleBytesAsStreamWithSignature(com.l7tech.objectmodel.Goid)
     */
    @NotNull
    Pair<InputStream, String> downloadModuleDataAndSignature(@NotNull final ServerModuleFile module) throws ModuleDownloadException {
        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Downloading Module Data and Signature; goid \"" + module.getGoid() + "\", name \"" + module.getName() + "\"");
        }
        try {
            final Pair<InputStream, String> streamAndSignature = Eithers.extract2(
                    readOnlyTransaction(new Functions.Nullary<Eithers.E2<FindException, ModuleMissingContentsException, Pair<InputStream, String>>>() {
                        @Override
                        public Eithers.E2<FindException, ModuleMissingContentsException, Pair<InputStream, String>> call() {
                            try {
                                final Pair<InputStream, String> streamAndSignature = serverModuleFileManager.getModuleBytesAsStreamWithSignature(module.getGoid());
                                if (streamAndSignature != null)
                                    return Eithers.right2(streamAndSignature);
                                return Eithers.left2_2(new ModuleMissingContentsException());
                            } catch (final FindException e) {
                                return Eithers.left2_1(e);
                            }
                        }
                    }));

            // for debug purposes
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Module Data and Signature Downloaded Successfully; goid \"" + module.getGoid() + "\", name \"" + module.getName() + "\", staging file-name is \"");
            }
            // finally return the module data and signature
            return streamAndSignature;
        } catch (final FindException e) {
            throw new ModuleDownloadException(e);
        }
    }

    /**
     * Saves the module content into a staging folder.<br/>
     * A temporary file will be created in the staging folder, with a deleteOnExit flag set,
     * having a unequally generated file-name (a hex dump of the module goid and a 32bit random number),
     * with extension either .aar or .jar depending on the module type, either
     * {@link ModuleType#MODULAR_ASSERTION} or {@link ModuleType#CUSTOM_ASSERTION} respectively.<br/>
     * On error the file will be deleted.
     *
     * @param module        the module to stage.  Required and cannot be {@code null}.
     * @param dataStream    the module data {@code InputStream}.  Required and cannot be {@code null}.
     * @param stagingDir    the staging folder.  Required and cannot be {@code null}.
     * @return A {@code Pair} of the staging {@code File}, holding the location of the downloaded module content, and a
     * {@code String} containing the Signature information, never {@code null}.
     * @throws ModuleStagingException if an error occurs while staging the module data {@code InputStream}.
     */
    @NotNull
    File stageModule(
            @NotNull final ServerModuleFile module,
            @NotNull final InputStream dataStream,
            @NotNull final File stagingDir
    ) throws ModuleStagingException {
        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Staging module; goid \"" + module.getGoid() + "\", name \"" + module.getName() + "\", stagingDir \"" + stagingDir + "\"");
        }

        File moduleFile = null;
        try {
            // generate unique random module file name inside staging dir, with deleteOnExit flag set
            moduleFile = generateRandomModuleFileName(stagingDir, module);
            // write module data to file
            final OutputStream out = new BufferedOutputStream(new FileOutputStream(moduleFile));
            try {
                IOUtils.copyStream(dataStream, out);
                out.flush();
            } finally {
                ResourceUtils.closeQuietly(out);
            }

            // for debug purposes
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Module Staged Successfully; goid \"" + module.getGoid() + "\", name \"" + module.getName() + "\", staging file-name is \"" + moduleFile.getPath() + "\"");
            }
            // finally return the module file
            return moduleFile;
        } catch (final IOException e) {
            //noinspection ConstantConditions
            if (moduleFile != null) {
                try {
                    FileUtils.delete(moduleFile);
                } catch (final IOException ex) {
                    logger.log(Level.WARNING, "Failed to remove staged module file: " + moduleFile, ExceptionUtils.getDebugException(ex));
                }
            }
            throw new ModuleStagingException(e);
        }
    }

    /**
     * Verify module signature.
     *
     * @param module                the module entity holding the signature.  Required and cannot be {@code null}
     * @param signatureProperties   module signature properties.  Optional and can be {@code null} if module is not signed.
     * @throws ModuleSignatureException when error happens while verifying module signature.
     */
    void verifySignature(
            @NotNull final StagedServerModuleFile module,
            @Nullable final String signatureProperties
    ) throws ModuleSignatureException {
        if (module.getStagingFile() == null) {
            throw new ModuleSignatureException(resources.getString("error.signature.file.missing"));
        }

        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Verifying module signature; goid \"" + module.getGoid() + "\", name \"" + module.getName() + "\", staging file-name \"" + ObjectUtils.defaultIfNull(module.getStagingFile(), "(null)") + "\"");
        }

        InputStream is = null;
        try {
            // get staged file InputStream
            is = new BufferedInputStream(new FileInputStream(module.getStagingFile()));
            // verify staged file signature
            signatureVerifier.verify(is, signatureProperties);

            // for debug purposes
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Module Verified Successfully; goid \"" + module.getGoid() + "\", name \"" + module.getName() + "\", stagingDir \"" + ObjectUtils.defaultIfNull(module.getStagingFile(), "(null)") + "\"");
            }
        } catch (final FileNotFoundException e) {
            // shouldn't happen though
            throw new ModuleSignatureException(resources.getString("error.signature.file.missing"), e);
        } catch (final SignatureException e) {
            throw new ModuleRejectedException(e);
        } finally {
            ResourceUtils.closeQuietly(is);
        }
    }

    /**
     * Update the specified module uploaded to the Database.<br/>
     * Currently the module content cannot be updated, which leaves only the entity name.
     * This method will update the entity name from {@link com.l7tech.server.policy.module.BaseAssertionModule}
     *
     * @param module    the module entity to load.  Required and cannot be {@code null}.
     */
    void updateModule(@NotNull final StagedServerModuleFile module) {
        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Updating module; goid \"" + module.getGoid() + "\", type \"" + module.getModuleType() + "\", name \"" + module.getName() + "\", staged file-name \"" + ObjectUtils.defaultIfNull(module.getStagingFile(), "(null)") + "\"");
        }

        try {
            // unload only if module has a staging file
            if (module.getStagingFile() != null) {
                final ModuleType moduleType = module.getModuleType();
                if (ModuleType.MODULAR_ASSERTION == moduleType) {
                    modularAssertionRegistrar.updateModule(module.getStagingFile(), module);
                } else if (ModuleType.CUSTOM_ASSERTION == moduleType) {
                    customAssertionRegistrar.updateModule(module.getStagingFile(), module);
                } else {
                    throw new UnsupportedModuleTypeException(moduleType);
                }
            }

            logger.log(Level.INFO, "Successfully updated module; goid \"" + module.getGoid() + "\", name \"" + module.getName() + "\", type \"" + module.getModuleType() + "\"");
        } catch (final ModuleLoadingException e) {
            logger.log(Level.WARNING, "Error while Updating Module \"" + module.getGoid() + "\", name \"" + module.getName() + "\": " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
        }
    }

    /**
     * Unloads the specified module entity.<br/>
     * The module entity and its staging file is send to the appropriate modules scanner for unload and unregister its assertions.
     *
     * @param module    the module entity to load.  Required and cannot be {@code null}.
     */
    void unloadModule(@NotNull final StagedServerModuleFile module) {
        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Unloading module; goid \"" + module.getGoid() + "\", type \"" + module.getModuleType() + "\", name \"" + module.getName() + "\", staging file-name \"" + ObjectUtils.defaultIfNull(module.getStagingFile(), "(null)") + "\"");
        }

        // audit un-installation start
        logAndAudit(ServerModuleFileSystemEvent.Action.UNINSTALLING, module);

        try {
            // unload only if module has a staging file
            if (module.getStagingFile() != null) {
                final ModuleType moduleType = module.getModuleType();
                if (ModuleType.MODULAR_ASSERTION == moduleType) {
                    modularAssertionRegistrar.unloadModule(module.getStagingFile(), module);
                } else if (ModuleType.CUSTOM_ASSERTION == moduleType) {
                    customAssertionRegistrar.unloadModule(module.getStagingFile(), module);
                } else {
                    throw new UnsupportedModuleTypeException(moduleType);
                }
            }

            // audit un-installation success
            logAndAudit(ServerModuleFileSystemEvent.Action.UNINSTALL_SUCCESS, module);

            // finally delete module staging file
            module.deleteStagingFile();

            // for debug purposes
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Module unloaded successfully; goid \"" + module.getGoid() + "\", type \"" + module.getModuleType() + "\", name \"" + module.getName() + "\", staging file-name \"" + ObjectUtils.defaultIfNull(module.getStagingFile(), "(null)") + "\"");
            }
        } catch (ModuleLoadingException e) {
            // audit un-installation failure
            logAndAudit(ServerModuleFileSystemEvent.Action.UNINSTALL_FAIL, module);
            logger.log(Level.WARNING, "Error while Unloading Module \"" + module.getGoid() + "\", name \"" + module.getName() + "\": " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
        }
    }

    /**
     * Determine whether the specified {@code ServerModuleFile} is currently loaded or not.
     *
     * @param module    the module entity to check.  Required and cannot be {@code null}.
     * @return {@code true} is loaded, {@code false} otherwise.
     */
    private boolean isModuleLoaded(@NotNull final StagedServerModuleFile module) {
        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Checking if module is loaded; goid \"" + module.getGoid() + "\", type \"" + module.getModuleType() + "\", name \"" + module.getName() + "\", staging file-name \"" + ObjectUtils.defaultIfNull(module.getStagingFile(), "(null)") + "\"");
        }

        // unload only if module has a staging file
        try {
            if (module.getStagingFile() != null) {
                final ModuleType moduleType = module.getModuleType();
                if (ModuleType.MODULAR_ASSERTION == moduleType) {
                    return modularAssertionRegistrar.isModuleLoaded(module.getStagingFile(), module);
                } else if (ModuleType.CUSTOM_ASSERTION == moduleType) {
                    return customAssertionRegistrar.isModuleLoaded(module.getStagingFile(), module);
                } else {
                    throw new UnsupportedModuleTypeException(moduleType);
                }
            }
        } catch (final ModuleLoadingException e) {
            logger.log(Level.WARNING, "Cannot determine if module is loaded; goid \"" + module.getGoid() + "\", name \"" + module.getName() + "\": " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
        }

        return false;
    }

    /**
     * Utility method for adding the specified {@code moduleFile} into the {@link #knownModuleFiles known modules cache}.
     *
     * @param moduleFile    the module to add.  Required and cannot be {@code null}.
     * @return a {@link com.l7tech.server.module.ServerModuleFileListener.StagedServerModuleFile copy} of the specified {@code moduleFile}.  Never {@code null}.
     */
    @NotNull
    private StagedServerModuleFile addToKnownModules(@NotNull final ServerModuleFile moduleFile) {
        final StagedServerModuleFile newModule = new StagedServerModuleFile(moduleFile);
        final StagedServerModuleFile prevModule = knownModuleFiles.put(moduleFile.getGoid(), newModule);
        if (prevModule != null) {
            // this is an update so update the staging file of the new ServerModuleFile object.
            newModule.setStagingFile(prevModule.getStagingFile());
        }

        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Added/Updating known cache, goid \"" + moduleFile.getGoid() + "\", type \"" + moduleFile.getModuleType() + "\", name \"" + moduleFile.getName() + "\"");
        }

        return newModule;
    }

    /**
     * Utility method for removing a module, specified with the {@code goid}, from the {@link #knownModuleFiles known modules cache}.<br/>
     * Will try toIf no module with {@code goid} is known then a
     *
     * @param goid    The OID of the server module file.  Required and cannot be {@code null}.
     * @return the module specified with the {@code goid}.
     * @throws FindException If no module with {@code goid} is known.
     */
    @NotNull
    private StagedServerModuleFile removeFromKnownModules(@NotNull final Goid goid) throws FindException {
        final StagedServerModuleFile moduleFile = knownModuleFiles.remove(goid);
        if (moduleFile == null) {
            throw new FindException("Module with goid \"" + goid + "\" doesn't exist in the known modules map");
        }

        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Removed from known cache, goid \"" + moduleFile.getGoid() + "\", type \"" + moduleFile.getModuleType() + "\", name \"" + moduleFile.getName() + "\", staged file-name \"" + ObjectUtils.defaultIfNull(moduleFile.getStagingFile(), "(null)") + "\"");
        }

        return moduleFile;
    }

    /**
     * Utility function to wrap {@code callback} into database transaction if available i.e. if {@link #transactionManager} is not {@code null}.<br/>
     * Default propagation is {@link org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     *
     * @param callback    the callback to execute.
     * @param readOnly    set whether to optimize as read-only transaction.
     */
    private <R> R transactional(@NotNull final Functions.Nullary<R> callback, final boolean readOnly) {
        if (transactionManager != null) {
            // default propagation REQUIRED
            final TransactionTemplate tt = new TransactionTemplate(transactionManager);
            tt.setReadOnly(readOnly);
            try {
                return tt.execute(new TransactionCallback<R>() {
                    @Override
                    public R doInTransaction(final TransactionStatus transactionStatus) {
                        try {
                            final R result = callback.call();
                            if (!isSuccess(result)) {
                                transactionStatus.setRollbackOnly();
                            }
                            return result;
                        } catch (final Throwable e) {
                            transactionStatus.setRollbackOnly();
                            throw new RuntimeException(e);
                        }
                    }
                });
            } catch (final TransactionException e) {
                throw new RuntimeException(e);
            }
        }
        return callback.call();
    }

    /**
     * Utility method for starting a read-only optimize transaction.
     *
     * @param callback    the callback to execute.
     * @see #transactional(com.l7tech.util.Functions.Nullary, boolean)
     */
    private <R> R readOnlyTransaction(@NotNull final Functions.Nullary<R> callback) {
        return transactional(callback, true);
    }

    /**
     * Utility method for starting a read-write transaction.
     *
     * @param callback    the callback to execute.
     * @see #transactional(com.l7tech.util.Functions.Nullary, boolean)
     */
    private <R> R readWriteTransaction(@NotNull final Functions.Nullary<R> callback) {
        return transactional(callback, false);
    }

    /**
     * Utility function for getting module state for the current cluster node.<br/>
     * Never returns {@code null}, if the current cluster node doesn't contain any state record,
     * then {@link ModuleState#UPLOADED} is returned.
     * <p/>
     * Note that this method doesn't require DB transaction i.e. there aren't going to be made any DB calls.
     *
     * @param moduleFile    the {@link ServerModuleFile} which state to extract
     * @return Specified {@code module} current cluster node {@link ModuleState state}, if any, or {@link ModuleState#UPLOADED} otherwise.
     */
    @NotNull
    ModuleState getModuleState(@NotNull final ServerModuleFile moduleFile) {
        // currently no transaction is needed
        // if that changes consider using readOnlyTransaction(...)
        final ServerModuleFileState state = serverModuleFileManager.findStateForCurrentNode(moduleFile);
        return (state != null) ? state.getState() : ModuleState.UPLOADED;
    }

    /**
     * Generate a random filename for the specified server module file.
     * Atomically creates a new, empty file, using the random generated filename, with deleteOnExit flag set.<br/>
     * Currently the file name will be a hex dump of the goid plus a 32bit random number.
     *
     * @param dir           the parent folder.  Required and cannot be {@code null}
     * @param moduleFile    the specified server module file.  Required and cannot be {@code null}
     * @return a {@code String} containing the module file-name.  Never {@code null}.
     * @throws ModuleUniqueFileNameException when failed to generate module unique filename, inside staging folder,
     * after ten attempts.
     */
    @NotNull
    private static File generateRandomModuleFileName(
            @NotNull final File dir,
            @NotNull final ServerModuleFile moduleFile
    ) throws ModuleUniqueFileNameException {
        // get module goid
        final byte[] goidBytes = moduleFile.getGoid().getBytes();
        // byte array for random bytes
        final byte[] rndBytes = new byte[8];
        // resulting byte array
        final byte[] bytes = new byte[goidBytes.length + rndBytes.length];

        // copy goid first
        System.arraycopy(goidBytes, 0, bytes, 0, goidBytes.length);

        // try 10 times
        File file = null;
        for (int i = 0; i < 10; ++i) {
            // copy random bytes first
            RandomUtil.nextBytes(rndBytes);
            System.arraycopy(rndBytes, 0, bytes, goidBytes.length, rndBytes.length);
            final String fileName = HexUtils.hexDump(bytes) + getModuleExtension(moduleFile);

            // if newly generated file name doesn't exist return
            final File tmpFile = new File(dir, fileName);
            if (!tmpFile.exists()) {
                file = tmpFile;
                break;
            }
        }

        // if module file is unique create it and return
        if (file != null) {
            try {
                if (!file.createNewFile()) {
                    throw new ModuleUniqueFileNameException(resources.getString("error.staging.create.unique.module.file"));
                }
            } catch (final IOException e) {
                throw new ModuleUniqueFileNameException(resources.getString("error.staging.create.unique.module.file"), e);
            }
            file.deleteOnExit();
            return file;
        }

        // otherwise throw
        throw new ModuleUniqueFileNameException(resources.getString("error.staging.unique.module.file.name"));
    }

    /**
     * Get the specified server module file extension based on the module {@link ModuleType type}.
     *
     * @param serverModuleFile    the specified server module file.  Required and cannot be {@code null}
     * @return a {@code String} containing the module extension based on the module {@link ModuleType type}.  Never {@code null}.
     */
    @NotNull
    private static String getModuleExtension(@NotNull final ServerModuleFile serverModuleFile) {
        return ModuleType.MODULAR_ASSERTION.equals(serverModuleFile.getModuleType()) ? ".aar" : ".jar";
    }

    /**
     * Utility method to update module state error message for the current cluster node.<br/>
     * Used to indicate an error happen while staging the module.
     *
     * @param moduleGoid      the module GOID.  Required and cannot be {@code null}
     * @param errorMessage    error message to set.  Required and cannot be {@code null}
     */
    void updateModuleState(
            @NotNull final Goid moduleGoid,
            @NotNull final String errorMessage
    ) {
        readWriteTransaction(new Functions.Nullary<Void>() {
            @Override
            public Void call() {
                // for debug purposes
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Updating module state error message for goid \"" + moduleGoid + "\", error-message \"" + errorMessage + "\"");
                }

                try {
                    serverModuleFileManager.updateState(moduleGoid, errorMessage);
                } catch (final UpdateException e) {
                    logger.log(Level.WARNING, "Failed to update module \"" + moduleGoid + "\" state error message: " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
                }
                return null;
            }
        });
    }

    /**
     * Utility method to update module state for the current cluster node.<br/>
     * Note that default transaction propagation is REQUIRED, therefore executing this inside transaction is fine, as
     * the existing transaction will be used.<br/>
     * If the above is changed consider modifying the logic.
     *
     * @param moduleGoid    the module GOID.  Required and cannot be {@code null}
     * @param state         the new state.  Required and cannot be {@code null}
     */
    void updateModuleState(
            @NotNull final Goid moduleGoid,
            @NotNull final ModuleState state
    ) {
        readWriteTransaction(new Functions.Nullary<Void>() {
            @Override
            public Void call() {
                // for debug purposes
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Updating module state for goid \"" + moduleGoid + "\", state \"" + state + "\"");
                }

                try {
                    serverModuleFileManager.updateState(moduleGoid, state);
                } catch (final UpdateException e) {
                    logger.log(Level.WARNING, "Failed to update module \"" + moduleGoid + "\" state!: " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
                }
                return null;
            }
        });
    }

    /**
     * Utility method for returning a string representation of the specified Database Operation.
     *
     * @param op    Database Operation ({@link EntityInvalidationEvent#CREATE CREATE}, {@link EntityInvalidationEvent#UPDATE UPDATE}
     *              or {@link EntityInvalidationEvent#DELETE DELETE})
     * @return A string representation of the Database Operation.  Never {@code null}
     */
    @NotNull
    private static String operationToString(char op) {
        if (op == EntityInvalidationEvent.CREATE) {
            return EVENT_OPERATION_CREATE_UFN;
        } else if (op == EntityInvalidationEvent.UPDATE) {
            return EVENT_OPERATION_UPDATE_UFN;
        } else if (op == EntityInvalidationEvent.DELETE) {
            return EVENT_OPERATION_DELETE_UFN;
        }
        return String.valueOf(op);
    }

    /**
     * Utility method for converting the specified {@code ServerModuleFile} collection into a set of {@code Goid}'s.
     *
     * @param modules    A read-only collection of {@code ServerModuleFile}'s.  Required and cannot be {@code null}.
     * @return A Read-ony set of all {@code Goid}'s from the specified {@code modules} collections.  Never {@code null}.
     */
    @NotNull
    private static Set<Goid> getGoids(@NotNull final Collection<ServerModuleFile> modules) {
        final Set<Goid> goids = new HashSet<>();
        for (final ServerModuleFile module : modules) {
            assert module.getGoid() != null;
            goids.add(module.getGoid());
        }
        return Collections.unmodifiableSet(goids);
    }

    @Override
    public void setApplicationContext(final ApplicationContext context) throws BeansException {
        this.applicationContext = context;
    }

    /**
     * Publish the specified audit event.
     */
    private void logAndAudit(@NotNull final ServerModuleFileSystemEvent event) {
        if (applicationContext != null) {
            applicationContext.publishEvent(event);
        }
    }

    /**
     * Convenient method for creating audit event for the specified {@code action} and {@code moduleFile}
     *
     * @param action        the module file action to audit.
     * @param moduleFile    the module file.
     */
    private void logAndAudit(@NotNull final ServerModuleFileSystemEvent.Action action, @NotNull final ServerModuleFile moduleFile) {
        logAndAudit(ServerModuleFileSystemEvent.createSystemEvent(this, action, moduleFile));
    }

    /**
     * Determine whether the specified directory is writable or not.
     *
     * @param dir    Directory to check.  Required and cannot be {@code null}
     * @return {@code true} if and only if the {@code dir} exists, is directory and the application is allowed to write to the directory; {@code false} otherwise.
     */
    private static boolean isDirectoryWritable(final File dir) {
        if (dir == null) throw new IllegalArgumentException("dir cannot be null");
        boolean isWritable = false;
        try {
            isWritable = dir.exists() && dir.isDirectory() && dir.canWrite();
        } catch (final Throwable ex) {
            logger.log(Level.WARNING, "Error while determining whether directory \"" + dir + "\" is writable: " + ExceptionUtils.getMessageWithCause(ex), ExceptionUtils.getDebugException(ex));
        }
        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "isDirectoryWritable(" + dir.getPath() + "): " + isWritable);
        }
        return isWritable;
    }

    /**
     * Lazy get configured Staging Directory.<br/>
     * This method ensures that the Staging Directory is properly configured, exists in the File System and is
     * writable by the Gateway process.
     *
     * @return The Configured Staging Directory.  Never {@code null}
     * @throws ConfigException if Staging Directory is not configured, doesn't exist or is not writable by the Gateway process.
     * Additionally if an I/O error occurs while constructing directory canonical path.
     */
    @NotNull
    File getStagingDir() throws ConfigException {
        if (this.stagingRootDir == null) {
            final String rootStagingPath = config.getProperty(ServerConfigParams.PARAM_SERVER_MODULE_FILE_STAGING_FOLDER, null);
            if (StringUtils.isBlank(rootStagingPath)) {
                throw new ConfigException(resources.getString("error.staging.dir.empty.or.not.configured"));
            }
            final File rootStagingDir;
            try {
                rootStagingDir = new File(rootStagingPath.trim()).getCanonicalFile();
            } catch (final IOException e) {
                throw new ConfigException(resources.getString("error.staging.dir.invalid"), e);
            }
            if (!(rootStagingDir.exists() && rootStagingDir.isDirectory())) {
                throw new ConfigException(MessageFormat.format(resources.getString("error.staging.dir.does.not.exist"), rootStagingDir));
            }
            if (!isDirectoryWritable(rootStagingDir)) {
                throw new ConfigException(MessageFormat.format(resources.getString("error.staging.dir.not.writable"), rootStagingDir));
            }
            this.stagingRootDir = rootStagingDir;
        }
        return this.stagingRootDir;
    }


    // --- Exceptions ---

    /**
     * Indicates Invalid Server Module File, unsupported type.
     */
    @SuppressWarnings("serial")
    static class UnsupportedModuleTypeException extends ModuleLoadingException {
        UnsupportedModuleTypeException(@NotNull final ModuleType moduleType) {
            super(MessageFormat.format(resources.getString("error.unsupported.module.type"), moduleType.toString()));
        }
    }

    /**
     * Indicates Configuration error, could be due to misconstrued staging folder.
     */
    @SuppressWarnings("serial")
    static class ConfigException extends ModuleLoadingException {
        ConfigException(@NotNull final String message) {
            super(message);
        }
        ConfigException(@NotNull final String message, @NotNull final Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Indicates an error while downloading the module content and signature properties from the DB.
     */
    @SuppressWarnings("serial")
    static class ModuleDownloadException extends ModuleLoadingException {
        ModuleDownloadException(@NotNull final Throwable cause) {
            super(resources.getString("error.module.download.fail"), cause);
        }
        ModuleDownloadException(@NotNull final String message) {
            super(message);
        }
    }

    /**
     * Indicates Invalid Server Module File, content is missing.
     */
    @SuppressWarnings("serial")
    static class ModuleMissingContentsException extends ModuleDownloadException {
        ModuleMissingContentsException() {
            super(resources.getString("error.invalid.module.bytes"));
        }
    }

    /**
     * Indicates an error while saving the module content into the staging folder.
     */
    @SuppressWarnings("serial")
    static class ModuleStagingException extends ModuleLoadingException {
        ModuleStagingException(@NotNull final Throwable cause) {
            super(resources.getString("error.module.staging.fail"), cause);
        }
        ModuleStagingException(@NotNull final String message) {
            super(message);
        }
        ModuleStagingException(@NotNull final String message, @NotNull final Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Failed to generate module unique filename inside staging folder.
     */
    @SuppressWarnings("serial")
    static class ModuleUniqueFileNameException extends ModuleStagingException {
        ModuleUniqueFileNameException(@NotNull final String message) {
            super(message);
        }
        ModuleUniqueFileNameException(@NotNull final String message, @NotNull final Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Indicates an error happen while Verifying module signature.
     */
    @SuppressWarnings("serial")
    static class ModuleSignatureException extends ModuleLoadingException {
        ModuleSignatureException(@NotNull final String message) {
            super(message);
        }
        ModuleSignatureException(@NotNull final String message, @NotNull final Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Indicates Module have been rejected.
     */
    @SuppressWarnings("serial")
    static class ModuleRejectedException extends ModuleSignatureException {
        ModuleRejectedException() {
            super(resources.getString("error.signature.rejected"));
        }
        ModuleRejectedException(@NotNull final Throwable cause) {
            super(
                    StringUtils.isNotBlank(ExceptionUtils.getMessage(cause))
                            ? MessageFormat.format("{0}: {1}", resources.getString("error.signature.rejected"), ExceptionUtils.getMessage(cause))
                            : resources.getString("error.signature.rejected"),
                    cause
            );
        }
    }
}
