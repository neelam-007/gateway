/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event.system;

import com.l7tech.common.Component;

import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class Closed extends SystemEvent {
    public Closed(Object source, Component component, String ip) {
        super(source, component, ip, Level.INFO);
    }

    public String getAction() {
        return NAME;
    }

    public static final String NAME = "Closed";
}
