package com.ca.apim.gateway.extension.event;

import com.ca.apim.gateway.extension.Extension;

/**
 * Defines a type of extension that listen to certain types of events fired by the gateway.
 *
 * <E> Event
 */
public interface EventListener<E extends Event> extends Extension {

    /**
     * Executes the operation when an event is triggered.
     *
     * @param event the event
     */
    void onEvent(E event);

    /**
     * Returns the event type that this listener listens to.
     *
     * @return the event class
     */
    Class<E> supportedEventType();
}
