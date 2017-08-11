package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.server.ApplicationContexts;
import com.l7tech.util.TestTimeSource;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by agram03 on 2017-08-03.
 * Unit tests for EventTrackerManager.java
 *
 */
public class EventTrackerManagerTest {

    private TestTimeSource timeSource;
    private static final long INTERVAL = 10L * 1000L;
    private EventTrackerManager eventTrackerManager;

    @Before
    public void setUp() throws Exception {
        long startTime = INTERVAL;
        timeSource = new TestTimeSource(startTime, TimeUnit.MILLISECONDS.toNanos(startTime));
        eventTrackerManager = new EventTrackerManager();
        EventTrackerManager.setCounterCleanupInterval(INTERVAL);
        ApplicationContexts.inject(eventTrackerManager, Collections.singletonMap("timeSource", timeSource));
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

        timeSource.advanceByMillis(INTERVAL + 10L);
        eventTrackerManager.getTIMER_TASK().run();
        assertEquals(0, policyFailureEventTracker.getCountSinceTimestamp(0));
    }

    @Test
    public void testEventsCleanupTask_OnTwoDIfferentTrackers_Success() throws Exception {
        eventTrackerManager.createEventTracker("POLICY_FAILURE_TRACKER_ID");
        eventTrackerManager.createEventTracker("LATENCY_FAILURE_TRACKER_ID");
        EventTracker policyFailureEventTracker = eventTrackerManager.getEventTracker("POLICY_FAILURE_TRACKER_ID");
        EventTracker latencyFailureEventTracker = eventTrackerManager.getEventTracker("LATENCY_FAILURE_TRACKER_ID");

        policyFailureEventTracker.recordEvent(timeSource.getNanoTime());
        timeSource.advanceByNanos(10);
        latencyFailureEventTracker.recordEvent(timeSource.getNanoTime());
        timeSource.advanceByNanos(5);
        policyFailureEventTracker.recordEvent(timeSource.getNanoTime());
        timeSource.advanceByNanos(10);
        latencyFailureEventTracker.recordEvent(timeSource.getNanoTime());
        timeSource.advanceByNanos(5);
        policyFailureEventTracker.recordEvent(timeSource.getNanoTime());
        timeSource.advanceByNanos(10);
        latencyFailureEventTracker.recordEvent(timeSource.getNanoTime());
        timeSource.advanceByNanos(10);
        latencyFailureEventTracker.recordEvent(timeSource.getNanoTime());
        timeSource.advanceByNanos(50);
        policyFailureEventTracker.recordEvent(timeSource.getNanoTime());
        timeSource.advanceByNanos(150);
        latencyFailureEventTracker.recordEvent(timeSource.getNanoTime());

        assertEquals(4, policyFailureEventTracker.getCountSinceTimestamp(0L));
        assertEquals(5, latencyFailureEventTracker.getCountSinceTimestamp(0L));

        timeSource.advanceByMillis(INTERVAL - 1L);
        eventTrackerManager.getTIMER_TASK().run();
        assertEquals(4, policyFailureEventTracker.getCountSinceTimestamp(0L));
        assertEquals(5, latencyFailureEventTracker.getCountSinceTimestamp(0L));

        timeSource.advanceByNanos(999792L);
        eventTrackerManager.getTIMER_TASK().run();
        assertEquals(1, policyFailureEventTracker.getCountSinceTimestamp(0L));
        assertEquals(2, latencyFailureEventTracker.getCountSinceTimestamp(0L));

        timeSource.advanceByMillis(1000L);
        eventTrackerManager.getTIMER_TASK().run();
        assertEquals(0, policyFailureEventTracker.getCountSinceTimestamp(0L));
        assertEquals(0, latencyFailureEventTracker.getCountSinceTimestamp(0L));

    }

    @Test
    public void testCleanupTaskShutdown() {
        eventTrackerManager.createEventTracker("POLICY_FAILURE_TRACKER_ID");
        eventTrackerManager.createEventTracker("LATENCY_FAILURE_TRACKER_ID");
        eventTrackerManager.shutdown();
        EventTracker policyFailureEventTracker = eventTrackerManager.getEventTracker("POLICY_FAILURE_TRACKER_ID");
        EventTracker latencyFailureEventTracker = eventTrackerManager.getEventTracker("LATENCY_FAILURE_TRACKER_ID");
        assertNull(policyFailureEventTracker);
        assertNull(latencyFailureEventTracker);

        try {
            Field field = TimerTask.class.getDeclaredField("state");
            assertEquals(3, field.get(eventTrackerManager.getTIMER_TASK()));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    // TODO: add test for shutdown method (add trackers, shutdown, assert expected task state and tracker map cleared)
}
