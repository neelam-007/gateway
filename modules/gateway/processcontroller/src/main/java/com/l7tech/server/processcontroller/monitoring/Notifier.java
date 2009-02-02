/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.server.management.config.monitoring.NotificationRule;
import com.l7tech.server.management.config.monitoring.Trigger;
import com.l7tech.util.Closeable;

import java.io.IOException;

public abstract class Notifier implements Closeable {
    protected final NotificationRule rule;

    protected Notifier(NotificationRule rule) {
        this.rule = rule;
    }

    /**
     * Do the notification.
     *
     * @param timestamp the time when the trigger fired (usually quite recent)
     * @param trigger the trigger that fired
     * @throws IOException if the notification could be done
     */
    public abstract void doNotification(Long timestamp, Trigger trigger) throws IOException;

    @Override
    public void close() { }
}
