/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event.system;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.SystemAuditRecord;

import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class AuditPurgeEvent extends SystemEvent {
    private static final Component COMPONENT = Component.GW_AUDIT_SYSTEM;
    private static final String NAME = "Purged";

    private final int numDeleted;
    /** Indicates if this purge event is to create a new record or update an existing record. */
    private final boolean update;
    private SystemAuditRecord systemAuditRecord; // TODO surely there must be some better way

    //- PUBLIC

    /**
     * Constructs an event for creating a new audit purge record.
     *
     * @param source        the component that published the event
     * @param numDeleted    the deleted count
     */
    public AuditPurgeEvent(Object source, int numDeleted) {
        super(source, COMPONENT, null, Level.INFO, buildMessage(numDeleted));
        this.numDeleted = numDeleted;
        this.update = false;
    }

    /**
     * Constructs an event for updating an existing audit purge record.
     *
     * @param source            the component that published the event
     * @param recordToUpdate    the system audit record to be updated; supply the original record when calling, it will be updated upon exit
     * @param numDeleted        the new deleted count
     */
    public AuditPurgeEvent(Object source, SystemAuditRecord recordToUpdate, int numDeleted) {
        super(source, COMPONENT, null, Level.INFO, buildMessage(numDeleted));
        this.numDeleted = numDeleted;
        this.update = true;
        recordToUpdate.setAction(buildAction(numDeleted));
        recordToUpdate.setMessage(buildMessage(numDeleted));
        recordToUpdate.setMillis(System.currentTimeMillis());
        setSystemAuditRecord(recordToUpdate);
    }

    /**
     * @return true if this event is to update an existing record;
     *         false if this event is to create a new record
     */
    public boolean isUpdate() {
        return update;
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
