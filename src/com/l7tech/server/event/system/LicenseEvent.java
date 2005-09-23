/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.event.system;

import com.l7tech.common.Component;

import java.util.logging.Level;

/**
 * Event fired when license status changes.
 */
public class LicenseEvent extends SystemEvent {
    private final String message;

    public LicenseEvent(Object source, Level level, String message) {
        super(source, Component.GW_LICENSE_MANAGER, null, level);
        this.message = message;
    }

    public String getAction() {
        return message;
    }
}
