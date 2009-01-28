/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.server.management.config.monitoring.NotificationRule;

public abstract class Notifier {
    private final NotificationRule rule;

    protected Notifier(NotificationRule rule) {
        this.rule = rule;
    }

    public abstract void doNotification();
}
