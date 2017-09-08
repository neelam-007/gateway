package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.MockClusterPropertyManager;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.TestTimeSource;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static com.l7tech.external.assertions.circuitbreaker.CircuitBreakerConstants.CB_EVENT_TRACKER_CLEANUP_INTERVAL_UI_PROPERTY;
import static com.l7tech.external.assertions.circuitbreaker.CircuitBreakerConstants.CB_FORCE_EVENT_TRACKER_LIST_CIRCUIT_OPEN_UI_PROPERTY;
import static org.junit.Assert.*;

/**
 * Created by agram03 on 2017-08-03.
 * Unit tests for EventTrackerManager.java
 *
 */
public class EventTrackerManagerTest {

    private static final String POLICY_FAILURE_TRACKER_ID = "POLICY_FAILURE_TRACKER_ID";
    private TestTimeSource timeSource;
    private static final long CLEANUP_INTERVAL = 10L * 1000L;
    private EventTrackerManager eventTrackerManager;
    private ClusterPropertyManager clusterPropertyManager;
    private ClusterProperty cleanupIntervalProperty;
    private ClusterProperty forcedOpenCircuitProperty;

    @Before
    public void setUp() throws Exception {
        long startTime = CLEANUP_INTERVAL;
        timeSource = new TestTimeSource(startTime, TimeUnit.MILLISECONDS.toNanos(startTime));
        cleanupIntervalProperty = new ClusterProperty(CB_EVENT_TRACKER_CLEANUP_INTERVAL_UI_PROPERTY, Long.toString(CLEANUP_INTERVAL));
        cleanupIntervalProperty.setGoid(new Goid(1,1));
        forcedOpenCircuitProperty = new ClusterProperty(CB_FORCE_EVENT_TRACKER_LIST_CIRCUIT_OPEN_UI_PROPERTY, "");
        forcedOpenCircuitProperty.setGoid(new Goid(2,2));
        clusterPropertyManager = new MockClusterPropertyManager(cleanupIntervalProperty, forcedOpenCircuitProperty);
        eventTrackerManager = new EventTrackerManager();
        ApplicationContexts.inject(eventTrackerManager, CollectionUtils.<String, Object>mapBuilder()
                .put("timeSource", timeSource)
                .put("clusterPropertyManager", clusterPropertyManager)
                .unmodifiableMap()
        );
        eventTrackerManager.start();
    }

    @Test
    public void testEventsCleanupTaskSuccess() throws Exception {
        eventTrackerManager.createEventTracker(POLICY_FAILURE_TRACKER_ID);
        EventTracker policyFailureEventTracker = eventTrackerManager.getEventTracker(POLICY_FAILURE_TRACKER_ID);

        policyFailureEventTracker.recordEvent(timeSource.getNanoTime());
        timeSource.advanceByNanos(10);
        policyFailureEventTracker.recordEvent(timeSource.getNanoTime());
        timeSource.advanceByNanos(10);
        policyFailureEventTracker.recordEvent(timeSource.getNanoTime());
        timeSource.advanceByNanos(10);

        assertEquals(3, policyFailureEventTracker.getCountSinceTimestamp(timeSource.getNanoTime() - 40));

        timeSource.advanceByMillis(CLEANUP_INTERVAL + 10L);
        eventTrackerManager.getCleanupTask().run();
        assertEquals(0, policyFailureEventTracker.getCountSinceTimestamp(0));
    }

    @Test
    public void testEventsCleanupTask_OnTwoDIfferentTrackers_Success() throws Exception {
        eventTrackerManager.createEventTracker(POLICY_FAILURE_TRACKER_ID);
        eventTrackerManager.createEventTracker("LATENCY_FAILURE_TRACKER_ID");
        EventTracker policyFailureCircuitEventTracker = eventTrackerManager.getEventTracker(POLICY_FAILURE_TRACKER_ID);
        EventTracker latencyCircuitEventTracker = eventTrackerManager.getEventTracker("LATENCY_FAILURE_TRACKER_ID");

        policyFailureCircuitEventTracker.recordEvent(timeSource.getNanoTime());
        timeSource.advanceByNanos(10);
        latencyCircuitEventTracker.recordEvent(timeSource.getNanoTime());
        timeSource.advanceByNanos(5);
        policyFailureCircuitEventTracker.recordEvent(timeSource.getNanoTime());
        timeSource.advanceByNanos(10);
        latencyCircuitEventTracker.recordEvent(timeSource.getNanoTime());
        timeSource.advanceByNanos(5);
        policyFailureCircuitEventTracker.recordEvent(timeSource.getNanoTime());
        timeSource.advanceByNanos(10);
        latencyCircuitEventTracker.recordEvent(timeSource.getNanoTime());
        timeSource.advanceByNanos(10);
        latencyCircuitEventTracker.recordEvent(timeSource.getNanoTime());
        timeSource.advanceByNanos(50);
        policyFailureCircuitEventTracker.recordEvent(timeSource.getNanoTime());
        timeSource.advanceByNanos(150);
        latencyCircuitEventTracker.recordEvent(timeSource.getNanoTime());

        assertEquals(4, policyFailureCircuitEventTracker.getCountSinceTimestamp(0L));
        assertEquals(5, latencyCircuitEventTracker.getCountSinceTimestamp(0L));

        timeSource.advanceByMillis(CLEANUP_INTERVAL - 1L);
        eventTrackerManager.getCleanupTask().run();
        assertEquals(4, policyFailureCircuitEventTracker.getCountSinceTimestamp(0L));
        assertEquals(5, latencyCircuitEventTracker.getCountSinceTimestamp(0L));

        timeSource.advanceByNanos(999792L);
        eventTrackerManager.getCleanupTask().run();
        assertEquals(1, policyFailureCircuitEventTracker.getCountSinceTimestamp(0L));
        assertEquals(2, latencyCircuitEventTracker.getCountSinceTimestamp(0L));

        timeSource.advanceByMillis(1000L);
        eventTrackerManager.getCleanupTask().run();
        assertEquals(0, policyFailureCircuitEventTracker.getCountSinceTimestamp(0L));
        assertEquals(0, latencyCircuitEventTracker.getCountSinceTimestamp(0L));
    }

    @Test
    public void testCleanupTaskShutdown() throws NoSuchFieldException, IllegalAccessException {
        eventTrackerManager.createEventTracker(POLICY_FAILURE_TRACKER_ID);
        eventTrackerManager.createEventTracker("LATENCY_FAILURE_TRACKER_ID");
        eventTrackerManager.shutdown();
        EventTracker policyFailureCircuitEventTracker = eventTrackerManager.getEventTracker(POLICY_FAILURE_TRACKER_ID);
        EventTracker latencyCircuitEventTracker = eventTrackerManager.getEventTracker("LATENCY_FAILURE_TRACKER_ID");
        assertNull(policyFailureCircuitEventTracker);
        assertNull(latencyCircuitEventTracker);
        assertNull(eventTrackerManager.getCleanupTask());
    }

    @Test
    public void testForcedOpenCircuit() throws UpdateException {
        eventTrackerManager.createEventTracker(POLICY_FAILURE_TRACKER_ID);
        assertFalse(eventTrackerManager.isCircuitForcedOpen(POLICY_FAILURE_TRACKER_ID));
        forcedOpenCircuitProperty.setValue(POLICY_FAILURE_TRACKER_ID);
        EntityInvalidationEvent event = new EntityInvalidationEvent(forcedOpenCircuitProperty, ClusterProperty.class, new Goid[]{new Goid(2,2)}, new char[]{EntityInvalidationEvent.UPDATE});
        eventTrackerManager.getApplicationListener().onApplicationEvent(event);
        assertTrue(eventTrackerManager.isCircuitForcedOpen(POLICY_FAILURE_TRACKER_ID));
    }

    @Test
    public void testCleanupIntervalUpdate() throws InvocationTargetException, IllegalAccessException, UpdateException {
        long newCleanupInterval = CLEANUP_INTERVAL + 1000L;
        eventTrackerManager.createEventTracker(POLICY_FAILURE_TRACKER_ID);
        Method method = getPrivateMethodInAccessibleMode(EventTrackerManager.class, "getEventCleanupInterval");
        long cleanupInterval = (long)method.invoke(eventTrackerManager);
        assertEquals(CLEANUP_INTERVAL, cleanupInterval);
        cleanupIntervalProperty.setValue(Long.toString(newCleanupInterval));
        clusterPropertyManager.update(cleanupIntervalProperty);
        assertEquals(newCleanupInterval, (long)method.invoke(eventTrackerManager, null));
    }

    private Method getPrivateMethodInAccessibleMode(final Class<?> aClass, final String methodName) {
        Method privateMethod = null;
        try {
            privateMethod = aClass.getDeclaredMethod(methodName, null);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        privateMethod.setAccessible(true);
        return privateMethod;
    }
}
