/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event.system;

import com.l7tech.common.Component;

/**
 * @author alex
 * @version $Revision$
 */
public class AuditPurgeEvent extends SystemEvent {
    public AuditPurgeEvent( Object source ) {
        super( source, Component.GW_AUDIT_SYSTEM);
    }

    public String getAction() {
        return NAME;
    }

    public static final String NAME = "Purged";
}
