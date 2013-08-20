/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.gateway.common.alert;

import com.l7tech.objectmodel.imp.NamedGoidEntityImp;

import java.util.Set;

/**
 * Configuration of an event listener that listens to system events and invokes one or more actions
 * (configured with {@link Notification} objects) as a result.
 */
public abstract class AlertEvent extends NamedGoidEntityImp {
    private Set notifications;

    public Set getNotifications() {
        return notifications;
    }

    public void setNotifications(Set notifications) {
        this.notifications = notifications;
    }
}
