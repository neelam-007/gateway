package com.l7tech.logging;

import java.text.SimpleDateFormat;
import java.util.Calendar;

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

        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(log.getMillis());
        final SimpleDateFormat sdf = new SimpleDateFormat( "yyyyMMdd HH:mm:ss.SSS" );
        time = sdf.format(cal.getTime());

        msgClass = log.getSourceClassName() != null ? log.getSourceClassName().toString() : null;
        msgMethod = log.getSourceMethodName() != null ? log.getSourceMethodName().toString() : null;

        // Strip redundant info from detail message (Bug #1281)
        String stripex = "^.*?\\s+\\d\\d\\d\\d\\d?\\s+\\d\\d?\\:\\d\\d?\\:\\d\\d?\\s+[AP]M\\s+";
        String severity = log.getLevel().toString() + ":\\s+";
        if (msgClass != null)
            stripex += msgClass.replace('$', '.') + "\\s+";
        if (msgMethod != null)
            stripex += msgMethod.replace('$', '.') + "\\s+" + severity;
        msgDetails = log.getMessage().replaceFirst(stripex, "");
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
}
