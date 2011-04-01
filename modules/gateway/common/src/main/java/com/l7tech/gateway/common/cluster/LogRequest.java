package com.l7tech.gateway.common.cluster;

import com.l7tech.gateway.common.audit.AuditType;
import com.l7tech.objectmodel.EntityType;

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
    private final String userName;
    private final String userIdOrDn;
    private final Long messageId;
    private final String paramValue;
    private final String entityTypeName;
    private final long entityId;

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
        this.userName = lr.getUserName();
        this.userIdOrDn = lr.getUserIdOrDn();
        this.messageId = lr.getMessageId();
        this.paramValue = lr.getParamValue();
        this.entityTypeName = lr.getEntityTypeName();
        this.entityId = lr.getEntityId();
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
        userName = builder.userName;
        userIdOrDn = builder.userIdOrDn;
        messageId = builder.messageId;
        paramValue = builder.paramValue;
        entityTypeName = builder.entityTypeName;
        entityId = builder.entityId;
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
        private String userName;
        private String userIdOrDn;
        private Long messageId = null;
        private String paramValue;
        private String entityTypeName = EntityType.ANY.getName();
        private long entityId = -1;

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

        public Builder userName(String value) {
            userName = value;
            return this;
        }
        public Builder userIdOrDn(String value) {
            userIdOrDn = value;
            return this;
        }

        public Builder messageId(Long value) {
            messageId = value;
            return this;
        }

        public Builder paramValue(String value) {
            paramValue = value;
            return this;
        }

        public Builder entityTypeName(String value) {
            entityTypeName = value;
            return this;
        }

        public Builder entityId(long value) {
            entityId = value;
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

    public long getEntityId() {
        return entityId;
    }

    public String getEntityTypeName() {
        return entityTypeName;
    }

    public Long getMessageId() {
        return messageId;
    }

    public String getParamValue() {
        return paramValue;
    }

    public String getUserIdOrDn() {
        return userIdOrDn;
    }

    public String getUserName() {
        return userName;
    }
}