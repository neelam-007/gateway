package com.l7tech.external.assertions.mqnative.server;

import static com.l7tech.external.assertions.mqnative.MqNativeConstants.*;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.util.Injector;
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Load logic that ensures the MQ Native transport module gets initialized.
 */
public class MqNativeModuleLoadListener {
    private static final Logger logger = Logger.getLogger(MqNativeModuleLoadListener.class.getName());

    // Manages all inbound native MQ listener processes
    private static MqNativeModule mqNativeListenerModule;

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