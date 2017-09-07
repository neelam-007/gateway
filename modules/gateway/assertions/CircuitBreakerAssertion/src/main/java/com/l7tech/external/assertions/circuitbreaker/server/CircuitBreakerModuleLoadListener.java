package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.server.ServerConfig;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.util.Injector;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.l7tech.external.assertions.circuitbreaker.CircuitBreakerConstants.*;
import static com.l7tech.server.ServerConfig.PropertyRegistrationInfo.prInfo;
import static com.l7tech.util.CollectionUtils.list;

/**
 * Sets up EventTrackerManager for use by ServerCircuitBreakerAssertion.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
@SuppressWarnings({"UnusedDeclaration", "WeakerAccess"})
public class CircuitBreakerModuleLoadListener {

    private static EventTrackerManager eventTrackerManager;
    private static ApplicationEventProxy eventProxy;
    private static ApplicationListener eventTrackerManagerListener;

    /**
     * This is a complete list of cluster-wide properties used by the Circuit Breaker Module.
     */
    private static final List<ServerConfig.PropertyRegistrationInfo> MODULE_CLUSTER_PROPERTIES = list(
        prInfo(CB_EVENT_TRACKER_CLEANUP_INTERVAL_PROPERTY, CB_EVENT_TRACKER_CLEANUP_INTERVAL_UI_PROPERTY,
                CB_EVENT_TRACKER_CLEANUP_INTERVAL_DESC, String.valueOf(CB_EVENT_TRACKER_CLEANUP_INTERVAL_DEFAULT),
                "greaterThanZeroLong"),
        prInfo(CB_FORCE_EVENT_TRACKER_LIST_CIRCUIT_OPEN, CB_FORCE_EVENT_TRACKER_LIST_CIRCUIT_OPEN_UI_PROPERTY, CB_FORCE_EVENT_TRACKER_LIST_CIRCUIT_OPEN_DESC, "")
    );

    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        initializeModuleClusterProperties(context.getBean("serverConfig", ServerConfig.class));

        eventTrackerManager = new EventTrackerManager();
        eventTrackerManagerListener = eventTrackerManager.getApplicationListener();

        eventProxy = context.getBean("applicationEventProxy", ApplicationEventProxy.class);
        eventProxy.addApplicationListener(eventTrackerManagerListener);

        final Injector injector = context.getBean("injector", Injector.class);
        injector.inject(eventTrackerManager);

        eventTrackerManager.start();

        EventTrackerManagerHolder.setEventTrackerManager(eventTrackerManager);
        CircuitStateManagerHolder.setCircuitStateManager(new CircuitStateManager());
    }

    /**
     * Checks for the existence of all cluster properties used by this module. If one does not already exist, then
     * it is created with the default value.
     *
     * @param config ServerConfig instance
     */
    private static void initializeModuleClusterProperties(final ServerConfig config) {
        final Map<String, String> names = config.getClusterPropertyNames();
        final List<ServerConfig.PropertyRegistrationInfo> toAdd = new ArrayList<>();
        for (final ServerConfig.PropertyRegistrationInfo info : MODULE_CLUSTER_PROPERTIES) {
            if (!names.containsKey(info.getName())) {
                // create it
                toAdd.add(info);
            }
        }
        config.registerServerConfigProperties(toAdd);
    }

    public static synchronized void onModuleUnloaded() {
        if (eventProxy != null && eventTrackerManagerListener != null) {
            eventProxy.removeApplicationListener(eventTrackerManagerListener);
            eventProxy = null;
            eventTrackerManagerListener = null;
        }
        eventTrackerManager.shutdown();
    }

}
