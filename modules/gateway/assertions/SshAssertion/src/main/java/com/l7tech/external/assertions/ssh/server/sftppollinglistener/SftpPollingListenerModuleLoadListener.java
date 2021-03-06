package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

import com.l7tech.external.assertions.ssh.SftpPollingListenerConstants;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SecurePasswordEntityHeader;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.search.DependencyProcessorRegistry;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.processors.DependencyFinder;
import com.l7tech.server.search.processors.DependencyProcessor;
import com.l7tech.server.search.processors.DependencyProcessorUtils;
import com.l7tech.server.util.Injector;
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.GoidUpgradeMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Load logic to initialize the SFTP Polling Listener module.
 */
public class SftpPollingListenerModuleLoadListener implements SftpPollingListenerConstants {
    private static final Logger logger = Logger.getLogger(SftpPollingListenerModuleLoadListener.class.getName());

    // Manages all SFTP polling listener processes
    private static SftpPollingListenerModule sftpPollingListenerModule;
    private static DependencyProcessorRegistry<SsgActiveConnector> processorRegistry;

    @SuppressWarnings({"UnusedDeclaration"})
    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        if ( sftpPollingListenerModule != null) {
            logger.log(Level.WARNING, "SFTP polling listener module is already initialized");
        } else {
            // (1) Create (if does not exist) all context variables used by this module
            initializeModuleClusterProperties( context.getBean( "serverConfig", ServerConfig.class ) );

            // (2) Instantiate the SFTP polling boot process
            ThreadPoolBean pool = new ThreadPoolBean(
                    ServerConfig.getInstance(),
                    "SFTP polling Listener Pool",
                    LISTENER_THREAD_LIMIT_PROPERTY,
                    LISTENER_THREAD_LIMIT_UI_PROPERTY,
                    25);
            final Injector injector = context.getBean( "injector", Injector.class );
            sftpPollingListenerModule = new SftpPollingListenerModule(pool);
            injector.inject( sftpPollingListenerModule );
            sftpPollingListenerModule.setApplicationContext( context );

            // (3) Start the module
            try {
                // Start the boot process which will start the listeners
                logger.log(Level.INFO, "SftpPollingListenerModuleLoadListener - starting SftpPollingListenerModule...");
                sftpPollingListenerModule.start();
            } catch (LifecycleException e) {
                logger.log(Level.WARNING, "SFTP polling listener module threw exception during startup: " + ExceptionUtils.getMessage(e), e);
            }

            // Get the ssg connector dependency processor registry to add the mq connector dependency processor
            //noinspection unchecked
            processorRegistry = context.getBean( "ssgActiveConnectorDependencyProcessorRegistry", DependencyProcessorRegistry.class );
            processorRegistry.register(SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_SFTP, new DependencyProcessor<SsgActiveConnector>() {
                @Override
                @NotNull
                public List<Dependency> findDependencies(@NotNull SsgActiveConnector activeConnector, @NotNull DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
                    final List<DependencyFinder.FindResults> dependentEntities;
                    //add the password as a dependency
                    if (activeConnector.getProperty(SsgActiveConnector.PROPERTIES_KEY_SFTP_SECURE_PASSWORD_OID) != null) {
                        dependentEntities = finder.retrieveObjects(GoidUpgradeMapper.mapId(EntityType.SECURE_PASSWORD, activeConnector.getProperty(SsgActiveConnector.PROPERTIES_KEY_SFTP_SECURE_PASSWORD_OID)), com.l7tech.search.Dependency.DependencyType.SECURE_PASSWORD, com.l7tech.search.Dependency.MethodReturnType.GOID);
                    } else {
                        dependentEntities = finder.retrieveObjects(GoidUpgradeMapper.mapId(EntityType.SECURE_PASSWORD, activeConnector.getProperty(SsgActiveConnector.PROPERTIES_KEY_SFTP_SECURE_PASSWORD_KEY_OID)), com.l7tech.search.Dependency.DependencyType.SECURE_PASSWORD, com.l7tech.search.Dependency.MethodReturnType.GOID);
                    }
                    return finder.getDependenciesFromObjects(activeConnector, finder, dependentEntities);
                }

                @Override
                public void replaceDependencies(@NotNull final SsgActiveConnector activeConnector, @NotNull final Map<EntityHeader, EntityHeader> replacementMap, @NotNull final DependencyFinder finder, final boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException {
                    final SecurePasswordEntityHeader securePasswordEntityHeader;
                    if (activeConnector.getProperty(SsgActiveConnector.PROPERTIES_KEY_SFTP_SECURE_PASSWORD_OID) != null) {
                        securePasswordEntityHeader = new SecurePasswordEntityHeader(GoidUpgradeMapper.mapId(EntityType.SECURE_PASSWORD, activeConnector.getProperty(SsgActiveConnector.PROPERTIES_KEY_SFTP_SECURE_PASSWORD_OID)), null, null, SecurePassword.SecurePasswordType.PASSWORD.name());
                    } else {
                        securePasswordEntityHeader = new SecurePasswordEntityHeader(GoidUpgradeMapper.mapId(EntityType.SECURE_PASSWORD, activeConnector.getProperty(SsgActiveConnector.PROPERTIES_KEY_SFTP_SECURE_PASSWORD_KEY_OID)), null, null, SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY.name());
                    }
                    final EntityHeader newEntity = DependencyProcessorUtils.findMappedHeader(replacementMap, securePasswordEntityHeader);
                    if(newEntity!=null){
                        if (activeConnector.getProperty(SsgActiveConnector.PROPERTIES_KEY_SFTP_SECURE_PASSWORD_OID) != null) {
                            activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_SFTP_SECURE_PASSWORD_OID, newEntity.getStrId());
                        } else {
                            activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_SFTP_SECURE_PASSWORD_KEY_OID, newEntity.getStrId());
                        }
                    }
                }
            });
        }
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public static synchronized void onModuleUnloaded() {
        if ( sftpPollingListenerModule != null) {
            logger.log(Level.INFO, "Shutdown SFTP polling listener module");
            try {
                sftpPollingListenerModule.stop();
                sftpPollingListenerModule.destroy();
            } catch (Exception e) {
                logger.log(Level.WARNING, "SFTP polling listener module threw exception during shutdown: " + ExceptionUtils.getMessage(e), e);
            } finally {
                sftpPollingListenerModule = null;
            }
        }
        //remove the dependency processor
        if(processorRegistry != null) {
            processorRegistry.remove(SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_SFTP);
        }
    }

    /**
     * Checks for the existence of all cluster properties used by this module. If one does not already exist, then
     * it is created with the default value.
     *
     * @param config ServerConfig instance
     */
    private static void initializeModuleClusterProperties(final ServerConfig config) {

        Map<String, String> names = config.getClusterPropertyNames();

        List<String[]> toAdd = new ArrayList<String[]>();
        for (String[] tuple : MODULE_CLUSTER_PROPERTIES) {
            if (!names.containsKey(tuple[0])) {
                // create it
                toAdd.add(tuple);
            }
        }
        config.registerServerConfigProperties(toAdd.toArray(new String[toAdd.size()][]));
    }
}
