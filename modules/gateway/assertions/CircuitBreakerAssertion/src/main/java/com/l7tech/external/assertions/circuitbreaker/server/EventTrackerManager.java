package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.util.Background;
import com.l7tech.util.TimeSource;

import javax.inject.Inject;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages Event Tracker.
 *
 * @author Ekta Khandelwal - khaek01@ca.com
 *
 */
public class EventTrackerManager {

    private static final Logger logger = Logger.getLogger(EventTrackerManager.class.getName());
    private final ConcurrentHashMap<String, EventTracker> eventTrackerMap = new ConcurrentHashMap<>();
    private final Object lock = new Object();

    //Clean up events after 10 minutes interval
    private static long counterCleanupInterval = 10L * 60 * 1000;

    private TimerTask TIMER_TASK;

    @Inject
    private TimeSource timeSource;

    EventTracker getEventTracker(final String trackerId) {
        return eventTrackerMap.get(trackerId);
    }

    EventTracker createEventTracker(final String trackerId) {
        EventTracker eventTracker;
        synchronized(lock) {
            eventTracker = eventTrackerMap.get(trackerId);
            if (null == eventTracker) {
                eventTracker = new EventTracker();
                //Add EventTracker if not present in eventTrackerMap
                eventTrackerMap.put(trackerId, eventTracker);
            }
        }
        return eventTracker;
    }

    void start() {
        // TODO: get cleanup task interval from cluster property when implemented
        TIMER_TASK = initializeEventCleanupTask();
    }

    void shutdown() {
        TIMER_TASK.cancel();
        eventTrackerMap.clear();
    }

    private TimerTask initializeEventCleanupTask() {
       //Run a schedule job to clean up old events at regular interval
        final TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                long interval = TimeUnit.MILLISECONDS.toNanos(counterCleanupInterval);
                logger.log(Level.FINE, "--------Starting Event Tracker clean up scheduled task--------");
                long cleanBeforeTimestamp = timeSource.nanoTime() - interval;
                for (Map.Entry<String, EventTracker> entry : eventTrackerMap.entrySet()) {
                    entry.getValue().clearTimestampBefore(cleanBeforeTimestamp);
                }
            }
        };
        Background.scheduleRepeated(timerTask, counterCleanupInterval, counterCleanupInterval);
        return timerTask;
   }

   /**
    *
    * These methods (setCounterCleanupInterval, getTIMER_TASK) are used for getting / setting values / objects for unit tests only.
    *
    * */
    static void setCounterCleanupInterval(long counterCleanupInterval) {
        EventTrackerManager.counterCleanupInterval = counterCleanupInterval;
    }

    TimerTask getTIMER_TASK() {
        return TIMER_TASK;
    }
}
