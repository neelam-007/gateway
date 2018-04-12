package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigStub;
import org.junit.After;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import static com.l7tech.external.assertions.circuitbreaker.CircuitBreakerConstants.CB_EVENT_TRACKER_CLEANUP_INTERVAL_DEFAULT;
import static com.l7tech.external.assertions.circuitbreaker.CircuitBreakerConstants.CB_EVENT_TRACKER_CLEANUP_INTERVAL_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by agram03 on 2017-08-10.
 *
 * Adding test for CircuitBreakerModuleLoadListener
 *
 */
public class CircuitBreakerModuleLoadListenerTest {

    @Test
    public void testOnModuleLoad() throws FindException {
        ApplicationContext context = ApplicationContexts.getTestApplicationContext();
        final ServerConfig config = context.getBean("serverConfig", ServerConfigStub.class);

        //Module not loaded yet
        assertEquals(0, config.getLongProperty(CB_EVENT_TRACKER_CLEANUP_INTERVAL_PROPERTY, 0));

        CircuitBreakerModuleLoadListener.onModuleLoaded(context);

        //Module is loaded cluster properties should be set
        assertEquals(CB_EVENT_TRACKER_CLEANUP_INTERVAL_DEFAULT, config.getLongProperty(CB_EVENT_TRACKER_CLEANUP_INTERVAL_PROPERTY, 0));
        assertNotNull(EventTrackerManagerHolder.getEventTrackerManager());
        assertNotNull(CircuitStateManagerHolder.getCircuitStateManager());
    }

    @After
    public void cleanUp() {
        CircuitBreakerModuleLoadListener.onModuleUnloaded();
    }
}
