/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.audit;

import com.l7tech.logging.SSGLogRecord;

import java.util.logging.Level;

/**
 * Abstract superclass of all of the different types of audit record.
 *
 * Note that audit records should be treated as immutable, although they still need non-final fields and setters
 * for persistence purposes.
 *
 * @author alex
 * @version $Revision$
 */
public abstract class AuditRecord extends SSGLogRecord {
    /** @deprecated to be called only for serialization and persistence purposes! */
    protected AuditRecord() {
    }

    /**
     * Fills in the fields that are common to all types of AuditRecord
     * @param level the {@link Level} of this record.
     * @param nodeId the ID of the cluster node from which this AuditRecord originates (see {@link com.l7tech.cluster.ClusterStatusAdmin#getClusterStatus()})
     * @param ipAddress the IP address of the entity that caused this AuditRecord to be created.  It could be that of a cluster node, an administrative workstation or a web service requestor.
     * @param name the name of the service or system affected by event that generated the AuditRecord
     * @param message a short description of the event that generated the AuditRecord
     */
    protected AuditRecord(Level level, String nodeId, String ipAddress, String name, String message) {
        super(level, nodeId, message);
        this.name = name;
        this.ipAddress = ipAddress;
    }

    /**
     * Gets the IP address of the entity that caused this AuditRecord to be created.  It could be that of a cluster node, an administrative workstation or a web service requestor.
     * @return the IP address of the entity that caused this AuditRecord to be created.
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Gets the name of the service or system affected by event that generated the AuditRecord
     * @return the name of the service or system affected by event that generated the AuditRecord
     */
    public String getName() {
        return name;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setIpAddress( String ipAddress ) {
        this.ipAddress = ipAddress;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setName( String name ) {
        this.name = name;
    }

    /** the IP address of the entity that caused this AuditRecord to be created. */
    protected String ipAddress;
    
    /** the name of the service or system affected by event that generated the AuditRecord */
    protected String name;
}
