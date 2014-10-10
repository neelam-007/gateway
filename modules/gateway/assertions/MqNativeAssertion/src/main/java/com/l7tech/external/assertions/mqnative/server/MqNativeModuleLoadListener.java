package com.l7tech.external.assertions.mqnative.server;

import com.l7tech.external.assertions.mqnative.MqNativeExternalReferenceFactory;
import com.l7tech.external.assertions.mqnative.MqNativeRoutingAssertion;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntryId;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.message.HasHeaders;
import com.l7tech.message.HeadersKnob;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SsgKeyHeader;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfig.PropertyRegistrationInfo;
import com.l7tech.server.policy.export.PolicyExporterImporterManager;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.policy.variable.MessageSelector;
import com.l7tech.server.search.DependencyProcessorRegistry;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.processors.DependencyFinder;
import com.l7tech.server.search.processors.DependencyProcessor;
import com.l7tech.server.util.Injector;
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.GoidUpgradeMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.mqnative.MqNativeConstants.*;
import static com.l7tech.server.ServerConfig.PropertyRegistrationInfo.prInfo;
import static com.l7tech.util.CollectionUtils.list;

/**
 * Load logic that ensures the MQ Native transport module gets initialized.
 */
public class MqNativeModuleLoadListener {
    private static final Logger logger = Logger.getLogger(MqNativeModuleLoadListener.class.getName());

    // Manages all inbound MQ Native listener processes
    private static MqNativeModule mqNativeListenerModule;

    private static PolicyExporterImporterManager policyExporterImporterManager;
    private static MqNativeExternalReferenceFactory externalReferenceFactory;

    private static DependencyProcessorRegistry<SsgActiveConnector> processorRegistry;

    /**
     * This is a complete list of cluster-wide properties used by the MQNative module.
     */
    private static final List<PropertyRegistrationInfo> MODULE_CLUSTER_PROPERTIES = list(
            prInfo( MQ_MESSAGE_MAX_BYTES_PROPERTY, MQ_MESSAGE_MAX_BYTES_UI_PROPERTY, MQ_MESSAGE_MAX_BYTES_DESC, "2621440" ),
            prInfo( MQ_LISTENER_THREAD_LIMIT_PROPERTY, MQ_LISTENER_THREAD_LIMIT_UI_PROPERTY, MQ_LISTENER_THREAD_LIMIT_DESC, "25" ),
            prInfo( MQ_LISTENER_POLLING_INTERVAL_PROPERTY, MQ_LISTENER_POLLING_INTERVAL_UI_PROPERTY, MQ_LISTENER_POLLING_INTERVAL_DESC, "5s" ),
            prInfo( MQ_LISTENER_MAX_CONCURRENT_CONNECTIONS_PROPERTY, MQ_LISTENER_MAX_CONCURRENT_CONNECTIONS_UI_PROPERTY, MQ_LISTENER_MAX_CONCURRENT_CONNECTIONS_DESC, "1000" ),
            prInfo( MQ_RESPONSE_TIMEOUT_PROPERTY, MQ_RESPONSE_TIMEOUT_UI_PROPERTY, MQ_RESPONSE_TIMEOUT_DESC, "10000" ),
            prInfo( MQ_CONNECT_ERROR_SLEEP_PROPERTY, MQ_CONNECT_ERROR_SLEEP_UI_PROPERTY, MQ_CONNECT_ERROR_SLEEP_DESC, "10s" ),
            prInfo(MQ_PREVENT_AUDIT_FLOOD_PERIOD_PROPERTY, MQ_PREVENT_AUDIT_FLOOD_PERIOD_UI_PROPERTY, MQ_PREVENT_AUDIT_FLOOD_PERIOD_DESC, "0s" ),
            // connection cache properties
            prInfo( MQ_CONNECTION_CACHE_MAX_AGE_PROPERTY, MQ_CONNECTION_CACHE_MAX_AGE_UI_PROPERTY, MQ_CONNECTION_CACHE_MAX_AGE_DESC, "10m" ),
            prInfo( MQ_CONNECTION_CACHE_MAX_IDLE_PROPERTY, MQ_CONNECTION_CACHE_MAX_IDLE_UI_PROPERTY, MQ_CONNECTION_CACHE_MAX_IDLE_DESC, "5m" ),
            prInfo( MQ_CONNECTION_CACHE_MAX_SIZE_PROPERTY, MQ_CONNECTION_CACHE_MAX_SIZE_UI_PROPERTY, MQ_CONNECTION_CACHE_MAX_SIZE_DESC, "100" )
    );

    public static class MqNativeHeaderSelector implements MessageSelector.MessageAttributeSelector {
        final String prefix;
        final boolean multi;
        final List<Class<? extends HasHeaders>> supportedClasses;

        public MqNativeHeaderSelector(final String prefix, final boolean multi, final List<Class<? extends HasHeaders>> supportedClasses) {
            this.prefix = prefix;
            this.multi = multi;
            this.supportedClasses = supportedClasses;
        }

        @Override
        public ExpandVariables.Selector.Selection select(Message message, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
            boolean sawHeaderHaver = false;
            final String hname = name.substring(prefix.length());
            for (Class<? extends HasHeaders> headerKnob : supportedClasses) {
                HasHeaders hrk = message.getKnob(headerKnob);
                if (hrk != null) {
                    sawHeaderHaver = true;
                    String[] vals = hrk.getHeaderValues(hname);
                    if (vals != null && vals.length > 0) {
                        return new ExpandVariables.Selector.Selection(multi ? vals : vals[0]);
                    }
                }
            }

            if (sawHeaderHaver) {
                String msg = handler.handleBadVariable(hname + " header was empty");
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            } else {
                String msg = handler.handleBadVariable(name + " in " + message.getClass().getName());
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            }
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        if (mqNativeListenerModule != null) {
            logger.log(Level.WARNING, "MQ Native active connector module is already initialized");
        } else {
            final List<MessageSelector.MessageAttributeSelector> selectors = new ArrayList<>();

            selectors.add(new MessageSelector.HeadersKnobSelector(MqNativeRoutingAssertion.MQ + ".") {
                @Override
                protected ExpandVariables.Selector.Selection createSelection(String headerName, HeadersKnob headersKnob) {
                    String[] values = headersKnob.getHeaderValues(headerName, HeadersKnob.HEADER_TYPE_HTTP);

                    if (values != null && values.length > 0) {
                        return new ExpandVariables.Selector.Selection(values); // return all values
                    } else {
                        return null;
                    }
                }
            });

            selectors.add(new MqNativeHeaderSelector(MqNativeRoutingAssertion.MQ + ".", true,
                    Arrays.<Class<? extends HasHeaders>>asList(MqNativeKnob.class)));

            MessageSelector.registerSelector(MqNativeRoutingAssertion.MQ, new MessageSelector.ChainedSelector(selectors));
            // Create (if does not exist) all context variables used by this module
            initializeModuleClusterProperties(context.getBean("serverConfig", ServerConfig.class));

            // Register ExternalReferenceFactory
            registerExternalReferenceFactory(context);

            // Probe for MQ native class files - not installed by default on the Gateway
            try {
                Class.forName("com.ibm.mq.MQException", false, MqNativeModuleLoadListener.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                // Cannot proceed with initialization
                logger.fine("MQ Native Jars are not installed. Cannot load MQ Native Module.");
                return;
            }

            // Instantiate the MQ native boot process
            ThreadPoolBean pool = new ThreadPoolBean(
                    ServerConfig.getInstance(),
                    "MQ Native Listener Pool",
                    MQ_LISTENER_THREAD_LIMIT_PROPERTY,
                    MQ_LISTENER_THREAD_LIMIT_UI_PROPERTY,
                    25);
            final Injector injector = context.getBean( "injector", Injector.class );
            mqNativeListenerModule = new MqNativeModule(pool);
            injector.inject( mqNativeListenerModule );
            mqNativeListenerModule.setApplicationContext(context);

            // Start the module
            try {
                // Start the MqBootProcess which will start the listeners
                logger.log(Level.INFO, "MQNativeConnector MqNativeModuleLoadListener - starting MqNativeModuleLoadListener...");
                mqNativeListenerModule.start();
            } catch (LifecycleException e) {
                logger.log(Level.WARNING, "MQ Native active connector module threw exception during startup: " + ExceptionUtils.getMessage(e), e);
            }

            // Get the ssg connector dependency processor registry to add the mq connector dependency processor
            //noinspection unchecked
            processorRegistry = context.getBean( "ssgActiveConnectorDependencyProcessorRegistry", DependencyProcessorRegistry.class );
            processorRegistry.register(SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_MQ_NATIVE, new DependencyProcessor<SsgActiveConnector>() {
                @Override
                @NotNull
                public List<Dependency> findDependencies(@NotNull SsgActiveConnector activeConnector, @NotNull DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
                    List<DependencyFinder.FindResults> dependentEntities = new ArrayList<>();
                    //add the mq password as a dependency if it is set
                    if (activeConnector.getBooleanProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_IS_QUEUE_CREDENTIAL_REQUIRED)) {
                        dependentEntities.addAll(finder.retrieveObjects(GoidUpgradeMapper.mapId(EntityType.SECURE_PASSWORD, activeConnector.getProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_SECURE_PASSWORD_OID)), com.l7tech.search.Dependency.DependencyType.SECURE_PASSWORD, com.l7tech.search.Dependency.MethodReturnType.GOID));
                    }
                    //add the ssl key used if it is set
                    if (activeConnector.getBooleanProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_IS_SSL_ENABLED) && activeConnector.getBooleanProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_IS_SSL_KEYSTORE_USED)) {
                        String keyAlias = activeConnector.getProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_SSL_KEYSTORE_ALIAS);
                        String keyStoreId = activeConnector.getProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_SSL_KEYSTORE_ID);
                        dependentEntities.addAll(finder.retrieveObjects(new SsgKeyHeader(keyStoreId + ":" + keyAlias, GoidUpgradeMapper.mapId(EntityType.SSG_KEY_ENTRY, keyStoreId), keyAlias, keyAlias), com.l7tech.search.Dependency.DependencyType.SSG_PRIVATE_KEY, com.l7tech.search.Dependency.MethodReturnType.ENTITY_HEADER));
                    }
                    return finder.getDependenciesFromObjects(activeConnector, finder, dependentEntities);
                }

                @Override
                public void replaceDependencies(@NotNull SsgActiveConnector activeConnector, @NotNull Map<EntityHeader, EntityHeader> replacementMap, @NotNull DependencyFinder finder, final boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException {

                    if (activeConnector.getBooleanProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_IS_QUEUE_CREDENTIAL_REQUIRED)) {
                        // replace password dependency
                        activeConnector.setProperty(
                                SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_SECURE_PASSWORD_OID,
                                findReplacementPasswordId(GoidUpgradeMapper.mapId(EntityType.SECURE_PASSWORD, activeConnector.getProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_SECURE_PASSWORD_OID)).toString(),replacementMap));
                    }
                    //add the ssl key used if it is set
                    if (activeConnector.getBooleanProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_IS_SSL_ENABLED) && activeConnector.getBooleanProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_IS_SSL_KEYSTORE_USED)) {
                        String keyAlias = activeConnector.getProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_SSL_KEYSTORE_ALIAS);
                        String keyStoreId = activeConnector.getProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_SSL_KEYSTORE_ID);
                        SsgKeyEntryId replacementKey = findReplacementSslKey (keyStoreId, keyAlias, replacementMap);
                        if(replacementKey != null){
                            activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_SSL_KEYSTORE_ID, replacementKey.getKeystoreId().toString());
                            activeConnector.setProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_SSL_KEYSTORE_ALIAS, replacementKey.getAlias());
                        }
                    }
                }

                // returns keystore and alias
                private SsgKeyEntryId findReplacementSslKey(String keyStoreId, String keyAlias, @NotNull Map<EntityHeader, EntityHeader> replacementMap) {
                    final String keyId = keyStoreId + ":" + keyAlias;
                    for(EntityHeader srcHeader:  replacementMap.keySet()){
                        if(srcHeader.getType().equals(EntityType.SSG_KEY_ENTRY) && srcHeader.getStrId().equalsIgnoreCase(keyId)){
                            String targetID = replacementMap.get(srcHeader).getStrId();
                            return new SsgKeyEntryId(targetID);
                        }
                    }
                    // nothing to replace
                    return null;
                }

                private String findReplacementPasswordId(String srcId, @NotNull Map<EntityHeader, EntityHeader> replacementMap) {
                    for(EntityHeader srcHeader:  replacementMap.keySet()){
                        if(srcHeader.getType().equals(EntityType.SECURE_PASSWORD) && srcHeader.getStrId().equalsIgnoreCase(srcId)){
                            return replacementMap.get(srcHeader).getStrId();
                        }
                    }
                    // nothing to replace
                    return srcId;
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
        if (mqNativeListenerModule != null) {
            logger.log(Level.INFO, "Shutdown MQ Native active connector module");
            try {
                mqNativeListenerModule.doStop();
                mqNativeListenerModule.destroy();
            } catch (Exception e) {
                logger.log(Level.WARNING, "MQ Native active connector module threw exception during shutdown: " + ExceptionUtils.getMessage(e), e);
            } finally {
                mqNativeListenerModule = null;
            }
        }

        MessageSelector.unRegisterSelector(MqNativeRoutingAssertion.MQ);
        unregisterExternalReferenceFactory();

        //remove the dependency processor
        if (processorRegistry != null) {
            processorRegistry.remove(SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_MQ_NATIVE);
        }
    }

    /**
     * Checks for the existence of all cluster properties used by this module. If one does not already exist, then
     * it is created with the default value.
     *
     * @param config ServerConfig instance
     */
    private static void initializeModuleClusterProperties(final ServerConfig config) {
        final Map<String, String> names = config.getClusterPropertyNames();
        final List<PropertyRegistrationInfo> toAdd = new ArrayList<>();
        for ( final PropertyRegistrationInfo info : MODULE_CLUSTER_PROPERTIES) {
            if (!names.containsKey( info.getName() )) {
                // create it
                toAdd.add(info);
            }
        }
        config.registerServerConfigProperties( toAdd );
    }

    private static void registerExternalReferenceFactory(final ApplicationContext context) {
        unregisterExternalReferenceFactory(); // ensure not already registered

        if (policyExporterImporterManager == null) {
            policyExporterImporterManager = context.getBean("policyExporterImporterManager", PolicyExporterImporterManager.class);
        }

        externalReferenceFactory = new MqNativeExternalReferenceFactory();
        policyExporterImporterManager.register(externalReferenceFactory);
    }

    private static void unregisterExternalReferenceFactory() {
        if (policyExporterImporterManager != null && externalReferenceFactory!=null) {
            policyExporterImporterManager.unregister(externalReferenceFactory);
            externalReferenceFactory = null;
        }
    }
}