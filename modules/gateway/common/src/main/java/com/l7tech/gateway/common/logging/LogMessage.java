package com.l7tech.gateway.common.logging;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class encapsulates an SSGLogRecord and contains further information for displaying it in a UI in a
 * friendly way.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */
public class LogMessage implements Comparable {
    private final SSGLogRecord log;
    private final String time;
    private final String msgClass;
    private final String msgMethod;
    private final String msgDetails;
    private String nodeName = "";  // gets filled in afterward

    public LogMessage(SSGLogRecord log) {
        this.log = log;
        if (log == null) throw new NullPointerException();

        SimpleDateFormat sdf = new SimpleDateFormat( "yyyyMMdd HH:mm:ss.SSS" );
        String details = log.getMessage();
        if(details==null) details = "";
        
        time = sdf.format(new Date(log.getMillis()));
        msgClass = log.getSourceClassName();
        msgMethod = log.getSourceMethodName();
        msgDetails = details;
    }

    public long getMsgNumber() {
        return log.getOid();
    }

    public String getTime() {
        return time;
    }

    public String getSeverity() {
        return log.getLevel().toString();
    }

    public String getMsgClass() {
        return msgClass;
    }

    public String getMsgMethod() {
        return msgMethod;
    }

    public String getMsgDetails(){
        return msgDetails;
    }

    public String getReqId() {
        return log.getReqId() == null ? null : log.getReqId().toString();
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getNodeId() {
        return log.getNodeId();
    }

    public SSGLogRecord getSSGLogRecord() {
        return log;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogMessage that = (LogMessage) o;

        if (log.getOid() != that.log.getOid()) return false;
        if (nodeName != null ? !nodeName.equals(that.nodeName) : that.nodeName != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = Long.valueOf(log.getOid()).hashCode();
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
            if ( other.getSSGLogRecord().getMillis() < getSSGLogRecord().getMillis()) {
                compareValue = -1;
            } else if ( other.getSSGLogRecord().getMillis() > getSSGLogRecord().getMillis()) {
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
