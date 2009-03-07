/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util;

import com.l7tech.server.policy.AssertionModuleUnregistrationEvent;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Non-transactional bean that gathers and re-dispatches application events.
 * This is used to work around a massive performance problem when a transactional bean's onApplicationEvent()
 * handler gets wrapped in expensive Spring TX-checking plumbing, which was otherwise going to cause
 * a 30% performance decrease system-wide between Gateway versions 3.5 and 3.6.
 */
public class ApplicationEventProxy implements ApplicationListener, DisposableBean {
    final Set<ApplicationListener> subscribers = new CopyOnWriteArraySet<ApplicationListener>();

    /**
     * Add an application listener.  The subscription will persist until the application context is closed,
     * the module to which the subscriber belongs is unloaded, or the subscription is manually removed
     * by a call to {@link #removeApplicationListener(org.springframework.context.ApplicationListener)}.
     *
     * @param listener The ApplicationListener to subscribe to events.
     */
    public void addApplicationListener(ApplicationListener listener) {
        if (listener == null) throw new NullPointerException("listener");
        subscribers.add(listener);
    }

    /**
     * Unsubscribe from application events.
     *
     * @param listener The ApplicationListener to unsubscribe from events.
     */
    public void removeApplicationListener(ApplicationListener listener) {
        subscribers.remove(listener);
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof AssertionModuleUnregistrationEvent)
            removeListenersFromClassLoader(((AssertionModuleUnregistrationEvent)event).getModule().getModuleClassLoader());

        for (ApplicationListener applicationListener : subscribers)
            applicationListener.onApplicationEvent(event);
    }

    void removeListenersFromClassLoader(ClassLoader classLoader) {
        List<ApplicationListener> subs = new ArrayList<ApplicationListener>(subscribers);
        List<ApplicationListener> toRemove = new ArrayList<ApplicationListener>();
        for (ApplicationListener sub : subs) {
            if (sub.getClass().getClassLoader() == classLoader)
                toRemove.add(sub);
        }
        subscribers.removeAll(toRemove);
    }

    public void destroy() throws Exception {
        subscribers.clear();
    }
}
