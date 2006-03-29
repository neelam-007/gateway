package com.l7tech.logging;

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
public class LogMessage {
    private final SSGLogRecord log;
    private final String time;
    private final String msgClass;
    private final String msgMethod;
    private final String msgDetails;
    private String nodeName = "";  // gets filled in afterward

    public LogMessage(SSGLogRecord log) {
        this.log = log;

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

    public boolean equals(Object other) {
        boolean equal = false;

        if(this==other) {
            equal = true;
        }
        else if(other instanceof LogMessage) {
            LogMessage om = (LogMessage) other;
            equal = log.getMillis()==om.log.getMillis() &&
                    log.getLevel().equals(om.log.getLevel()) &&
                    log.getMessage().equals(om.log.getMessage());
        }

        return equal;
    }
}
