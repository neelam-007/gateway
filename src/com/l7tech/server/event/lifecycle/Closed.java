/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event.lifecycle;

import com.l7tech.common.Component;

/**
 * @author alex
 * @version $Revision$
 */
public class Closed extends LifecycleEvent {
    public Closed( Object source, Component component ) {
        super(source, component);
    }

    public String getAction() {
        return NAME;
    }

    public static final String NAME = "Closed";
}
