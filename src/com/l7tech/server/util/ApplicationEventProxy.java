/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.Map;
import java.util.WeakHashMap;

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

    public void addApplicationListener(ApplicationListener listener) {
        subscribers.put(listener, null);
    }

    public void removeApplicationListener(ApplicationListener listener) {
        subscribers.remove(listener);
    }

    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        for (ApplicationListener applicationListener : subscribers.keySet()) {
            if (applicationListener != null)
                applicationListener.onApplicationEvent(applicationEvent);
        }
    }
}
