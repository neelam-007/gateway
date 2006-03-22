/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event.system;

import java.util.logging.Level;

import com.l7tech.common.Component;

/**
 * @author alex
 * @version $Revision$
 */
public class AuditPurgeEvent extends SystemEvent {

    //- PUBLIC

    public AuditPurgeEvent( Object source, int numDeleted ) {
        super(source, COMPONENT, null, Level.INFO, buildMessage(numDeleted));
        this.numDeleted = numDeleted;
    }

    public String getAction() {
        return buildAction(numDeleted);
    }

    //- PRIVATE

    private static String buildAction(int numDeleted) {
        return NAME + " " + numDeleted + " Audit records";
    }

    private static String buildMessage(int numDeleted) {
        return COMPONENT.getName() + " " + buildAction(numDeleted);
    }

    private static final Component COMPONENT = Component.GW_AUDIT_SYSTEM;
    private static final String NAME = "Purged";
    
    private int numDeleted;
}
