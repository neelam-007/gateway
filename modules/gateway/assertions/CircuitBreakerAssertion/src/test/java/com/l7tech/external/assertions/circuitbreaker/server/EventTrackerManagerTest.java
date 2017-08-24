package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.MockClusterPropertyManager;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.TestTimeSource;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.l7tech.external.assertions.circuitbreaker.CircuitBreakerConstants.CB_EVENT_TRACKER_CLEANUP_INTERVAL_UI_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by agram03 on 2017-08-03.
 * Unit tests for EventTrackerManager.java
 *
 */
public class EventTrackerManagerTest {

    private TestTimeSource timeSource;
    private static final long CLEANUP_INTERVAL = 10L * 1000L;
    private EventTrackerManager eventTrackerManager;

    @Before
    public void setUp() throws Exception {
        long startTime = CLEANUP_INTERVAL;
        timeSource = new TestTimeSource(startTime, TimeUnit.MILLISECONDS.toNanos(startTime));
        MockClusterPropertyManager clusterPropertyManager = new MockClusterPropertyManager(
                new ClusterProperty(CB_EVENT_TRACKER_CLEANUP_INTERVAL_UI_PROPERTY, Long.toString(CLEANUP_INTERVAL))
        );
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
        eventTrackerManager.createEventTracker("POLICY_FAILURE_TRACKER_ID");
        EventTracker policyFailureEventTracker = eventTrackerManager.getEventTracker("POLICY_FAILURE_TRACKER_ID");

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
        eventTrackerManager.createEventTracker("POLICY_FAILURE_TRACKER_ID");
        eventTrackerManager.createEventTracker("LATENCY_FAILURE_TRACKER_ID");
        EventTracker policyFailureCircuitEventTracker = eventTrackerManager.getEventTracker("POLICY_FAILURE_TRACKER_ID");
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
        eventTrackerManager.createEventTracker("POLICY_FAILURE_TRACKER_ID");
        eventTrackerManager.createEventTracker("LATENCY_FAILURE_TRACKER_ID");
        eventTrackerManager.shutdown();
        EventTracker policyFailureCircuitEventTracker = eventTrackerManager.getEventTracker("POLICY_FAILURE_TRACKER_ID");
        EventTracker latencyCircuitEventTracker = eventTrackerManager.getEventTracker("LATENCY_FAILURE_TRACKER_ID");
        assertNull(policyFailureCircuitEventTracker);
        assertNull(latencyCircuitEventTracker);
        assertNull(eventTrackerManager.getCleanupTask());
    }
}
