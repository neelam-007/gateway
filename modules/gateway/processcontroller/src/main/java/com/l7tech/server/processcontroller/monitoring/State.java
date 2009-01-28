/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.server.management.config.monitoring.Trigger;

import java.util.List;

/**
 * A holder for monitoring state, consisting of a {@link Trigger} and a list of the {@link NotificationState}s that
 * track the temporal progress of the trigger's associated notification rules.
 */
abstract class State<T extends Trigger> {
    protected final T trigger;
    protected final List<NotificationState> notificationStates;

    protected State(T trigger, List<NotificationState> notificationStates) {
        this.trigger = trigger;
        this.notificationStates = notificationStates;
    }

    public T getTrigger() {
        return trigger;
    }

    public List<NotificationState> getNotificationStates() {
        return notificationStates;
    }
}
