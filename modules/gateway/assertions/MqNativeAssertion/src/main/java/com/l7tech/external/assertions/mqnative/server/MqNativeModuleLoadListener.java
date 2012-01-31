package com.l7tech.external.assertions.mqnative.server;

import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfig.PropertyRegistrationInfo;
import com.l7tech.server.util.Injector;
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
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

    /**
     * This is a complete list of cluster-wide properties used by the MQNative module.
     */
    private static final List<PropertyRegistrationInfo> MODULE_CLUSTER_PROPERTIES = list(
            prInfo( MQ_MESSAGE_MAX_BYTES_PROPERTY, MQ_MESSAGE_MAX_BYTES_UI_PROPERTY, MQ_MESSAGE_MAX_BYTES_DESC, "2621440" ),
            prInfo( LISTENER_THREAD_LIMIT_PROPERTY, LISTENER_THREAD_LIMIT_UI_PROPERTY, LISTENER_THREAD_LIMIT_DESC, "25" ),
            prInfo( MQ_RESPONSE_TIMEOUT_PROPERTY, MQ_RESPONSE_TIMEOUT_UI_PROPERTY, MQ_RESPONSE_TIMEOUT_DESC, "10000" ),
            prInfo( MQ_CONNECT_ERROR_SLEEP_PROPERTY, MQ_CONNECT_ERROR_SLEEP_UI_PROPERTY, MQ_CONNECT_ERROR_SLEEP_DESC, "10s" ),
            // connection cache properties
            prInfo( MQ_CONNECTION_CACHE_MAX_AGE_PROPERTY, MQ_CONNECTION_CACHE_MAX_AGE_UI_PROPERTY, MQ_CONNECTION_CACHE_MAX_AGE_DESC, "10m" ),
            prInfo( MQ_CONNECTION_CACHE_MAX_IDLE_PROPERTY, MQ_CONNECTION_CACHE_MAX_IDLE_UI_PROPERTY, MQ_CONNECTION_CACHE_MAX_IDLE_DESC, "5m" ),
            prInfo( MQ_CONNECTION_CACHE_MAX_SIZE_PROPERTY, MQ_CONNECTION_CACHE_MAX_SIZE_UI_PROPERTY, MQ_CONNECTION_CACHE_MAX_SIZE_DESC, "100" )
    );

    @SuppressWarnings({"UnusedDeclaration"})
    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        if (mqNativeListenerModule != null) {
            logger.log(Level.WARNING, "MQ Native active connector module is already initialized");
        } else {
            // (1) Create (if does not exist) all context variables used by this module
            initializeModuleClusterProperties( context.getBean( "serverConfig", ServerConfig.class ) );

            // (2) Instantiate the MQ native boot process
            ThreadPoolBean pool = new ThreadPoolBean(
                    ServerConfig.getInstance(),
                    "MQ Native Listener Pool",
                    LISTENER_THREAD_LIMIT_PROPERTY,
                    LISTENER_THREAD_LIMIT_UI_PROPERTY,
                    25);
            final Injector injector = context.getBean( "injector", Injector.class );
            mqNativeListenerModule = new MqNativeModule(pool);
            injector.inject( mqNativeListenerModule );
            mqNativeListenerModule.setApplicationContext(context);

            // (3) Start the module
            try {
                // Start the MqBootProcess which will start the listeners
                logger.log(Level.INFO, "MQNativeConnector MqNativeModuleLoadListener - starting MqNativeModuleLoadListener...");
                mqNativeListenerModule.start();
            } catch (LifecycleException e) {
                logger.log(Level.WARNING, "MQ Native active connector module threw exception during startup: " + ExceptionUtils.getMessage(e), e);
            }
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
    }

    /**
     * Checks for the existence of all cluster properties used by this module. If one does not already exist, then
     * it is created with the default value.
     *
     * @param config ServerConfig instance
     */
    private static void initializeModuleClusterProperties(final ServerConfig config) {
        final Map<String, String> names = config.getClusterPropertyNames();
        final List<PropertyRegistrationInfo> toAdd = new ArrayList<PropertyRegistrationInfo>();
        for ( final PropertyRegistrationInfo info : MODULE_CLUSTER_PROPERTIES) {
            if (!names.containsKey( info.getName() )) {
                // create it
                toAdd.add(info);
            }
        }
        config.registerServerConfigProperties( toAdd );
    }
}