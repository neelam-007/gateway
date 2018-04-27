package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TimeSource;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationListener;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.circuitbreaker.CircuitBreakerConstants.*;

/**
 * Manages Event Tracker.
 *
 * @author Ekta Khandelwal - khaek01@ca.com
 *
 */
class EventTrackerManager {

    private static final Logger LOGGER = Logger.getLogger(EventTrackerManager.class.getName());
    private final ConcurrentHashMap<String, EventTracker> eventTrackerMap = new ConcurrentHashMap<>();
    private final Object lock = new Object();
    private final AtomicBoolean started = new AtomicBoolean(false);

    private final AtomicReference<TimerTaskWithGoid> cleanupTask = new AtomicReference<>(null);
    private final List<String> forcedOpenEventTrackerList = new ArrayList<>();

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
                // double null check required - Map.computeIfAbsent not suitable
                if (null == eventTracker) {
                    eventTracker = new EventTracker();
                    //Add EventTracker if not present in eventTrackerMap
                    eventTrackerMap.put(trackerId, eventTracker);
                }
            }
        }
        return eventTracker;
    }

    boolean isCircuitForcedOpen(String eventTrackerId) {
        return forcedOpenEventTrackerList.contains(eventTrackerId);
    }

    synchronized void start() throws FindException {
        if (!started.get()) {
            initialize();
        }
    }

    synchronized void shutdown() {
        if (started.get()) {
            stopEventCleanupTask();
            started.set(false);
            eventTrackerMap.clear();
        }
    }

    private void initialize() throws FindException {
        scheduleEventCleanupTask(getEventCleanupInterval());
        started.set(true);
        loadForcedOpenEventTrackerList();
    }

    protected void rescheduleEventCleanupTask(final long eventCleanupInterval) throws FindException {
        if (started.get()) {
            LOGGER.log(Level.FINE, "Rescheduling Event Tracker clean up task");
            stopEventCleanupTask();
            scheduleEventCleanupTask(eventCleanupInterval);
        }
    }

    private void scheduleEventCleanupTask(final long eventCleanupInterval) throws FindException {
        if (cleanupTask.get() == null) {
            LOGGER.log(Level.FINE, "Scheduling Event Tracker clean up task");
            cleanupTask.set(new TimerTaskWithGoid(eventCleanupInterval));

            Background.scheduleRepeated(cleanupTask.get(), eventCleanupInterval, eventCleanupInterval);
        } else {
            LOGGER.log(Level.FINE, "Failed to schedule Event Tracker clean up task; a task is already scheduled");
        }
    }

    protected class TimerTaskWithGoid extends TimerTask {
        protected final long interval;
        private Goid cleanupIntervalGoid;
        private Goid eventTrackerListGoid;

        TimerTaskWithGoid(long cleanupInterval) throws FindException {
            this.interval = cleanupInterval;
            setCleanupIntervalGoid();
            setEventTrackerListlGoid();
        }

        @Override
        public void run() {cleanupEvents(interval);}

        protected synchronized void setCleanupIntervalGoid () throws FindException {
            this.cleanupIntervalGoid = getCleanupIntervalCWPGoid();
        }

        protected synchronized void setEventTrackerListlGoid () throws FindException {
            this.eventTrackerListGoid = getEventTrackerListCWPGoid();
        }

        protected Goid getCleanupIntervalGoid () { return cleanupIntervalGoid; }

        protected Goid getEventTrackerListlGoid () {
            return eventTrackerListGoid;
        }

        private Goid getCleanupIntervalCWPGoid() throws FindException {
            try{
                return clusterPropertyManager.findByUniqueName(CB_EVENT_TRACKER_CLEANUP_INTERVAL_UI_PROPERTY).getGoid();
            }
            catch (NullPointerException | FindException e) {
                return Goid.DEFAULT_GOID;
            }
        }

        private Goid getEventTrackerListCWPGoid() throws FindException {
            try{
                return clusterPropertyManager.findByUniqueName(CB_FORCE_EVENT_TRACKER_LIST_CIRCUIT_OPEN_UI_PROPERTY).getGoid();
            }
            catch (NullPointerException | FindException e) {
                return Goid.DEFAULT_GOID;
            }
        }
    }

    private synchronized void stopEventCleanupTask() {
        if (cleanupTask.get() != null) {
            Background.cancel(cleanupTask.get());
            cleanupTask.set(null);
            LOGGER.log(Level.FINE, "Stopping Event Tracker clean up task");
        }
    }

    protected long getEventCleanupInterval() {
        try {
            return Long.valueOf(clusterPropertyManager.getProperty(CB_EVENT_TRACKER_CLEANUP_INTERVAL_UI_PROPERTY));
        } catch (NumberFormatException | FindException e) {
            return CB_EVENT_TRACKER_CLEANUP_INTERVAL_DEFAULT;
        }
    }

    private synchronized void cleanupEvents(long eventCleanupInterval) {
        long interval = TimeUnit.MILLISECONDS.toNanos(eventCleanupInterval);
        long cleanBeforeTimestamp = timeSource.nanoTime() - interval;
        LOGGER.log(Level.FINE, "Cleaning events older than " + eventCleanupInterval + " milliseconds");
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

    protected void handleClusterPropertyChange(EntityInvalidationEvent entityInvalidationEvent) {
        for (Goid oid : entityInvalidationEvent.getEntityIds()) {
            try {
                ClusterProperty clusterProperty = clusterPropertyManager.findByPrimaryKey(oid);
                if (clusterProperty != null && CB_EVENT_TRACKER_CLEANUP_INTERVAL_UI_PROPERTY.equals(clusterProperty.getName())) {
                    rescheduleEventCleanupTask(Long.valueOf(clusterPropertyManager.getProperty(CB_EVENT_TRACKER_CLEANUP_INTERVAL_UI_PROPERTY)));
                }
                else if (clusterProperty == null && oid.equals(getCleanupTask().cleanupIntervalGoid)){
                    rescheduleEventCleanupTask(CB_EVENT_TRACKER_CLEANUP_INTERVAL_DEFAULT);
                }

                if ((clusterProperty != null && CB_FORCE_EVENT_TRACKER_LIST_CIRCUIT_OPEN_UI_PROPERTY.equals(clusterProperty.getName())) ||
                        (clusterProperty == null && oid.equals(getCleanupTask().eventTrackerListGoid) )) {
                    loadForcedOpenEventTrackerList();
                }
            } catch (FindException e) {
                LOGGER.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }
    }

    protected void loadForcedOpenEventTrackerList() {
        try {
            String eventTrackerIds = clusterPropertyManager.getProperty(CB_FORCE_EVENT_TRACKER_LIST_CIRCUIT_OPEN_UI_PROPERTY);
            TimerTaskWithGoid task = getCleanupTask();
            if (StringUtils.isNotEmpty(eventTrackerIds)) {
                forcedOpenEventTrackerList.addAll(Arrays.asList(eventTrackerIds.split("\n")));
            } else {
                forcedOpenEventTrackerList.clear();
            }
            task.setEventTrackerListlGoid();
        } catch (FindException e) {
            LOGGER.log(Level.FINE, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    /**
    *
    * These methods (getCleanupTask) are used for getting / setting values / objects for unit tests only.
    *
    * */
    TimerTaskWithGoid getCleanupTask() {
        return cleanupTask.get();
    }
}