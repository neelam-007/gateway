package com.l7tech.server.module;

import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.module.ModuleState;
import com.l7tech.gateway.common.module.ModuleType;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.common.module.ServerModuleFileState;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.LicenseChangeEvent;
import com.l7tech.server.event.system.ServerModuleFileSystemEvent;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.policy.module.AssertionModuleRegistrationEvent;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server Module File event listener.<br/>
 * Listens for {@link ServerModuleFile} {@link EntityInvalidationEvent}'s.
 */
public abstract class ServerModuleFileListener implements ApplicationContextAware {
    private static final Logger logger = Logger.getLogger(ServerModuleFileListener.class.getName());

    protected static final String MODULAR_MODULES_DIR = "modular";
    protected static final String CUSTOM_MODULES_DIR = "custom";

    // Logger friendly names for EntityInvalidationEvent operation
    private static final String EVENT_OPERATION_CREATE_UFN = "CREATE";
    private static final String EVENT_OPERATION_UPDATE_UFN = "UPDATE";
    private static final String EVENT_OPERATION_DELETE_UFN = "DELETE";

    @NotNull protected final ServerModuleFileManager serverModuleFileManager;
    protected final PlatformTransactionManager transactionManager;
    @NotNull protected final Config config;
    @NotNull protected final ServerAssertionRegistry modularAssertionRegistrar;
    @NotNull protected final CustomAssertionsRegistrar customAssertionRegistrar;
    private ApplicationContext applicationContext;

    private volatile boolean gatewayStarted = false;

    /**
     * Contains cached server module files copies, having only meta-data (i.e. without bytes and state).
     */
    @NotNull protected final Map<Goid, ServerModuleFile> knownModuleFiles = new ConcurrentHashMap<>();

    /**
     * Default constructor.
     *
     * @param eventProxy                 {@link ApplicationEventProxy} needed to register for listening application events.
     * @param serverModuleFileManager    {@link ServerModuleFile} entity manager.
     * @param transactionManager         Transaction manager.
     * @param config                     Server Config.
     */
    public ServerModuleFileListener(
            @NotNull final ApplicationEventProxy eventProxy,
            @NotNull final ServerModuleFileManager serverModuleFileManager,
            final PlatformTransactionManager transactionManager,
            @NotNull final Config config,
            @NotNull final ServerAssertionRegistry modularAssertionRegistrar,
            @NotNull final CustomAssertionsRegistrar customAssertionRegistrar
    ) {
        this.serverModuleFileManager = serverModuleFileManager;
        this.transactionManager = transactionManager;
        this.config = config;
        this.modularAssertionRegistrar = modularAssertionRegistrar;
        this.customAssertionRegistrar = customAssertionRegistrar;

        eventProxy.addApplicationListener(new ApplicationListener() {
            @Override
            public void onApplicationEvent(final ApplicationEvent event) {
                handleEvent(event);
            }
        });
    }

    /**
     * Notify when a new module has been successfully uploaded or existing one has been updated.
     * <p/>
     * This method is executed within transaction making sure all {@code moduleFile} properties can be lazy fetched.<br/>
     * The transaction is created with {@link org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * propagation meaning that {@link #transactionIfAvailable(Runnable)} can be used.
     *
     * @param moduleFile    The newly created or updated {@link ServerModuleFile server module file}.  Required and cannot be {@code null}.
     * @throws ModuleStagingException When an error occurs while processing (e.g. staging) the module.
     */
    protected abstract void onModuleChanged(@NotNull final ServerModuleFile moduleFile) throws ModuleStagingException;

    /**
     * Notify when the module, specified with the {@code moduleFileName}, has been successfully removed from the database.<br/>
     * Use this event to remove the module from modules and staging dirs.
     * <p/>
     * Note that this method is not wrapped inside a transaction as {@code moduleFile} is a copy of the persisted entity,
     * having no state and data information.
     *
     * @param moduleFile    The deleted {@link ServerModuleFile server module file}.  Required and cannot be {@code null}.
     * @throws ModuleStagingException When an error occurs while processing (e.g. staging) deleted module.
     */
    protected abstract void onModuleDeleted(@NotNull final ServerModuleFile moduleFile) throws ModuleStagingException;

    /**
     * Handle Application events, specifically {@link com.l7tech.server.event.EntityInvalidationEvent} for
     * {@link com.l7tech.gateway.common.module.ServerModuleFile} entity.
     *
     * @param event    the {@link ApplicationEvent event} that occurred.
     */
    private void handleEvent(@NotNull final ApplicationEvent event) {
        try {
            // once the Gateway is up and running, create staging folders for custom and modular modules.
            if (event instanceof Started) {
                gatewayStarted = true;
                knownModuleFiles.clear();
                initModules();
                return;
            } else //noinspection StatementWithEmptyBody
                if (event instanceof LicenseChangeEvent) {
                // TODO: handle license here, once server module files are licensable
            }

            // do not process any events until SSG is started.
            if (!gatewayStarted) {
                return;
            }

            if (event instanceof EntityInvalidationEvent) {
                // we are only interested in ServerModuleFile entity changes
                final EntityInvalidationEvent entityInvalidationEvent = (EntityInvalidationEvent) event;
                if (!ServerModuleFile.class.equals(entityInvalidationEvent.getEntityClass())) {
                    return;
                }

                final Goid[] goids = entityInvalidationEvent.getEntityIds();
                final char[] ops = entityInvalidationEvent.getEntityOperations();

                // for debug purposes
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "EntityInvalidationEvent ops \"" + Arrays.toString(ops) + "\", goids \"" + Arrays.toString(goids) + "\"");
                }

                for (int ix = 0; ix < goids.length; ++ix) {
                    final char op = ops[ix];
                    @NotNull final Goid goid = goids[ix];

                    // for debug purposes
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Processing operation \"" + operationToString(op) + "\", for goid \"" + goid + "\"");
                    }

                    // on error continue with the next operation and log the failure
                    if (op == EntityInvalidationEvent.CREATE || op == EntityInvalidationEvent.UPDATE) {
                        transactionIfAvailable(new Runnable() {
                            @Override
                            public void run() {
                                // extract server module file
                                final ServerModuleFile moduleFile;
                                try {
                                    moduleFile = serverModuleFileManager.findByPrimaryKey(goid);
                                } catch (final FindException e) {
                                    logger.log(Level.WARNING, "Failed to find Server Module File with \"" + goid + "\", operation was \"" + operationToString(op) + "\": " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
                                    return;
                                }

                                try {
                                    if (moduleFile != null) {
                                        // add it to the known modules cache
                                        addToKnownModules(moduleFile);
                                        // process new or updated module only if upload is enabled
                                        if (serverModuleFileManager.isModuleUploadEnabled()) {
                                            onModuleChanged(moduleFile);
                                        }
                                    } else {
                                        // non-existent module goid, remove it from known cache
                                        knownModuleFiles.remove(goid);
                                    }
                                } catch (final ModuleStagingException e) {
                                    updateModuleState(goid, e.getMessage());
                                    // audit installation failure
                                    logAndAudit(ServerModuleFileSystemEvent.Action.INSTALL_FAIL, moduleFile);
                                    logger.log(Level.WARNING, "Error while Installing Module \"" + goid + "\", operation was \"" + operationToString(op) + "\": " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
                                }
                            }
                        });
                    } else if (op == EntityInvalidationEvent.DELETE) {
                        // extract server module file
                        final ServerModuleFile moduleFile;
                        try {
                            moduleFile = removeFromKnownModules(goid);
                        } catch (FindException e) {
                            // for debug purposes should be ignored otherwise
                            if (logger.isLoggable(Level.FINE)) {
                                logger.log(Level.FINE, "Unable to find Server Module File with \"" + goid + "\", from known modules cache. Ignoring Module Delete Event.", e);
                            }
                            return;
                        }

                        try {
                            // process removed module only if upload is enabled
                            if (serverModuleFileManager.isModuleUploadEnabled()) {
                                onModuleDeleted(moduleFile);
                            }
                        } catch (ModuleStagingException e) {
                            // audit un-installation failure
                            logAndAudit(ServerModuleFileSystemEvent.Action.UNINSTALL_FAIL, moduleFile);
                            logger.log(Level.WARNING, "Error while Uninstalling Module \"" + goid + "\": " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
                        }
                    } else {
                        logger.log(Level.WARNING, "Unexpected operation \"" + operationToString(op) + "\" for goid \"" + goid + "\". Ignoring...");
                    }
                }
            } else if (event instanceof AssertionModuleRegistrationEvent && serverModuleFileManager.isModuleUploadEnabled()) {
                final AssertionModuleRegistrationEvent registrationEvent = (AssertionModuleRegistrationEvent)event;
                if (registrationEvent.getModule().isLeft()) {
                    // modular assertion
                    processLoadedModule(registrationEvent.getModule().left().getName(), ModuleType.MODULAR_ASSERTION);
                } else if (registrationEvent.getModule().isRight()) {
                    // custom assertion
                    processLoadedModule(registrationEvent.getModule().right().getName(), ModuleType.CUSTOM_ASSERTION);
                } else {
                    logger.log(Level.WARNING, "Module Registration Event is not for Modular nor Custom Assertion module");
                }
            }
        } catch (final Throwable e) {
            logger.log(Level.SEVERE, "Unhandled exception while handling Server Module Files events: " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
        }
    }

    /**
     * Process currently loaded module.<br/>
     * Finds the module specified with the file-name and type from the known modules cache and update its state to {@link }
     *
     * @param moduleName    the module file-name.  Required and cannot be {@link null}
     * @param moduleType    the module type. Required and cannot be {@link null}
     */
    private void processLoadedModule(
            @NotNull final String moduleName,
            @NotNull final ModuleType moduleType
    ) {
        // do not process Module Registration Event if upload is disabled
        if (!serverModuleFileManager.isModuleUploadEnabled()) {
            return;
        }

        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Process Module Registration Event; module name \"" + moduleName + "\", type \"" + moduleType + "\"");
        }

        // first try to locate in our modules cache
        Goid moduleGoid = null;
        for (final ServerModuleFile moduleFile : knownModuleFiles.values()) {
            if (moduleType.equals(moduleFile.getModuleType()) && StringUtils.equals(moduleName, moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME))) {
                moduleGoid = moduleFile.getGoid();
                break;
            }
        }
        // todo: if not found in modules cache perhaps try to find it in DB
        // todo: drawback is that for any arbitrary module being loaded, we'll search the DB

        if (moduleGoid != null) {
            updateModuleState(moduleGoid, ModuleState.LOADED);

            // audit module loaded
            logAndAudit(ServerModuleFileSystemEvent.createLoadedSystemEvent(this, moduleType, moduleName));

        }
    }

    /**
     * Utility method for adding the specified {@code moduleFile} into the {@link #knownModuleFiles known modules cache}.
     *
     * @param moduleFile    the module to add.  Required and cannot be {@code null}.
     */
    private void addToKnownModules(@NotNull final ServerModuleFile moduleFile) {
        final ServerModuleFile copy = new ServerModuleFile();
        copy.copyFrom(moduleFile, false, true, false);
        knownModuleFiles.put(moduleFile.getGoid(), copy);

        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Added/Updating known cache, goid \"" + moduleFile.getGoid() + "\", type \"" + moduleFile.getModuleType() + "\", name \"" + moduleFile.getName() + "\", file-name \"" + moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME) + "\"");
        }
    }

    /**
     * Utility method for removing a module, specified with the {@code goid}, from the {@link #knownModuleFiles known modules cache}.<br/>
     * Will try toIf no module with {@code goid} is known then a
     *
     * @param goid    The OID of the server module file to update the state.  Required and cannot be {@code null}.
     * @return the module specified with the {@code goid}.
     * @throws FindException If no module with {@code goid} is known.
     */
    @NotNull
    private ServerModuleFile removeFromKnownModules(@NotNull final Goid goid) throws FindException {
        final ServerModuleFile moduleFile = knownModuleFiles.remove(goid);
        if (moduleFile == null) {
            throw new FindException("Module with goid \"" + goid + "\" doesn't exist in the known modules map");
        }

        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Removed from known cache, goid \"" + moduleFile.getGoid() + "\", type \"" + moduleFile.getModuleType() + "\", name \"" + moduleFile.getName() + "\", file-name \"" + moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME) + "\"");
        }

        return moduleFile;
    }

    /**
     * Utility function to wrap {@code callback} into transaction if available i.e. if {@link #transactionManager} is not {@code null}.<br/>
     * Default propagation is {@link org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * and readOnly is {@code false}
     *
     * @param callback    the callback to execute.
     */
    protected void transactionIfAvailable(@NotNull final Runnable callback) {
        if (transactionManager != null) {
            // default propagation REQUIRED
            // default readOnly false
            final TransactionTemplate tt = new TransactionTemplate(transactionManager);
            tt.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(final TransactionStatus status) {
                    callback.run();
                }
            });
        } else {
            callback.run();
        }
    }

    /**
     * Retrieve the root staging folder.  The root staging folder contains separate folders for Modular and Custom Assertion Modules.
     *
     * @return the value of {@link com.l7tech.server.ServerConfigParams#PARAM_SERVER_MODULE_FILE_STAGING_FOLDER} cluster wide property.
     * Default is <code>${ssg.var}/modstaging</code>.
     */
    @SuppressWarnings("SpellCheckingInspection")
    protected String getRootStagingPath() {
        return config.getProperty(ServerConfigParams.PARAM_SERVER_MODULE_FILE_STAGING_FOLDER, null);
    }

    /**
     * Go through all server module file entities, try to process them (i.e. stage or deploy) and add each module into the
     * {@link #knownModuleFiles known module cache}.<br/>
     * Note that this method is executed into a transaction.
     */
    private void initModules() {

        // for debug purposes
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "initModules...");
        }

        transactionIfAvailable(new Runnable() {
            @Override
            public void run() {
                try {
                    for (final ServerModuleFile moduleFile : serverModuleFileManager.findAll()) {
                        addToKnownModules(moduleFile);
                        if (serverModuleFileManager.isModuleUploadEnabled()) {
                            final ModuleState moduleState = getModuleState(moduleFile);
                            if (ModuleState.LOADED == moduleState) {
                                continue;  // if already loaded skip processing
                            }
                            try {
                                // if already loaded do not process, set the state directly to LOADED.
                                if (isModuleLoaded(moduleFile)) {
                                    updateModuleState(moduleFile.getGoid(), ModuleState.LOADED);
                                } else {
                                    onModuleChanged(moduleFile);
                                }
                            } catch (final ModuleStagingException e) {
                                updateModuleState(moduleFile.getGoid(), e.getMessage());
                                logger.log(Level.WARNING, "Failed to process initial module with goid \"" + moduleFile.getGoid() + "\": " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
                            }
                        }
                    }
                } catch (FindException e) {
                    logger.log(Level.WARNING, "Failed to find all Server Module Files: " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
                }
            }
        });
    }

    /**
     * Utility function for getting module state for the current cluster node.<br/>
     * Never returns {@code null}, if the current cluster node doesn't contain any state record,
     * then {@link ModuleState#UPLOADED} is returned.
     *
     * @param moduleFile    the {@link ServerModuleFile} which state to extract
     * @return Specified {@code module} current cluster node {@link ModuleState state}, if any, or {@link ModuleState#UPLOADED} otherwise.
     */
    @NotNull
    private ModuleState getModuleState(@NotNull final ServerModuleFile moduleFile) {
        final ServerModuleFileState state = serverModuleFileManager.findStateForCurrentNode(moduleFile);
        return (state != null) ? state.getState() : ModuleState.UPLOADED;
    }

    /**
     * Utility method to update module state error message for the current cluster node.<br/>
     * Used to indicate an error happen while staging the module.
     *
     * @param moduleGoid      the module GOID.  Required and cannot be {@code null}
     * @param errorMessage    error message to set.  Required and cannot be {@code null}
     */
    private void updateModuleState(
            @NotNull final Goid moduleGoid,
            @NotNull final String errorMessage
    ) {
        transactionIfAvailable(new Runnable() {
            @Override
            public void run() {
                // for debug purposes
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Updating module state error message for goid \"" + moduleGoid + "\", error-message \"" + errorMessage + "\"");
                }

                try {
                    serverModuleFileManager.updateState(moduleGoid, errorMessage);
                } catch (final UpdateException e) {
                    logger.log(Level.WARNING, "Failed to update module \"" + moduleGoid + "\" state error message: " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
                }
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
    protected void updateModuleState(
            @NotNull final Goid moduleGoid,
            @NotNull final ModuleState state
    ) {
        transactionIfAvailable(new Runnable() {
            @Override
            public void run() {
                // for debug purposes
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Updating module state for goid \"" + moduleGoid + "\", state \"" + state + "\"");
                }

                try {
                    serverModuleFileManager.updateState(moduleGoid, state);
                } catch (final UpdateException e) {
                    logger.log(Level.WARNING, "Failed to update module \"" + moduleGoid + "\" state!: " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
                }
            }
        });
    }

    /**
     * Determine whether the specified module is already loaded or not.<br/>
     *
     * @param moduleFile    module to check.
     */
    protected boolean isModuleLoaded(@NotNull final ServerModuleFile moduleFile) {
        if (ModuleType.MODULAR_ASSERTION == moduleFile.getModuleType()) {
            return modularAssertionRegistrar.isServerModuleFileLoaded(moduleFile);
        } else if (ModuleType.CUSTOM_ASSERTION == moduleFile.getModuleType()) {
            return customAssertionRegistrar.isServerModuleFileLoaded(moduleFile);
        }
        return false;
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

    @Override
    public void setApplicationContext(final ApplicationContext context) throws BeansException {
        this.applicationContext = context;
    }

    protected void logAndAudit(@NotNull final ServerModuleFileSystemEvent event) {
        if (applicationContext != null) {
            applicationContext.publishEvent(event);
        }
    }

    protected void logAndAudit(@NotNull final ServerModuleFileSystemEvent.Action action, @NotNull final ServerModuleFile moduleFile) {
        logAndAudit(ServerModuleFileSystemEvent.createSystemEvent(this, action, moduleFile));
    }
}
