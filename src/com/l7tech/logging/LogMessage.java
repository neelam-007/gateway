package com.l7tech.logging;

import com.l7tech.common.RequestId;

import java.util.StringTokenizer;
import java.util.Calendar;
import java.text.SimpleDateFormat;

/*
 * This class encapsulates the log message.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class LogMessage {
    private long msgNumber = 0;
    private String time = null;
    private String severity = null;
    private String msgClass = null;
    private String msgMethod = null;
    private String msgDetails = null;
    private String nodeName = "";
    private String reqId = "";
    SimpleDateFormat sdf = new SimpleDateFormat( "yyyyMMdd HH:mm:ss.SSS" );

    public LogMessage(SSGLogRecord log) {
        msgNumber = log.getSequenceNumber();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(log.getMillis());
        time = sdf.format(cal.getTime());
        severity = log.getLevel().toString();
        msgClass = log.getSourceClassName().toString();
        msgMethod = log.getSourceMethodName().toString();
        msgDetails = log.getMessage();

        if(log.getReqId() != null) {
             reqId = log.getReqId().toString();
        }
    }

    public LogMessage(String log){

        //System.out.println(log);
        StringTokenizer st = new StringTokenizer(log, "|");

        int index = 0;
        while(st.hasMoreTokens()){
          String s = st.nextToken();
               if(index == 0){
                   msgNumber = Long.parseLong(s);
                   index++;
               }
               else if (index == 1){
                   time = s;
                   index++;
               }
               else if (index == 2){
                   severity = s;
                   index++;
               }
               else if (index == 3) {
                   msgClass = s;
                   index++;
               }
                else if(index == 4){
                    msgMethod = s;
                    index++;
                }
                else if(index == 5){
                    msgDetails = s;
                    index++;
                }
                else
                {
                    msgDetails = msgDetails + s;
                }

          }

    }

    public long getMsgNumber() {
        return msgNumber;
    }
    public String getTime() {
        return time;
    }

    public String getSeverity() {
        return severity;
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
        return reqId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public static void main(String[] args) {
        LogMessage logRec = new LogMessage("11|20030917 13:07:36.281|INFO|com.l7tech.identity.internal.InternalIdentityProviderServer|authenticate|Couldn't find user with login admin");
        System.out.println("msgNumber : " + logRec.getMsgNumber());
        System.out.println("time: " + logRec.getTime());
        System.out.println("severity: " + logRec.getSeverity());
        System.out.println("msgClass: " + logRec.getMsgClass());
        System.out.println("msgMethod: " + logRec.getMsgMethod());
        System.out.println("msgDetails: " + logRec.getMsgDetails());

    }
}
