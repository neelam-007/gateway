package com.l7tech.external.assertions.amqpassertion.server;

import com.l7tech.external.assertions.amqpassertion.AmqpExternalReferenceFactory;
import com.l7tech.server.GatewayState;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.policy.export.PolicyExporterImporterManager;
import com.l7tech.server.util.ApplicationEventProxy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.server.ServerConfig.PropertyRegistrationInfo.prInfo;
import static com.l7tech.util.CollectionUtils.list;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 22/02/12
 * Time: 10:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class ModuleLoadListener {
    private static final Logger logger = Logger.getLogger(ModuleLoadListener.class.getName());

    private static PolicyExporterImporterManager policyExporterImporterManager;
    private static AmqpExternalReferenceFactory externalReferenceFactory;

    // Context variables used
    public static final String AMQP_MESSAGE_MAX_BYTES_PROPERTY = "ioAMQPMessageMaxBytes";
    public static final String AMQP_MESSAGE_MAX_BYTES_UI_PROPERTY = "io.amqpMessageMaxBytes";
    public static final String AMQP_MESSAGE_MAX_BYTES_DESC = "Maximum number of bytes permitted for a AMQP message, or 0 for unlimited (Integer), default 0";
    public static final int DEFAULT_AMQP_MESSAGE_MAX_BYTES = 0;

    public static final String AMQP_RESPONSE_TIMEOUT_PROPERTY = "ioAMQPResponseTimeout";
    public static final String AMQP_RESPONSE_TIMEOUT_UI_PROPERTY = "io.amqpresponseTimeout";
    public static final String AMQP_RESPONSE_TIMEOUT_DESC = "Timeout for AMQP routing to wait on the replyTo queue in milliseconds, default 10 seconds.";
    public static final long DEFAULT_AMQP_RESPONSE_TIMEOUT_MS = 10000L;

    public static final String AMQP_CONNECT_ERROR_SLEEP_PROPERTY = "amqpConnectErrorSleep";
    public static final String AMQP_CONNECT_ERROR_SLEEP_UI_PROPERTY = "amqp.connectErrorSleep";
    public static final String AMQP_CONNECT_ERROR_SLEEP_DESC = "Time to sleep after a connection error for inbound/outbound AMQP queue in milliseconds, default 10 seconds";
    public static final long DEFAULT_AMQP_CONNECT_ERROR_SLEEP_MS = 10000L;

    public static final String AMQP_CONNECTION_CACHE_MAX_AGE_PROPERTY = "ioAMQPConnectionCacheMaxAge";
    public static final String AMQP_CONNECTION_CACHE_MAX_AGE_UI_PROPERTY = "io.amqpConnectionCacheMaxAge";
    public static final String AMQP_CONNECTION_CACHE_MAX_AGE_DESC = "Maximum age for cached AMQP connections or 0 for no limit (timeunit)";
    public static final String DEFAULT_AMQP_CONNECTION_CACHE_MAX_AGE_TIMEUNIT = "10m";

    public static final String AMQP_CONNECTION_CACHE_MAX_IDLE_PROPERTY = "ioAMQPConnectionCacheMaxIdle";
    public static final String AMQP_CONNECTION_CACHE_MAX_IDLE_UI_PROPERTY = "io.amqpConnectionCacheMaxIdle";
    public static final String AMQP_CONNECTION_CACHE_MAX_IDLE_DESC = "The maximum time an idle AMQP connection will be cached or 0 for no limit (timeunit)";
    public static final String DEFAULT_AMQP_CONNECTION_CACHE_MAX_IDLE_TIMEUNIT = "5m";
    /**
     * This is a complete list of cluster-wide properties used by the AMQP module.
     */
    public static final List<ServerConfig.PropertyRegistrationInfo> MODULE_CLUSTER_PROPERTIES = list(
            prInfo(AMQP_MESSAGE_MAX_BYTES_PROPERTY, AMQP_MESSAGE_MAX_BYTES_UI_PROPERTY, AMQP_MESSAGE_MAX_BYTES_DESC, String.valueOf(DEFAULT_AMQP_MESSAGE_MAX_BYTES)),
            prInfo(AMQP_RESPONSE_TIMEOUT_PROPERTY, AMQP_RESPONSE_TIMEOUT_UI_PROPERTY, AMQP_RESPONSE_TIMEOUT_DESC, String.valueOf(DEFAULT_AMQP_RESPONSE_TIMEOUT_MS)),
            prInfo(AMQP_CONNECT_ERROR_SLEEP_PROPERTY, AMQP_CONNECT_ERROR_SLEEP_UI_PROPERTY, AMQP_CONNECT_ERROR_SLEEP_DESC, String.valueOf(DEFAULT_AMQP_CONNECT_ERROR_SLEEP_MS)),
            prInfo(AMQP_CONNECTION_CACHE_MAX_AGE_PROPERTY, AMQP_CONNECTION_CACHE_MAX_AGE_UI_PROPERTY, AMQP_CONNECTION_CACHE_MAX_AGE_DESC, DEFAULT_AMQP_CONNECTION_CACHE_MAX_AGE_TIMEUNIT),
            prInfo(AMQP_CONNECTION_CACHE_MAX_IDLE_PROPERTY, AMQP_CONNECTION_CACHE_MAX_IDLE_UI_PROPERTY, AMQP_CONNECTION_CACHE_MAX_IDLE_DESC, DEFAULT_AMQP_CONNECTION_CACHE_MAX_IDLE_TIMEUNIT)
    );

    // SSG Application eveny proxy
    private static ApplicationEventProxy applicationEventProxy;

    // SSG cluster property manager
    private static ClusterPropertyManager cpManager;

    // AMQP Destinations manager
    private static ServerAMQPDestinationManager destinationManager;

    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        if (applicationEventProxy == null) {
            applicationEventProxy = context.getBean("applicationEventProxy", ApplicationEventProxy.class);
        }

        if (cpManager == null) {
            cpManager = context.getBean("clusterPropertyManager", ClusterPropertyManager.class);
        }

        // Register ExternalReferenceFactory
        registerExternalReferenceFactory(context);

        if (destinationManager != null) {
            logger.log(Level.WARNING, "AMQP connector module is already initialized");
        } else {
            // (1) Create if not exist all context variables used by this connector module
            initializeModuleClusterProperties(ServerConfig.getInstance());

            // Only initialize all the AMQP inbound/outbound resource managers when the SSG is "ready for messages" and AMQP manager has not been initialized
            GatewayState gatewayState = context.getBean("gatewayState", GatewayState.class);
            if (gatewayState.isReadyForMessages() && destinationManager == null) {
                initializeAmqpManager(context);
            } else {
                applicationEventProxy.addApplicationListener(new ApplicationListener() {
                    @Override
                    public void onApplicationEvent(ApplicationEvent event) {
                        if (destinationManager == null) {
                            if (event instanceof ReadyForMessages) {
                                initializeAmqpManager(context);
                            }
                        }
                    }
                });
            }
        }
    }

    private static void initializeAmqpManager(final ApplicationContext context) {
        destinationManager = ServerAMQPDestinationManager.getInstance(context);
        applicationEventProxy.addApplicationListener(destinationManager);
        destinationManager.start();
    }

    /*
    * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
    * that would otherwise keep our instances from getting collected.
    */
    public static synchronized void onModuleUnloaded() {
        destinationManager.shutdown();
        unregisterExternalReferenceFactory();
    }

    /**
     * Checks for the existence of all cluster properties used by this module. If one does not already exist, then
     * it is created with the default value.
     *
     * @param config ServerConfig instance
     */
    private static void initializeModuleClusterProperties(final ServerConfig config) {
        Map<String, String> names = config.getClusterPropertyNames();

        final List<ServerConfig.PropertyRegistrationInfo> toAdd = new ArrayList<>();
        for (final ServerConfig.PropertyRegistrationInfo info : MODULE_CLUSTER_PROPERTIES) {
            if (!names.containsKey(info.getName())) {
                toAdd.add(info);
            }
        }
        config.registerServerConfigProperties(toAdd);
    }

    private static void registerExternalReferenceFactory(final ApplicationContext context) {
        unregisterExternalReferenceFactory(); // ensure not already registered

        if (policyExporterImporterManager == null) {
            policyExporterImporterManager = context.getBean("policyExporterImporterManager", PolicyExporterImporterManager.class);
        }

        externalReferenceFactory = new AmqpExternalReferenceFactory();
        policyExporterImporterManager.register(externalReferenceFactory);
    }

    private static void unregisterExternalReferenceFactory() {
        if (policyExporterImporterManager != null && externalReferenceFactory != null) {
            policyExporterImporterManager.unregister(externalReferenceFactory);
            externalReferenceFactory = null;
        }
    }
}
