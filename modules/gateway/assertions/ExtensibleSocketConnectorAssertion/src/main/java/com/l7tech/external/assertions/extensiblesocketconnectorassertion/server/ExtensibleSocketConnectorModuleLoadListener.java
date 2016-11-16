package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.CodecModule;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorEntity;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorReferenceFactory;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntity;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.GatewayState;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.policy.export.PolicyExporterImporterManager;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.service.FirewallRulesManager;
import com.l7tech.server.util.ApplicationEventProxy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.net.ssl.TrustManager;
import java.io.InputStream;
import java.io.StringReader;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ExtensibleSocketConnector's ModuleLoadListener
 */
public class ExtensibleSocketConnectorModuleLoadListener {
    private static final Logger LOGGER = Logger.getLogger(ExtensibleSocketConnectorModuleLoadListener.class.getName());

    private static final String DIRECT_BYTE_BUFFER_SYS_PROP = "mina.sslfilter.directbuffer";

    private static PolicyExporterImporterManager policyExporterImporterManager;
    private static ExtensibleSocketConnectorReferenceFactory externalReferenceFactory;
    private static ApplicationEventProxy eventProxy;

    private static SocketConnectorManager connectionManager;
    private static ApplicationListener applicationListener;
    private static GenericEntityManager gem;

    @SuppressWarnings({"UnusedDeclaration"})
    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        System.setProperty(DIRECT_BYTE_BUFFER_SYS_PROP, "false"); // RSA doesn't like direct ByteBuffers

        registerGenericEntities(context);

        registerExternalReferenceFactory(context);

        if (SocketConnectorManager.getInstance() == null) {
            initSocketConnectionManager(context);
        }

        /* Starts the connection manager if the Gateway is ReadyForMessages */
        GatewayState gatewayState = context.getBean("gatewayState", GatewayState.class);
        if (gatewayState.isReadyForMessages() && SocketConnectorManager.getInstance() != null) {
            // Only initialize all the ExtensibleSocketConnector inbound/outbound resource managers when the SSG is "ready for messages"
            SocketConnectorManager.getInstance().start();
        }

        eventProxy = context.getBean("applicationEventProxy", ApplicationEventProxy.class);
        applicationListener = new ApplicationListener() {
            @Override
            public void onApplicationEvent(ApplicationEvent applicationEvent) {

                // only do this when the SSG is ready for messages
                if (applicationEvent instanceof ReadyForMessages) {
                    // initialize the outbound MQ connection manager
                    connectionManager.start();

                }

                // startup the connection for the generic entity when one has been created
                // or updated
                if (applicationEvent instanceof EntityInvalidationEvent) {

                    EntityInvalidationEvent event = (EntityInvalidationEvent) applicationEvent;

                    if (GenericEntity.class.equals(event.getEntityClass())) {
                        Goid[] goids = event.getEntityIds();
                        char[] ops = event.getEntityOperations();
                        for (int i = 0; i < ops.length; i++) {
                            switch (ops[i]) {
                                case EntityInvalidationEvent.CREATE:
                                    try {
                                        ExtensibleSocketConnectorEntity entity = gem.getEntityManager(ExtensibleSocketConnectorEntity.class).findByPrimaryKey(goids[i]);
                                        if (entity != null) {
                                            connectionManager.connectionAdded(entity);
                                        }
                                    } catch (FindException e) {
                                        LOGGER.warning("Entity for goid, " + goids[i].toString() + ", not found when creating " +
                                                "ExtensibleSocketConnectorEntity");
                                    }
                                    break;

                                case EntityInvalidationEvent.UPDATE:
                                    try {
                                        ExtensibleSocketConnectorEntity entity = gem.getEntityManager(ExtensibleSocketConnectorEntity.class).findByPrimaryKey(goids[i]);
                                        if (entity != null) {
                                            connectionManager.connectionUpdated(entity);
                                        }
                                    } catch (FindException e) {
                                        LOGGER.warning("Entity for goid, " + goids[i].toString() + ", not found when updating " +
                                                "ExtensibleSocketConnectorEntity");
                                    }
                                    break;

                                case EntityInvalidationEvent.DELETE:
                                    connectionManager.connectionRemoved(goids[i]);
                                    break;

                                default:
                                    LOGGER.log(Level.WARNING, "Unexpected EntityInvalidationEvent Operation: " + ops[i]);
                                    break;
                            }
                        }
                    }
                }

            }
        };

        eventProxy.addApplicationListener(applicationListener);
    }

    /**
     * Register external reference factory
     * @param context the application context
     */
    private static void registerExternalReferenceFactory(final ApplicationContext context) {
        unregisterExternalReferenceFactory(); // ensure not already registered

        if (policyExporterImporterManager == null) {
            policyExporterImporterManager = context.getBean("policyExporterImporterManager", PolicyExporterImporterManager.class);
        }

        externalReferenceFactory = new ExtensibleSocketConnectorReferenceFactory();
        policyExporterImporterManager.register(externalReferenceFactory);
    }

    /**
     * Unregister external reference factory
     */
    private static void unregisterExternalReferenceFactory() {
        if (policyExporterImporterManager != null && externalReferenceFactory != null) {
            policyExporterImporterManager.unregister(externalReferenceFactory);

            externalReferenceFactory = null;
            policyExporterImporterManager = null;
        }
    }

    /**
     * Initializes the SocketConnectionManager
     * @param context the application context
     */
    private static void initSocketConnectionManager(ApplicationContext context) {
        ClusterPropertyManager clusterPropertyManager = context.getBean("clusterPropertyManager", ClusterPropertyManager.class);
        SsgKeyStoreManager keyStoreManager = context.getBean("ssgKeyStoreManager", SsgKeyStoreManager.class);
        TrustManager trustManager = context.getBean("routingTrustManager", TrustManager.class);
        SecureRandom secureRandom = context.getBean("secureRandom", SecureRandom.class);
        StashManagerFactory stashManagerFactory = context.getBean("stashManagerFactory", StashManagerFactory.class);
        MessageProcessor messageProcessor = context.getBean("messageProcessor", MessageProcessor.class);
        DefaultKey defaultKey = context.getBean("defaultKey", DefaultKey.class);
        FirewallRulesManager firewallRulesManager = context.getBean("ssgFirewallManager", FirewallRulesManager.class);

        loadCodecModules(clusterPropertyManager);
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");

        try {
            SocketConnectorManager.createConnectionManager(
                    gem.getEntityManager(ExtensibleSocketConnectorEntity.class),
                    clusterPropertyManager, keyStoreManager, trustManager, secureRandom, stashManagerFactory,
                    messageProcessor, defaultKey, firewallRulesManager);
            connectionManager = SocketConnectorManager.getInstance();
            connectionManager.initializeConnectorClasses();
            connectionManager.setDirectoryContext(new InitialDirContext(env));
        } catch (IllegalStateException | NamingException e) {
            LOGGER.log(Level.WARNING, "Error creating the ExtensibleSocket connection manager.", e);
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static synchronized void onModuleUnloaded() {
        removeApplicationListeners();

        if (SocketConnectorManager.getInstance() != null) {
            SocketConnectorManager.getInstance().stop();
        }

        unregisterGenericEntities();
        unregisterExternalReferenceFactory();

        System.setProperty(DIRECT_BYTE_BUFFER_SYS_PROP, "");
    }

    private static void loadCodecModules(ClusterPropertyManager clusterPropertyManager) {
        try {
            Properties properties = new Properties();
            InputStream stream = ExtensibleSocketConnectorModuleLoadListener.class.getClassLoader().getResourceAsStream(
                    "com/l7tech/external/assertions/extensiblesocketconnectorassertion/codecs.properties");
            properties.load(stream);
            stream.close();

            // Load the extra codecs defined in the cluster property
            String value = clusterPropertyManager.getProperty("extensibleSocketConnector.codecs");
            if (value != null) {
                properties.load(new StringReader(value));
            }

            Map<String, HashMap<String, String>> codecModulesData = new HashMap<>();
            for (String propertyName : properties.stringPropertyNames()) {
                String key = propertyName.substring(0, propertyName.lastIndexOf("."));

                HashMap<String, String> codecModuleData;
                if (codecModulesData.containsKey(key)) {
                    codecModuleData = codecModulesData.get(key);
                } else {
                    codecModuleData = new HashMap<>();
                    codecModulesData.put(key, codecModuleData);
                }

                String settingName = propertyName.substring(propertyName.lastIndexOf(".") + 1);
                codecModuleData.put(settingName, properties.getProperty(propertyName));
            }

            for (HashMap<String, String> data : codecModulesData.values()) {
                if (data.containsKey("dialog") && data.containsKey("configuration") && data.containsKey("codec") &&
                        data.containsKey("defaultContentType") && data.containsKey("displayName")) {
                    CodecModule codecModule = new CodecModule(
                            data.get("dialog"),
                            data.get("defaultContentType"),
                            data.get("configuration"),
                            data.get("displayName"),
                            data.get("codec")
                    );

                    MinaCodecFactory.addCodecModule(codecModule);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to load the codecs for the ExtensibleSocketConnector assertion.");
        }
    }

    /**
     * Remove application listeners added
     */
    private static void removeApplicationListeners() {
        if (eventProxy != null && applicationListener != null) {
            eventProxy.removeApplicationListener(applicationListener);
            eventProxy = null;
            applicationListener = null;
        }
    }

    /**
     * Register generic entities for this module
     * @param context
     */
    private static void registerGenericEntities(final ApplicationContext context) {
        if (gem == null) {
            gem = context.getBean("genericEntityManager", GenericEntityManager.class);
        }

        if (gem.isRegistered(ExtensibleSocketConnectorEntity.class.getName())) {
            gem.unRegisterClass(ExtensibleSocketConnectorEntity.class.getName());
        }

        gem.registerClass(ExtensibleSocketConnectorEntity.class);
    }

    /**
     * Unregister generic entities for this module
     */
    private static void unregisterGenericEntities() {
        if (gem != null) {
            gem.unRegisterClass(ExtensibleSocketConnectorEntity.class.getName());
            gem = null;
        }
    }
}
