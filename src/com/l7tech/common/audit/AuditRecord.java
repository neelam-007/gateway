/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.audit;

import com.l7tech.logging.SSGLogRecord;

import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class AuditRecord extends SSGLogRecord {
    /** @deprecated to be called only for serialization and persistence purposes! */
    protected AuditRecord() {
    }

    public AuditRecord(Level level, String nodeId, String ipAddress, String message) {
        super(level, nodeId, message);
        this.ipAddress = ipAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setIpAddress( String ipAddress ) {
        this.ipAddress = ipAddress;
    }

    protected String ipAddress;
}
