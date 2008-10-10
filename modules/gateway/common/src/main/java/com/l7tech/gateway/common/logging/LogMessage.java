package com.l7tech.gateway.common.logging;

import com.l7tech.gateway.common.audit.AuditRecordHeader;
import com.l7tech.gateway.common.audit.MessageSummaryAuditRecord;
import com.l7tech.gateway.common.audit.AuditRecord;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class encapsulates an SSGLogRecord and/or an AuditRecordHeader and contains further information for displaying it in a UI in a
 * friendly way.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */
public class LogMessage implements Comparable {
    private AuditRecordHeader header = null;
    private SSGLogRecord log = null;
    private final String time;
    private String nodeName = "";  // gets filled in afterward

    public LogMessage(AuditRecordHeader header){
        this.header = header;
        if(header == null) throw new NullPointerException();

        SimpleDateFormat sdf = new SimpleDateFormat( "yyyyMMdd HH:mm:ss.SSS" );
        time = sdf.format(new Date(header.getTimestamp()));
    }

    public LogMessage(SSGLogRecord log) {
        this.log = log;
        if (log == null) throw new NullPointerException();

        SimpleDateFormat sdf = new SimpleDateFormat( "yyyyMMdd HH:mm:ss.SSS" );
        time = sdf.format(new Date(log.getMillis()));
    }

    public long getMsgNumber() {
        if(log != null) return log.getOid();
        return header.getOid();
    }

    public String getTime() {
        return time;
    }

    public String getSeverity() {
        if(log != null) return log.getLevel().toString();
        return header.getLevel().toString();
    }

    public String getMsgClass() {
        if(log != null) return log.getSourceClassName();
        return "";
    }

    public String getMsgMethod() {
        if(log != null) return log.getSourceMethodName();
        return "";
    }

    public String getMsgDetails(){
        if(log != null) return log.getMessage() == null ? "" : log.getMessage();
        return header.getMessage() == null ? "" : header.getMessage();
    }

    public String getReqId() {
        return (log == null || log.getReqId() == null) ? null : log.getReqId().toString();
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getServiceName() {
        if(log != null && log instanceof MessageSummaryAuditRecord) return ((MessageSummaryAuditRecord) log).getName();
        if(header != null) return header.getServiceName();
        return "";
    }

    public String getNodeId() {
        if(log != null) return log.getNodeId();
        return header.getNodeId();
    }

    public SSGLogRecord getSSGLogRecord() {
        return log;
    }

    public void setLog(SSGLogRecord log) {
        this.log = log;
    }

    public AuditRecordHeader getHeader() {
        return header;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogMessage that = (LogMessage) o;

        if (getMsgNumber() != that.getMsgNumber()) return false;
        if (nodeName != null ? !nodeName.equals(that.nodeName) : that.nodeName != null) return false;

        return true;
    }

    public int hashCode() {
        int result;

        result = Long.valueOf(getMsgNumber()).hashCode();
        result = 31 * result + (nodeName != null ? nodeName.hashCode() : 0);
        return result;
    }

    public int compareTo(Object o) {
        int compareValue;

        if (!(o instanceof LogMessage)) {
            throw new IllegalStateException("Can only compare to other LogMessages ("+o.getClass()+")");
        }

        LogMessage other = (LogMessage) o;

        if ( this.equals(other) ) {
            compareValue = 0;
        } else {
            if ( other.getHeader().getTimestamp() < getHeader().getTimestamp()) {
                compareValue = -1;
            } else if ( other.getHeader().getTimestamp() > getHeader().getTimestamp()) {
                compareValue = 1;
            } else {
                if (other.getNodeId().compareTo(getNodeId()) == -1) {
                    compareValue = -1;
                } else if (other.getNodeId().compareTo(getNodeId()) == 1) {
                    compareValue = 1;
                } else {
                    // this may not be meaningful for audit records, but is at least definitive
                    compareValue = Long.valueOf(other.getMsgNumber()).compareTo(getMsgNumber());
                }
            }
        }

        return compareValue;
    }
}
