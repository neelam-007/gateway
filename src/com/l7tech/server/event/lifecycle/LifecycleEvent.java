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
    public LifecycleEvent(Object source, Component component) {
        super(source);
        this.component = component;
    }

    public abstract String getAction();

    public Component getComponent() {
        return component;
    }

    private final Component component;
}
