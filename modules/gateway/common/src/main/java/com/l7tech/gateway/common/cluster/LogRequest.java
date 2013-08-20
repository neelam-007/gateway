package com.l7tech.gateway.common.cluster;

import com.l7tech.gateway.common.audit.AuditType;
import com.l7tech.objectmodel.Goid;

import java.util.*;
import java.util.logging.Level;

/*
 * This class encapsulates the request for retrieving logs from cluster nodes.
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
    private final Integer messageId; // null = any
    private final String paramValue;
    private final String entityClassName; // null = any
    private final Goid entityId; // null = any
    private int retrievedLogCount;
    private final boolean getFromPolicy;
    private final String operation;
    private final Map<String, Long> nodeIdToStartTimestamp = new HashMap<String, Long>();
    private final Map<String, Long> nodeIdToEndMsg = new HashMap<String, Long>();

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
        entityClassName = builder.entityClassName;
        entityId = builder.entityId;
        getFromPolicy = builder.getFromPolicy;
        operation = builder.operation;
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
        private boolean getFromPolicy;
        private Integer messageId; // null = any
        private String paramValue; //not currently supported. Will be ignored.
        private String entityClassName; // null = any
        private Goid entityId; // null = any
        private String operation = null;

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

        public Builder messageId(Integer value) {
            messageId = value;
            return this;
        }

        public Builder paramValue(String value) {
            paramValue = value;
            return this;
        }

        public Builder entityClassName(String value) {
            entityClassName = value;
            return this;
        }

        public Builder entityId(Goid value) {
            entityId = value;
            return this;
        }

        public Builder operation(String value) {
            operation = value;
            return this;
        }

        public Builder getFromPolicy(boolean value) {
            getFromPolicy = value;
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
     * Do not use for Audits.
     *
     * @return long  The endMsgNumber.
     */
    public long getEndMsgNumber() {
        return endMsgNumber;
    }

    /**
     * Record the maximum record id to retrieve for a node.
     * @param nodeId String node id
     * @param endMsgNumber object id of the audit record.
     */
    public void setEndMsgNumberForNode(String nodeId, long endMsgNumber) {
        nodeIdToEndMsg.put(nodeId, endMsgNumber);
    }

    /**
     * Get the node id to maximum audit record id mappings.
     * @return Map of node id to maximum audit record object id
     */
    public Map<String, Long> getNodeIdToEndMsg() {
        return Collections.unmodifiableMap(nodeIdToEndMsg);
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
     * Do not use for Audits.
     *
     * @param startMsgNumber  The value of the startMsgNumber
     */
    public void setStartMsgNumber(long startMsgNumber) {
        this.startMsgNumber = startMsgNumber;
    }

    /**
     * Record the minimum record id to retrieve for a node.
     * @param nodeId String node id
     * @param startMsgNumber object id of the audit record.
     */
    public void setStartTimestampForNode(String nodeId, long startMsgNumber) {
        nodeIdToStartTimestamp.put(nodeId, startMsgNumber);
    }

    /**
     * Get the node id to minimum audit record id mappings.
     * @return Map of node id to minimum audit record object id
     */
    public Map<String, Long> getNodeIdToStartTimestamp() {
        return Collections.unmodifiableMap(nodeIdToStartTimestamp);
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

    public Goid getEntityId() {
        return entityId;
    }

    public String getEntityClassName() {
        return entityClassName;
    }

    public Integer getMessageId() {
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
    
    public boolean isGetFromPolicy() {
        return getFromPolicy;
    }

    public String getOperation() {
        return operation;
    }

}