/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.event.system;

import com.l7tech.gateway.common.Component;

import java.util.logging.Level;

/**
 * Event fired when license status changes.
 */
public class LicenseEvent extends SystemEvent {
    private final String action;

    public LicenseEvent(Object source, Level level, String action, String message) {
        super(source, Component.GW_LICENSE_MANAGER, null, level, message);
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}