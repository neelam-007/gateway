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
public class Stopping extends LifecycleEvent {
    public Stopping( Object source, Component component ) {
        super( source, component );
    }

    public String getAction() {
        return NAME;
    }

    private static final String NAME = "Stopping";
}
