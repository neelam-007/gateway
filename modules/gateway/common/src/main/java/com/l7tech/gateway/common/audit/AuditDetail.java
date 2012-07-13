/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */
package com.l7tech.gateway.common.audit;

import com.l7tech.objectmodel.imp.PersistentEntityImp;

import java.io.*;
import java.util.Arrays;
import java.text.MessageFormat;
import java.text.FieldPosition;

/**
 * An audit detail record.
 */
public class AuditDetail extends PersistentEntityImp implements Serializable, Comparable {
    private transient AuditRecord auditRecord;
    private String auditGuid;
    private long time;
    private int messageId;
    /** can be null */
    private String[] params;
    private int componentId;
    private String exception;

    // used to determine the order of the messages as the timestamp (ms) of the messages may be the same.
    private int ordinal;

    public AuditDetail(AuditDetailMessage message) {
        this(message, null, (Throwable)null);
    }

    public AuditDetail(AuditDetailMessage message, String param1) {
        this(message, param1 == null ? null : new String[] { param1 }, null);
    }

    public AuditDetail(AuditDetailMessage message, String param1, String param2) {
        this(message, (param1 == null && param2 == null) ? null : new String[] { param1, param2 }, null);
    }

    public AuditDetail(AuditDetailMessage message, String... params) {
        this(message, params, null);
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


    public String getAuditGuid() {
        return auditGuid;
    }

    public int getOrdinal() {
        return ordinal;
    }

    /** Can be null. */
    public String[] getParams() {
        return params;
    }


    /**
     * Not deprecated because this needs to be set later on
     * @param auditGuid
     */
    public void setAuditGuid(String auditGuid) {
        this.auditGuid = auditGuid;
    }

    public String getException() {
        return exception;
    }

    public int getComponentId() {
        return componentId;
    }

    public void setOrdinal(int ordinal) {
         this.ordinal = ordinal;
     }

    /** @deprecated only for persistence */
    @Deprecated
    public AuditDetail() {
    }

    /** @deprecated only for persistence */
    @Deprecated
    public void setComponentId(int componentId) {
        this.componentId = componentId;
    }

    /** @deprecated only for persistence */
    @Deprecated
    public void setTime(long time) {
        this.time = time;
    }

    /** @deprecated only for persistence */
    @Deprecated
    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    /** @deprecated only for persistence */
    @Deprecated
    public void setParams(String[] params) {
        this.params = params;
    }

    /** @deprecated only for persistence */
    @Deprecated
    public void setException(String exception) {
        this.exception = exception;
    }

    @Override
    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final AuditDetail that = (AuditDetail) o;

        if (auditGuid != null ? !auditGuid.equals(that.auditGuid) : that.auditGuid != null) return false;
        if (componentId != that.componentId) return false;
        if (messageId != that.messageId) return false;
        if (ordinal != that.ordinal) return false;
        if (time != that.time) return false;
        if (auditRecord != null ? !auditRecord.equals(that.auditRecord) : that.auditRecord != null) return false;
        if (exception != null ? !exception.equals(that.exception) : that.exception != null) return false;
        if (!Arrays.equals(params, that.params)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + (auditGuid != null ? auditGuid.hashCode() : 0);
        result = 29 * result + (int) (time ^ (time >>> 32));
        result = 29 * result + messageId;
        result = 29 * result + componentId;
        result = 29 * result + (exception != null ? exception.hashCode() : 0);
        result = 29 * result + ordinal;
        return result;
    }

    public void serializeSignableProperties(OutputStream out) throws IOException {
        out.write(Integer.toString(messageId).getBytes());
        out.write("\\".getBytes());
        out.write(AuditRecord.SERSEP.getBytes());

        AuditDetailMessage message = MessagesUtil.getAuditDetailMessageById(messageId);
        String reConstructedMsg = message==null ? null : message.getMessage();
        StringBuffer tmp = new StringBuffer();
        if (reConstructedMsg != null && params != null) {
            MessageFormat mf = new MessageFormat(reConstructedMsg);
            mf.format(params, tmp, new FieldPosition(0));
            reConstructedMsg = tmp.toString();
        }
        if (reConstructedMsg != null && reConstructedMsg.length() > 0) {
            out.write(reConstructedMsg.getBytes());
        }
    }

    @Override
    public int compareTo(Object o) {
        if (!(o instanceof AuditDetail)) {
            // should not happen
            throw new RuntimeException("you can't compare AuditDetails and " + o.getClass().getName() + "s");
        }
        AuditDetail other = (AuditDetail)o;
        return this.getOrdinal() - other.getOrdinal();
    }
}
