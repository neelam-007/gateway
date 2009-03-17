package com.l7tech.server.util;

/**
 * Represents an alternative event channel, that can be used to deliver events directly to subscribers within
 * the same process, with delivery occurring on the same thread that publishes the event, without
 * having to clutter up the Spring context's primary application event channel.
 */
public class EventChannel extends ApplicationEventProxy {
    public EventChannel() {
        super(false);
    }
}
