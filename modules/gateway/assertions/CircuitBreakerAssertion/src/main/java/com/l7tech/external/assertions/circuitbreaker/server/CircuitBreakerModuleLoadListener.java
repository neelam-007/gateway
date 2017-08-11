package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.server.util.Injector;
import org.springframework.context.ApplicationContext;

/**
 * Sets up EventTrackerManager for use by ServerCircuitBreakerAssertion.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
@SuppressWarnings({"UnusedDeclaration"})
public class CircuitBreakerModuleLoadListener {

    private static EventTrackerManager eventTrackerManager;

    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        eventTrackerManager = new EventTrackerManager();

        final Injector injector = context.getBean("injector", Injector.class);
        injector.inject(eventTrackerManager);

        eventTrackerManager.start();

        EventTrackerManagerHolder.setEventTrackerManager(eventTrackerManager);
        CircuitStateManagerHolder.setCircuitStateManager(new CircuitStateManager());
    }

    public static synchronized void onModuleUnloaded() {
        eventTrackerManager.shutdown();
    }

}
