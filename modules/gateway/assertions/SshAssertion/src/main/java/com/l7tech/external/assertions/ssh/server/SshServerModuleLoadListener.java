package com.l7tech.external.assertions.ssh.server;

import com.l7tech.external.assertions.ssh.SshCredentialAssertion;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SecurePasswordEntityHeader;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.search.DependencyProcessorRegistry;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.processors.DependencyFinder;
import com.l7tech.server.search.processors.DependencyProcessor;
import com.l7tech.server.search.processors.DependencyProcessorUtils;
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
 * Listener that ensures the SSH module gets initialized.
 */
public class SshServerModuleLoadListener {
    private static final Logger logger = Logger.getLogger(SshServerModuleLoadListener.class.getName());
    private static SshServerModule instance;
    private static DependencyProcessorRegistry<SsgConnector> processorRegistry;

    public static synchronized void onModuleLoaded(ApplicationContext context) {
        if (instance != null) {
            logger.log(Level.WARNING, "SSH module is already initialized");
        } else {
            instance = SshServerModule.createModule(context);
            try {
                instance.start();
            } catch (LifecycleException e) {
                logger.log(Level.WARNING, "SSH module threw exception on startup: " + ExceptionUtils.getMessage(e), e);
            }

            // Get the ssg connector dependency processor registry to add the ssh connector dependency processor
            //noinspection unchecked
            processorRegistry = context.getBean( "ssgConnectorDependencyProcessorRegistry", DependencyProcessorRegistry.class );
            processorRegistry.register(SshServerModule.SCHEME_SSH, new DependencyProcessor<SsgConnector>() {
                @Override
                @NotNull
                public List<Dependency> findDependencies(@NotNull SsgConnector connector, @NotNull DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
                    List<DependencyFinder.FindResults> dependentEntities = new ArrayList<>();
                    //adds the ssh password as a dependency if one is defined.
                    if (connector.getProperty(SshCredentialAssertion.LISTEN_PROP_HOST_PRIVATE_KEY) != null) {
                        dependentEntities.addAll(finder.retrieveObjects(GoidUpgradeMapper.mapId(EntityType.SECURE_PASSWORD, connector.getProperty(SshCredentialAssertion.LISTEN_PROP_HOST_PRIVATE_KEY)), com.l7tech.search.Dependency.DependencyType.SECURE_PASSWORD, com.l7tech.search.Dependency.MethodReturnType.GOID));
                    }
                    return finder.getDependenciesFromObjects(connector, finder, dependentEntities);
                }

                @Override
                public void replaceDependencies(@NotNull final SsgConnector connector, @NotNull final Map<EntityHeader, EntityHeader> replacementMap, @NotNull final DependencyFinder finder, final boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException {
                    if (connector.getProperty(SshCredentialAssertion.LISTEN_PROP_HOST_PRIVATE_KEY) != null) {
                        final SecurePasswordEntityHeader securePasswordEntityHeader = new SecurePasswordEntityHeader(GoidUpgradeMapper.mapId(EntityType.SECURE_PASSWORD, connector.getProperty(SshCredentialAssertion.LISTEN_PROP_HOST_PRIVATE_KEY)), null, null, SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY.name());
                        final EntityHeader newEntity = DependencyProcessorUtils.findMappedHeader(replacementMap, securePasswordEntityHeader);
                        if(newEntity!=null){
                            connector.putProperty(SshCredentialAssertion.LISTEN_PROP_HOST_PRIVATE_KEY, newEntity.getStrId());
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
    public static synchronized void onModuleUnloaded() {
        if (instance != null) {
            logger.log(Level.INFO, "SSH module is shutting down");
            try {
                instance.destroy();

            } catch (Exception e) {
                logger.log(Level.WARNING, "SSH module threw exception on shutdown: " + ExceptionUtils.getMessage(e), e);
            } finally {
                instance = null;
            }
            //remove the dependency processor from the list
            if(processorRegistry != null){
                processorRegistry.remove(SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_SFTP);
            }
        }
    }

}
