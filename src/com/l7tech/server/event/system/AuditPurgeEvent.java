/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event.system;

import com.l7tech.common.Component;
import com.l7tech.common.audit.SystemAuditRecord;

import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class AuditPurgeEvent extends SystemEvent {
    private static final Component COMPONENT = Component.GW_AUDIT_SYSTEM;
    private static final String NAME = "Purged";

    private final int numDeleted;
    private SystemAuditRecord systemAuditRecord; // TODO surely there must be some better way

    //- PUBLIC

    public AuditPurgeEvent( Object source, int numDeleted ) {
        super(source, COMPONENT, null, Level.INFO, buildMessage(numDeleted));
        this.numDeleted = numDeleted;
    }

    public String getAction() {
        return buildAction(numDeleted);
    }

    public static String buildAction(int numDeleted) {
        return NAME + " " + numDeleted + " Audit records";
    }

    public static String buildMessage(int numDeleted) {
        return COMPONENT.getName() + " " + buildAction(numDeleted);
    }

    public SystemAuditRecord getSystemAuditRecord() {
        return systemAuditRecord;
    }

    public void setSystemAuditRecord(SystemAuditRecord systemAuditRecord) {
        this.systemAuditRecord = systemAuditRecord;
    }
}
