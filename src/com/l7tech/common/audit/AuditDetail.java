/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.audit;

import com.l7tech.objectmodel.imp.EntityImp;

import java.io.Serializable;

/**
 * An audit detail record.
 */
public class AuditDetail extends EntityImp implements Serializable {
    private transient AuditRecord auditRecord;
    private long auditOid;
    private long time;
    private long componentOid;
    private long messageOid;
    private int ordinal;

    public AuditDetail(long time, long componentOid, long messageOid, int ordinal) {
        // auditOid is probably not known at this time
        this.time = time;
        this.componentOid = componentOid;
        this.messageOid = messageOid;
        this.ordinal = ordinal;
    }

    public AuditRecord getAuditRecord() {
        return auditRecord;
    }

    public void setAuditRecord(AuditRecord auditRecord) {
        this.auditRecord = auditRecord;
    }

    public long getTime() {
        return time;
    }

    public long getComponentOid() {
        return componentOid;
    }

    public long getMessageOid() {
        return messageOid;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public long getAuditOid() {
        return auditOid;
    }

    /**
     * Not deprecated because this needs to be set later on
     * @param oid
     */
    public void setAuditOid(long oid) {
        this.auditOid = oid;
    }

    /** @deprecated only for persistence */
    public AuditDetail() {
    }

    /** @deprecated only for persistence */
    public void setTime(long time) {
        this.time = time;
    }

    /** @deprecated only for persistence */
    public void setComponentOid(long componentOid) {
        this.componentOid = componentOid;
    }

    /** @deprecated only for persistence */
    public void setMessageOid(long messageOid) {
        this.messageOid = messageOid;
    }

    /** @deprecated only for persistence */
    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

}
