package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TimeSource;
import org.springframework.context.ApplicationListener;

import javax.inject.Inject;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.circuitbreaker.CircuitBreakerConstants.CB_EVENT_TRACKER_CLEANUP_INTERVAL_DEFAULT;
import static com.l7tech.external.assertions.circuitbreaker.CircuitBreakerConstants.CB_EVENT_TRACKER_CLEANUP_INTERVAL_UI_PROPERTY;

/**
 * Manages Event Tracker.
 *
 * @author Ekta Khandelwal - khaek01@ca.com
 *
 */
class EventTrackerManager {

    private static final Logger logger = Logger.getLogger(EventTrackerManager.class.getName());
    private final ConcurrentHashMap<String, EventTracker> eventTrackerMap = new ConcurrentHashMap<>();
    private final Object lock = new Object();
    private final AtomicBoolean started = new AtomicBoolean(false);

    private final AtomicReference<TimerTask> cleanupTask = new AtomicReference<>(null);

    @Inject
    private TimeSource timeSource;

    @Inject
    private ClusterPropertyManager clusterPropertyManager;

    EventTracker getEventTracker(final String trackerId) {
        return eventTrackerMap.get(trackerId);
    }

    EventTracker createEventTracker(final String trackerId) {
        EventTracker eventTracker = eventTrackerMap.get(trackerId);
        if (null == eventTracker) {
            synchronized (lock) {
                eventTracker = eventTrackerMap.get(trackerId);
                if (null == eventTracker) {
                    eventTracker = new EventTracker();
                    //Add EventTracker if not present in eventTrackerMap
                    eventTrackerMap.put(trackerId, eventTracker);
                }
            }
        }
        return eventTracker;
    }

    synchronized void start() {
        if (!started.get()) {
            scheduleEventCleanupTask(getEventCleanupInterval());
            started.set(true);
        }
    }

    synchronized void shutdown() {
        if (started.get()) {
            stopEventCleanupTask();
            started.set(false);
            eventTrackerMap.clear();
        }
    }

    private synchronized void rescheduleEventCleanupTask(final long eventCleanupInterval) {
        if (started.get()) {
            logger.log(Level.FINE, "Rescheduling Event Tracker clean up task");
            stopEventCleanupTask();
            scheduleEventCleanupTask(eventCleanupInterval);
        }
    }

    private void scheduleEventCleanupTask(final long eventCleanupInterval) {
        if (cleanupTask.get() == null) {
            logger.log(Level.FINE, "Scheduling Event Tracker clean up task");
            cleanupTask.set(new TimerTask() {
                @Override
                public void run() {
                    cleanupEvents(eventCleanupInterval);
                }
            });

            Background.scheduleRepeated(cleanupTask.get(), eventCleanupInterval, eventCleanupInterval);
        } else {
            logger.log(Level.FINE, "Failed to schedule Event Tracker clean up task; a task is already scheduled");
        }
    }

    private synchronized void stopEventCleanupTask() {
        if (cleanupTask.get() != null) {
            Background.cancel(cleanupTask.get());
            cleanupTask.set(null);
            logger.log(Level.FINE, "Stopping Event Tracker clean up task");
        }
    }

    private long getEventCleanupInterval() {
        try {
            return Long.valueOf(clusterPropertyManager.getProperty(CB_EVENT_TRACKER_CLEANUP_INTERVAL_UI_PROPERTY));
        } catch (NumberFormatException | FindException e) {
            return CB_EVENT_TRACKER_CLEANUP_INTERVAL_DEFAULT;
        }
    }

    private synchronized void cleanupEvents(long eventCleanupInterval) {
        long interval = TimeUnit.MILLISECONDS.toNanos(eventCleanupInterval);
        long cleanBeforeTimestamp = timeSource.nanoTime() - interval;
        logger.log(Level.FINE, "Cleaning events older than " + eventCleanupInterval + " milliseconds");
        for (Map.Entry<String, EventTracker> entry : eventTrackerMap.entrySet()) {
            entry.getValue().clearTimestampBefore(cleanBeforeTimestamp);
        }
    }

    ApplicationListener getApplicationListener() {
        return applicationEvent -> {
            if (applicationEvent instanceof EntityInvalidationEvent) {
                EntityInvalidationEvent event = (EntityInvalidationEvent) applicationEvent;
                if (ClusterProperty.class.equals(event.getEntityClass())) {
                    handleClusterPropertyChange(event);
                }
            }
        };
    }

    private void handleClusterPropertyChange(EntityInvalidationEvent entityInvalidationEvent) {
        for (Goid oid : entityInvalidationEvent.getEntityIds()) {
            try {
                ClusterProperty clusterProperty = clusterPropertyManager.findByPrimaryKey(oid);
                if (clusterProperty != null && CB_EVENT_TRACKER_CLEANUP_INTERVAL_UI_PROPERTY.equals(clusterProperty.getName())) {
                    rescheduleEventCleanupTask(Long.valueOf(clusterProperty.getValue()));
                } else if (clusterProperty == null && CB_EVENT_TRACKER_CLEANUP_INTERVAL_UI_PROPERTY.equals(((ClusterProperty) entityInvalidationEvent.getSource()).getName())) {// when cluster property is deleted
                    rescheduleEventCleanupTask(CB_EVENT_TRACKER_CLEANUP_INTERVAL_DEFAULT);
                }
            } catch (FindException e) {
                logger.log(Level.FINE, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }
    }

    /**
    *
    * These methods (getCleanupTask) are used for getting / setting values / objects for unit tests only.
    *
    * */
    TimerTask getCleanupTask() {
        return cleanupTask.get();
    }
}
