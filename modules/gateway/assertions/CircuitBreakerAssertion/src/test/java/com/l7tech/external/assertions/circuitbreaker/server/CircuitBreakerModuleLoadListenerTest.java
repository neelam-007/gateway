package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.server.ApplicationContexts;
import org.junit.After;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import static org.junit.Assert.assertNotNull;

/**
 * Created by agram03 on 2017-08-10.
 *
 * Adding test for CircuitBreakerModuleLoadListener
 *
 */
public class CircuitBreakerModuleLoadListenerTest {

    @Test
    public void testOnModuleLoad() {
        ApplicationContext context = ApplicationContexts.getTestApplicationContext();
        CircuitBreakerModuleLoadListener.onModuleLoaded(context);
        assertNotNull(EventTrackerManagerHolder.getEventTrackerManager());
        assertNotNull(CircuitStateManagerHolder.getCircuitStateManager());
    }

    @After
    public void cleanUp() {
        CircuitBreakerModuleLoadListener.onModuleUnloaded();
    }
}
