/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.audit;

import com.l7tech.objectmodel.imp.EntityImp;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

/**
 * An audit detail record.
 */
public class AuditDetail extends EntityImp implements Serializable {
    private transient AuditRecord auditRecord;
    private long auditOid;
    private long time;
    private int messageId;
    /** can be null */
    private String[] params;
    private int componentId;
    private String exception;

    public AuditDetail(AuditDetailMessage message) {
        this(message, null, (Throwable)null);
    }

    public AuditDetail(AuditDetailMessage message, String param1) {
        this(message, param1 == null ? null : new String[] { param1 }, null);
    }

    public AuditDetail(AuditDetailMessage message, String param1, String param2) {
        this(message, (param1 == null && param2 == null) ? null : new String[] { param1, param2 }, null);
    }

    public AuditDetail(AuditDetailMessage message, String[] params, Throwable e) {
        // auditOid is probably not known at this time but will be by the time it's saved
        this.time = System.currentTimeMillis();
        this.messageId = message.getId();
        this.params = params;
        if (e != null) {
            StringWriter sw = new StringWriter(4096);
            e.printStackTrace(new PrintWriter(sw));
            this.exception = sw.toString();
        }
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

    public int getMessageId() {
        return messageId;
    }

    public long getAuditOid() {
        return auditOid;
    }

    /** Can be null. */
    public String[] getParams() {
        return params;
    }

    /**
     * Not deprecated because this needs to be set later on
     * @param oid
     */
    public void setAuditOid(long oid) {
        this.auditOid = oid;
    }

    public String getException() {
        return exception;
    }

    public int getComponentId() {
        return componentId;
    }

    /** @deprecated only for persistence */
    public AuditDetail() {
    }

    /** @deprecated only for persistence */
    public void setComponentId(int componentId) {
        this.componentId = componentId;
    }

    /** @deprecated only for persistence */
    public void setTime(long time) {
        this.time = time;
    }

    /** @deprecated only for persistence */
    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    /** @deprecated only for persistence */
    public void setParams(String[] params) {
        this.params = params;
    }

    /** @deprecated only for persistence */
    public void setException(String exception) {
        this.exception = exception;
    }
}
