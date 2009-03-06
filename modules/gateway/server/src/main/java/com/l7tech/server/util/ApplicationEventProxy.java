/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.Set;

/**
 * Non-transactional bean that gathers and re-dispatches application events.
 * This is used to work around a massive performance problem when a transactional bean's onApplicationEvent()
 * handler gets wrapped in expensive Spring TX-checking plumbing, which was otherwise going to cause
 * a 30% performance decrease system-wide between Gateway versions 3.5 and 3.6.
 */
public class ApplicationEventProxy implements ApplicationListener {
    private final Map<ApplicationListener, Object> subscribers = new WeakHashMap<ApplicationListener, Object>();

    public ApplicationEventProxy() {
    }

    /**
     * Add an application listener.
     *
     * <p>WARNING: You must hold a reference to the listener to prevent it from
     * being garbage collected.</p>
     *
     * @param listener The ApplicationListener to subscribe to events.
     */
    public synchronized void addApplicationListener(ApplicationListener listener) {
        subscribers.put(listener, null);
    }

    /**
     * Unsubscribe from application events.
     *
     * @param listener The ApplicationListener to unsubscribe from events.
     */
    public synchronized void removeApplicationListener(ApplicationListener listener) {
        subscribers.remove(listener);
    }

    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        final Set<ApplicationListener> listeners;
        synchronized (this) {
            listeners = subscribers.keySet();
        }
        for (ApplicationListener applicationListener : listeners) {
            if (applicationListener != null)
                applicationListener.onApplicationEvent(applicationEvent);
        }
    }
}
