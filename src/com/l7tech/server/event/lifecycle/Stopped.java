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
public class Stopped extends LifecycleEvent {
    public Stopped(Object source, Component component, String ip) {
        super(source, component, ip);
    }

    public String getAction() {
        return NAME;
    }

    private static final String NAME = "Stopped";

}
