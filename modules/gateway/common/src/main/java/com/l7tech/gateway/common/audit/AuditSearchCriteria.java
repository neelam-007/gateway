/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.gateway.common.audit;

import com.l7tech.gateway.common.cluster.LogRequest;

import java.io.Serializable;
import java.util.Date;
import java.util.logging.Level;

/**
 * A set of criteria that can be used to search for {@link AuditRecord}s using the {@link AuditAdmin} interface.
 *
 * @author mike
 */
public final class AuditSearchCriteria implements Serializable {
    /**
     * Constructs a new AuditSearchCriteria with all possible search parameters
     *
     * @param fromTime the time of the earliest record to retrieve, inclusive (null = one day ago)
     * @param toTime the time of the latest record to retrieve, inclusive (null = now)
     * @param fromLevel the level of the least severe record to retrieve, inclusive (null = {@link java.util.logging.Level#INFO})
     * @param toLevel the level of the most severe record to retrieve, inclusive (null = {@link java.util.logging.Level#SEVERE})
     * @param recordClass the type of {@link AuditRecord}s to retrieve; must be AuditRecord.class or one of its subclasses (null = AuditRecord.class)
     * @param nodeId the ID (usually a MAC address) of the cluster node to search (null = any, see {@link com.l7tech.gateway.common.cluster.ClusterStatusAdmin#getClusterStatus()})
     * @param startMessageNumber the minimum OID to find, inclusive (0 = don't care)
     * @param endMessageNumber the maximum OID to find, inclusive (0 = don't care)
     * @param maxRecords the maximum number of records to retrieve (0 = 4096);
     */

    /**
     * @param builder the builder from which to create an AuditSearchCriteria
     */
    private AuditSearchCriteria(Builder builder) {
        fromTime = builder.fromTime;
        toTime = builder.toTime;
        fromLevel = builder.fromLevel;
        toLevel = builder.toLevel;
        recordClass = builder.recordClass;
        nodeId = builder.nodeId;
        startMessageNumber = builder.startMessageNumber;
        endMessageNumber = builder.endMessageNumber;
        maxRecords = builder.maxRecords;
        serviceName = builder.serviceName;
        message = builder.message;
        requestId = builder.requestId;
    }

    /**
     * the time of the earliest record to retrieve, inclusive (null = one day ago)
     */
    public final Date fromTime;
    /**
     * the time of the latest record to retrieve, inclusive (null = now)
     */
    public final Date toTime;

    /**
     * the level of the least severe record to retrieve, inclusive (null = {@link java.util.logging.Level#INFO})
     */
    public final Level fromLevel;
    /**
     * the level of the most severe record to retrieve, inclusive (null = {@link java.util.logging.Level#SEVERE})
     */
    public final Level toLevel;

    /**
     * the type of {@link AuditRecord}s to retrieve; must be AuditRecord.class or one of its subclasses (null = AuditRecord.class)
     */
    public final Class recordClass;

    /**
     * the ID (usually a MAC address) of the cluster node to search (null = any, see {@link com.l7tech.gateway.common.cluster.ClusterStatusAdmin#getClusterStatus()})
     */
    public final String nodeId;

    /**
     * the minimum OID to find, inclusive (0 = don't care)
     */
    public final long startMessageNumber;

    /**
     * the maximum OID to find, inclusive (0 = don't care)
     */
    public final long endMessageNumber;

    /**
     * the maximum number of records to retrieve (0 = 4096)
     */
    public final int maxRecords;

    /**
     * the name of the service (null = don't care)
     */
    public final String serviceName;

    /**
     * the audit message (null = don't care)
     */
    public final String message;

    /**
     * the requestId of the message (null = don't care)
     */
    public final String requestId;

    public static class Builder {
        private Date fromTime = null;
        private Date toTime = null;
        private Level fromLevel = null;
        private Level toLevel = null;
        private Class recordClass = null;
        private String nodeId = null;
        private long startMessageNumber = 0;
        private long endMessageNumber = 0;
        private int maxRecords = 0;

        private String serviceName = null; //null == any
        private String message = null; //null == any
        private String requestId = null; //null == any

        public Builder() {
        }

        public Builder(LogRequest logRequest){
            fromTime(logRequest.getStartMsgDate());
            toTime(logRequest.getEndMsgDate());
            fromLevel(logRequest.getLogLevel());
            startMessageNumber(logRequest.getStartMsgNumber());
            endMessageNumber(logRequest.getEndMsgNumber());
            serviceName(logRequest.getServiceName());
            message(logRequest.getMessage());
            requestId(logRequest.getRequestId());
            auditType(logRequest.getAuditType());
        }

        public Builder fromTime(Date value) {
            fromTime = value;
            return this;
        }

        public Builder toTime(Date value) {
            toTime = value;
            return this;
        }

        public Builder fromLevel(Level value) {
            fromLevel = value;
            return this;
        }

        public Builder toLevel(Level value) {
            toLevel = value;
            return this;
        }

        public Builder recordClass(Class value) {
            recordClass = value;
            return this;
        }

        public Builder nodeId(String value) {
            nodeId = value;
            return this;
        }

        public Builder startMessageNumber(long value) {
            startMessageNumber = value;
            return this;
        }

        public Builder endMessageNumber(long value) {
            endMessageNumber = value;
            return this;
        }

        public Builder maxRecords(int value) {
            maxRecords = value;
            return this;
        }

        public Builder serviceName(String value) {
            if (value != null && value.length() > 0) {
                serviceName = value.replace("*", "%");//translate any wildcard chars from GUI to mysql format
            }
            return this;
        }

        public Builder message(String value) {
            if (value != null && value.length() > 0) {
                message = value.replace("*", "%");//translate any wildcard chars from GUI to mysql format
            }
            return this;
        }

        public Builder requestId(String value) {
            if (value != null && value.length() > 0) {
                requestId = value.replace("*", "%");//translate any wildcard chars from GUI to mysql format
            }
            return this;
        }

        public Builder auditType(AuditType value) {
            switch (value) {
                case ADMIN:
                    return recordClass(AdminAuditRecord.class);
                case MESSAGE:
                    return recordClass(MessageSummaryAuditRecord.class);
                case SYSTEM:
                    return recordClass(SystemAuditRecord.class);
            }
            return this;//if value is ALL
        }

        public AuditSearchCriteria build() {
            return new AuditSearchCriteria(this);
        }
    }
}
