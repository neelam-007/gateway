package com.l7tech.gateway.common.cluster;

import com.l7tech.gateway.common.audit.AuditType;

import java.util.Date;
import java.util.logging.Level;

/*
 * This class encapsultes the request for retrieving logs from cluster nodes.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public final class LogRequest {

    private final String nodeName;
    private long startMsgNumber;
    private long endMsgNumber;
    private final Date startMsgDate;
    private Date endMsgDate;
    private final Level logLevel;
    private final String serviceName;
    private final String message;
    private final String requestId;
    private final AuditType auditType;

    private int retrievedLogCount;

    public LogRequest(LogRequest lr) {
        this.nodeName = lr.getNodeName();
        this.endMsgNumber = lr.getEndMsgNumber();
        this.startMsgNumber = lr.getStartMsgNumber();
        this.startMsgDate = lr.getStartMsgDate();
        this.endMsgDate = lr.getEndMsgDate();
        this.logLevel = lr.getLogLevel();
        this.serviceName = lr.getServiceName();
        this.message = lr.getMessage();
        this.requestId = lr.getRequestId();
        this.auditType = lr.getAuditType();
    }

    private LogRequest(Builder builder) {
        nodeName = builder.nodeName;
        endMsgNumber = builder.endMsgNumber;
        startMsgNumber = builder.startMsgNumber;
        startMsgDate = builder.startMsgDate;
        endMsgDate = builder.endMsgDate;
        logLevel = builder.logLevel;
        serviceName = builder.serviceName;
        message = builder.message;
        requestId = builder.requestId;
        auditType = builder.auditType;
        retrievedLogCount = 0;
    }

    public static class Builder{
        private String nodeName = null;
        private long startMsgNumber = -1;
        private long endMsgNumber = -1;
        private Date startMsgDate = null;
        private Date endMsgDate = null;
        private Level logLevel = Level.WARNING;
        private String serviceName = null;
        private String message = null;
        private String requestId = null;
        private AuditType auditType = AuditType.ALL;

        public Builder(){
        }

        public Builder nodeName(String value){
            nodeName = value;
            return this;
        }

        public Builder startMsgNumber(long value){
            startMsgNumber = value;
            return this;
        }

        public Builder endMsgNumber(long value){
            endMsgNumber = value;
            return this;
        }

        public Builder startMsgDate(Date value){
            startMsgDate = value;
            return this;
        }

        public Builder endMsgDate(Date value){
            endMsgDate = value;
            return this;
        }

        public Builder logLevel(Level value){
            logLevel = value;
            return this;
        }

        public Builder serviceName(String value){
            serviceName = value;
            return this;
        }

        public Builder message(String value){
            message = value;
            return this;
        }

        public Builder requestId(String value){
            requestId = value;
            return this;
        }

        public Builder auditType(AuditType value){
            auditType = value;
            return this;
        }

        public LogRequest build(){
            return new LogRequest(this);
        }
    }

    /**
     * Return the retrieved log count.
     *
     * @return int  The retrieved log count.
     */
    public int getRetrievedLogCount() {
        return retrievedLogCount;
    }

    /**
     * Set the retrieved log count.
     *
     * @param retrievedLogCount  The value of the retrieved log count.
     */
    public void setRetrievedLogCount(int retrievedLogCount) {
        this.retrievedLogCount = retrievedLogCount;
    }

    /**
     * Add the retrieved log count.
     *
     * @param count  The value of the retrieved log count to be added.
     */
    public void addRetrievedLogCount(int count) {
        retrievedLogCount += count;
    }

    /**
     * Return the endMsgNumber.
     *
     * @return long  The endMsgNumber.
     */
    public long getEndMsgNumber() {
        return endMsgNumber;
    }

    /**
     * Set the endMsgNumber.
     *
     * @param endMsgNumber The value of the endMsgNumber.
     */
    public void setEndMsgNumber(long endMsgNumber) {
        this.endMsgNumber = endMsgNumber;
    }

    /**
     * Return the startMsgNumber.
     *
     * @return long  The value of the startMsgNumber.
     */
    public long getStartMsgNumber() {
        return startMsgNumber;
    }

    /**
     * Set the startMsgNumber
     *
     * @param startMsgNumber  The value of the startMsgNumber
     */
    public void setStartMsgNumber(long startMsgNumber) {
        this.startMsgNumber = startMsgNumber;
    }

    /**
     * Get the start date (may be null)
     *
     * @return the date if set
     */
    public Date getStartMsgDate() {
        return startMsgDate;
    }

    /**
     * Get the end date (may be null)
     *
     * @return the date if set
     */
    public Date getEndMsgDate() {
        return endMsgDate;
    }

    /**
     * Set the end date (may be null)
     *
     * @param endMsgDate the date to use
     */
    public void setEndMsgDate(Date endMsgDate) {
        this.endMsgDate = endMsgDate;
    }

    public String getNodeName() {
        return nodeName;
    }

    public Level getLogLevel() {
        return logLevel;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getMessage() {
        return message;
    }

    public String getRequestId() {
        return requestId;
    }

    public AuditType getAuditType() {
        return auditType;
    }
}
