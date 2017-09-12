package com.l7tech.external.assertions.circuitbreaker.server;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
class EventTrackerManagerHolder {

    private static EventTrackerManager eventTrackerManager;

    static EventTrackerManager getEventTrackerManager() {
        return eventTrackerManager;
    }

    static void setEventTrackerManager(EventTrackerManager eventTrackerManager) {
        EventTrackerManagerHolder.eventTrackerManager = eventTrackerManager;
    }
}
