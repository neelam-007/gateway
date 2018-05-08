package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
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
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static com.l7tech.external.assertions.circuitbreaker.CircuitBreakerConstants.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

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
        forcedOpenCircuitProperty = new ClusterProperty(CB_FORCE_EVENT_TRACKER_LIST_CIRCUIT_OPEN_UI_PROPERTY, "a\b");
        forcedOpenCircuitProperty.setGoid(new Goid(2,2));
        clusterPropertyManager = Mockito.spy(new MockClusterPropertyManager(cleanupIntervalProperty, forcedOpenCircuitProperty));
        eventTrackerManager = Mockito.spy(new EventTrackerManager());
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
        assertEquals(newCleanupInterval, (long) method.invoke(eventTrackerManager, null));
    }

    @Test
    public void testRescheduleEventWhenCleanupIntervalUpdates() throws FindException, InvocationTargetException, IllegalAccessException {
        long newCleanupInterval = CLEANUP_INTERVAL + 1000L;

        eventTrackerManager.createEventTracker(POLICY_FAILURE_TRACKER_ID);
        cleanupIntervalProperty.setValue(Long.toString(newCleanupInterval));
        EntityInvalidationEvent event = new EntityInvalidationEvent(cleanupIntervalProperty, ClusterProperty.class, new Goid[]{new Goid(1,1)}, new char[]{EntityInvalidationEvent.DELETE});
        eventTrackerManager.getApplicationListener().onApplicationEvent(event);

        Mockito.verify(eventTrackerManager, Mockito.times(1)).rescheduleEventCleanupTask(newCleanupInterval);
    }

    //DE351400 - CWP deletion causes Circuit Breaker to fail with clusterproperty cast
    @Test
    public void testClusterPropertyDeletion() throws InvocationTargetException, IllegalAccessException, DeleteException, UpdateException, FindException {
        //Check the Circuit Breaker CWP are set and the Goids are the expected values
        eventTrackerManager.createEventTracker(POLICY_FAILURE_TRACKER_ID);

        Goid cleanupGoid = eventTrackerManager.getCleanupTask().getCleanupIntervalGoid();
        Goid eventTrackerGoid = eventTrackerManager.getCleanupTask().getEventTrackerListlGoid();
        assertEquals(cleanupGoid, cleanupIntervalProperty.getGoid());
        assertEquals(eventTrackerGoid, forcedOpenCircuitProperty.getGoid());

        //Remove the cleanup interval Circuit Breaker CWP
        clusterPropertyManager.delete(cleanupIntervalProperty);

        //Create EntityInvlidationEvents
        EntityInvalidationEvent event = new EntityInvalidationEvent(cleanupIntervalProperty, ClusterProperty.class, new Goid[]{new Goid(1,1)}, new char[]{EntityInvalidationEvent.DELETE});
        eventTrackerManager.getApplicationListener().onApplicationEvent(event);

        //Re-acquiring the tracking Goid
        cleanupGoid = eventTrackerManager.getCleanupTask().getCleanupIntervalGoid();
        assertEquals(cleanupGoid, Goid.DEFAULT_GOID);//back to Goid.Default when CWP removed
        Mockito.verify(eventTrackerManager, Mockito.atLeastOnce()).rescheduleEventCleanupTask(CB_EVENT_TRACKER_CLEANUP_INTERVAL_DEFAULT);

        //Remove the tracker list Circuit Breaker CWP
        clusterPropertyManager.delete(forcedOpenCircuitProperty);

        //Create EntityInvlidationEvents
        EntityInvalidationEvent event2 = new EntityInvalidationEvent(forcedOpenCircuitProperty, ClusterProperty.class, new Goid[]{new Goid(2,2)}, new char[]{EntityInvalidationEvent.DELETE});
        eventTrackerManager.getApplicationListener().onApplicationEvent(event2);

        //Re-acquiring the tracking Goid
        eventTrackerGoid = eventTrackerManager.getCleanupTask().getEventTrackerListlGoid();
        assertEquals(eventTrackerGoid, Goid.DEFAULT_GOID);
    }

    @Test
    public void testHandlingFindExceptionDuringStart() throws FindException {
        Mockito.when(clusterPropertyManager.getProperty(anyString())).thenThrow(new FindException());

        eventTrackerManager.shutdown();//need to restart to make the clusterPropertyManager.getProperty throw
        eventTrackerManager.start();

        Mockito.verify(eventTrackerManager, Mockito.times(2)).getEventCleanupInterval();//once at setup, 2nd time inside this test

        //When initialization failed, the cleanupInterval should be set to the default value
        assertEquals(eventTrackerManager.getCleanupTask().interval, CB_EVENT_TRACKER_CLEANUP_INTERVAL_DEFAULT);
    }

    @Test
    public void testHandleClusterPropertyChangeWhenFindByKeyThrows() throws FindException {
        Mockito.when(clusterPropertyManager.findByPrimaryKey(any(Goid.class))).thenThrow(new FindException());

        //update cleanup
        long newCleanupInterval = CLEANUP_INTERVAL + 1000L;
        eventTrackerManager.createEventTracker(POLICY_FAILURE_TRACKER_ID);
        cleanupIntervalProperty.setValue(Long.toString(newCleanupInterval));
        EntityInvalidationEvent event = new EntityInvalidationEvent(cleanupIntervalProperty, ClusterProperty.class, new Goid[]{new Goid(1,1)}, new char[]{EntityInvalidationEvent.DELETE});
        eventTrackerManager.getApplicationListener().onApplicationEvent(event);

        Mockito.verify(eventTrackerManager, Mockito.times(1)).handleClusterPropertyChange(event);//this method gets called
        Mockito.verify(eventTrackerManager, Mockito.times(0)).rescheduleEventCleanupTask(any(long.class));//clusterpropertyManager threw so never get to reschedule
        assertEquals(eventTrackerManager.getCleanupTask().interval, CLEANUP_INTERVAL);//since handleChange failed, interval should stay the same as default
    }

    @Test
    public void testLoadForcedOpenEventTrackerListWhenGetPropertyThrows() throws FindException {
        Mockito.when(clusterPropertyManager.getProperty(CB_FORCE_EVENT_TRACKER_LIST_CIRCUIT_OPEN_UI_PROPERTY)).thenThrow(new FindException());

        eventTrackerManager.createEventTracker(POLICY_FAILURE_TRACKER_ID);

        //update force list
        forcedOpenCircuitProperty.setValue(POLICY_FAILURE_TRACKER_ID);

        //Create EntityInvlidationEvents
        EntityInvalidationEvent event2 = new EntityInvalidationEvent(forcedOpenCircuitProperty, ClusterProperty.class, new Goid[]{new Goid(2,2)}, new char[]{EntityInvalidationEvent.DELETE});
        eventTrackerManager.getApplicationListener().onApplicationEvent(event2);

        Mockito.verify(eventTrackerManager, Mockito.times(2)).loadForcedOpenEventTrackerList();//once at setup(), 2nd time inside this test
        assertFalse(eventTrackerManager.isCircuitForcedOpen(POLICY_FAILURE_TRACKER_ID));//shows that updates to forcedOpenCircuitProperty failed
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
