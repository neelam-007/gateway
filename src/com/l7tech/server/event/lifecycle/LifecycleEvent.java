/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event.lifecycle;

import com.l7tech.common.Component;
import com.l7tech.objectmodel.event.Event;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class LifecycleEvent extends Event {
    public LifecycleEvent(Object source, Component component, String ipAddress) {
        super(source);
        this.component = component;
        this.ipAddress = ipAddress;
    }

    public abstract String getAction();

    public Component getComponent() {
        return component;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    private final Component component;
    private final String ipAddress;
}
