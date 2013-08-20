/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.gateway.common.audit;

import com.l7tech.gateway.common.cluster.LogRequest;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Pair;

import java.io.Serializable;
import java.util.*;
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
        maxRecords = builder.maxRecords;
        serviceName = builder.serviceName;
        message = builder.message;
        requestId = builder.requestId;
        operation = builder.operation;
        user = builder.user; // For ESM Audits Page
        userName = builder.userName;
        userIdOrDn = builder.userIdOrDn;
        messageId = builder.messageId;
        entityClassName = builder.entityClassName;
        entityId = builder.entityId;
        nodeIdToStartMsg = builder.nodeIdToStartMsg;
        nodeIdToEndMsg = builder.nodeIdToEndMsg;
        getFromPolicy = builder.getFromPolicy;
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
     * Map of node id to the minimum audit record object id to retrieve for that node.
     * If any node id key is added, then so should all known node ids, as otherwise only results for the supplied node id
     * will be returned for any search which uses this AuditSearchCriteria. If there is no
     * minimum yet for a node, just supply -1.
     */
    public final Map<String, Long> nodeIdToStartMsg;

    /**
     * Map of node id to the maximum audit record object id to retrieve for that node.
     * If any node id key is added, then so should all known node ids, as otherwise only results for the supplied node id
     * will be returned for any search which uses this AuditSearchCriteria. If there is no
     * maximum yet for a node, just supply Long.MAX_VALUE.
     */
    public final Map<String, Long> nodeIdToEndMsg;

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

    /**
     * the user that performed the operation
     */
    public final User user;

    public final String userName;
    public final String userIdOrDn;
    public final Integer messageId; // null = any
    public final String entityClassName; // Initial value: null, which means Entity Type Search Criterion is not set.
    public final Goid entityId; // null = any
    public final String operation;
    public final boolean getFromPolicy;

    /**
     * Grabs information about the search criteria.  Similar to toString() method.
     *
     * @return  Details of the search criteria in a pair object (Left = partial details, Right = full details)
     */
    public Pair<String, String> getAuditQueryDetails() {
        final StringBuilder partialDetailsOnSearchCriteria = new StringBuilder();
        final StringBuilder fullDetailsOnSearchCriteria = new StringBuilder();

        //construct time information
        Date fromTime = this.fromTime;
        Date toTime = this.toTime;
        final String fromStr = (fromTime == null || fromTime.getTime() == 0) ? "<Start of calendar>" : fromTime.toString();
        if (fromTime != null && toTime != null) {
            partialDetailsOnSearchCriteria.append("Time: " + fromStr + " to " + toTime.toString() + " ");
            fullDetailsOnSearchCriteria.append("{Time: " + fromStr + " to " + toTime.toString() + "} ");
        } else if (fromTime != null && toTime == null) {
            partialDetailsOnSearchCriteria.append("Start Time: " + fromStr + " ");
            fullDetailsOnSearchCriteria.append("{Start Time: " + fromStr + "} ");
        } else if (fromTime == null && toTime != null) {
            partialDetailsOnSearchCriteria.append("End Time: " + toTime.toString() + " ");
            fullDetailsOnSearchCriteria.append("{End Time: " + toTime.toString() + "} ");
        }

        //construct level
        Level fromLevel = this.fromLevel;
        if (fromLevel == null) fromLevel = Level.FINEST;
        Level toLevel = this.toLevel;
        if (toLevel == null) toLevel = Level.SEVERE;

        if (fromLevel.equals(toLevel)) {
            fullDetailsOnSearchCriteria.append("{Level: " + fromLevel.getName() + "} ");
        } else {
            fullDetailsOnSearchCriteria.append("{Level: " + fromLevel + " to " + toLevel + "} ");
        }

        //Note: start and end should not be used together, based on how the audit viewer works
        //it either gets newer values or older values, it does not search for ranges.
        final Set<Map.Entry<String,Long>> entries = nodeIdToStartMsg.entrySet();
        for (Map.Entry<String, Long> entry : entries) {
            fullDetailsOnSearchCriteria.append("{Node id " + entry.getKey() + " minimum audit record: " + entry.getValue() + "} ");
        }

        final Set<Map.Entry<String, Long>> endEntries = nodeIdToEndMsg.entrySet();
        for (Map.Entry<String, Long> entry : endEntries){
            fullDetailsOnSearchCriteria.append("{Node id " + entry.getKey() + " maximum audit record: " + entry.getValue() + "} ");
        }

        //construct record class (Audit Type)
        if (recordClass != null) fullDetailsOnSearchCriteria.append("{Audit Type: " + recordClass.getName() + "} ");

        //construct request ID
        if (requestId != null) fullDetailsOnSearchCriteria.append("{Request ID: " + requestId + "} ");

        //construct operation
        if (operation != null) fullDetailsOnSearchCriteria.append("{Operation: " + operation + "} ");

        //construct service name
        if (serviceName != null) fullDetailsOnSearchCriteria.append("{Service name: " + serviceName + "} ");

        //construct message
        if (message != null) fullDetailsOnSearchCriteria.append("{Message contains: " + message + "} ");

        //construct node ID
        if (nodeId != null) fullDetailsOnSearchCriteria.append("{Node ID: " + nodeId + "} ");

        // construct User Name
        if (userName != null) fullDetailsOnSearchCriteria.append("{User Name: " + userName + "} ");

        // construct User ID or DN
        if (userIdOrDn != null) fullDetailsOnSearchCriteria.append("{User ID or DN: " + userIdOrDn + "} ");

        // construct Audit Code
        if (messageId != null) fullDetailsOnSearchCriteria.append("{Audit Code: " + messageId + "} ");

        // Entity Type and Entity ID search criteria are only applied to Audit Type, ANY and Admin.
        if (recordClass == null || recordClass.equals(AdminAuditRecord.class)) {
            // construct Entity Type
            if (entityClassName != null) fullDetailsOnSearchCriteria.append("{Entity Class: " + entityClassName + "} ");

            // construct Entity ID
            if (entityId != null) fullDetailsOnSearchCriteria.append("{Entity ID: " + entityId + "} ");
        }

        // construct get from audit lookup policy
        fullDetailsOnSearchCriteria.append("{Get from Audit Lookup Policy: " + getFromPolicy + "} ");

        return new Pair<String, String>(partialDetailsOnSearchCriteria.toString(), fullDetailsOnSearchCriteria.toString());
    }

    /**
     * Compares if the criteria is similar to this instance one.
     * @param criteria
     * @return
     */
    public boolean containsSimilarCritiera(final AuditSearchCriteria criteria) {
        if ((fromLevel == null && criteria.fromLevel != null)
            || (fromLevel != null && !fromLevel.equals(criteria.fromLevel))) return false;

        if ((toLevel == null && criteria.toLevel != null)
            || (toLevel != null && !toLevel.equals(criteria.toLevel))) return false;

        if ((recordClass == null && criteria.recordClass != null)
            || (recordClass != null && !recordClass.equals(criteria.recordClass))) return false;

        if ((requestId == null && criteria.requestId != null)
            || (requestId != null && !requestId.equals(criteria.requestId))) return false;

        if ((operation == null && criteria.operation != null)
                || (operation != null && !operation.equals(criteria.operation))) return false;

        if ((serviceName == null && criteria.serviceName != null)
            || (serviceName != null && !serviceName.equals(criteria.serviceName))) return false;

        if ((message == null && criteria.message != null)
            || (message != null && !message.equals(criteria.message))) return false;

        if ((nodeId == null && criteria.nodeId != null)
            || (nodeId != null && !nodeId.equals(criteria.nodeId))) return false;

        if ((userName == null && criteria.userName != null)
            || (userName != null && !userName.equals(criteria.userName))) return false;

        if ((userIdOrDn == null && criteria.userIdOrDn != null)
            || (userIdOrDn != null && !userIdOrDn.equals(criteria.userIdOrDn))) return false;

        if ((messageId == null && criteria.messageId != null)
            || (messageId != null && !messageId.equals(criteria.messageId))) return false;

        // Entity Type and Entity ID search criteria are only applied to Audit Type, ANY and Admin.
        if (recordClass == null || recordClass.equals(AdminAuditRecord.class)) {
            if ((entityClassName == null && criteria.entityClassName != null)
                || (entityClassName != null && !entityClassName.equals(criteria.entityClassName))) return false;

            if ((entityId == null && criteria.entityId != null)
                || (entityId != null && !entityId.equals(criteria.entityId))) return false;
        }

        return true;
    }

    public static class Builder {
        private Date fromTime = null;
        private Date toTime = null;
        private Level fromLevel = null;
        private Level toLevel = null;
        private Class recordClass = null;
        private String nodeId = null;
        private final Map<String, Long> nodeIdToStartMsg;
        private final Map<String, Long> nodeIdToEndMsg;
        private int maxRecords = 0;
        private boolean getFromPolicy = false;

        private String serviceName = null; //null == any
        private String message = null; //null == any
        private String requestId = null; //null == any
        private User user = null; //null == any

        private String userName; // null == any
        private String userIdOrDn; // null == any
        private Integer messageId; // null == any
        private String entityClassName; // null == any
        private Goid entityId; // null == any
        private String operation = null; // null == any

        public Builder() {
            nodeIdToStartMsg = Collections.emptyMap();
            nodeIdToEndMsg = Collections.emptyMap();
        }

        public Builder(LogRequest logRequest) {
            fromTime(logRequest.getStartMsgDate());
            toTime(logRequest.getEndMsgDate());
            fromLevel(logRequest.getLogLevel());
            serviceName(logRequest.getServiceName());
            message(logRequest.getMessage());
            requestId(logRequest.getRequestId());
            auditType(logRequest.getAuditType());
            userName(logRequest.getUserName());
            userIdOrDn(logRequest.getUserIdOrDn());
            messageId(logRequest.getMessageId());
            entityClassName(logRequest.getEntityClassName());
            entityId(logRequest.getEntityId());
            operation(logRequest.getOperation());
            getFromPolicy(logRequest.isGetFromPolicy());
            //String and Long are immutable - can just add all to Map.
            nodeIdToStartMsg = Collections.unmodifiableMap(new HashMap<String, Long>(logRequest.getNodeIdToStartTimestamp()));
            nodeIdToEndMsg = Collections.unmodifiableMap(new HashMap<String, Long>(logRequest.getNodeIdToEndMsg()));
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

        public Builder maxRecords(int value) {
            maxRecords = value;
            return this;
        }

        public Builder serviceName(String value) {
            if (value != null && value.length() > 0) {
                serviceName = value.replace("*", "%");//translate any wildcard chars from GUI to mysql format
            } else {
                serviceName = null;
            }
            return this;
        }

        public Builder message(String value) {
            if (value != null && value.length() > 0) {
                message = value.replace("*", "%");//translate any wildcard chars from GUI to mysql format
            } else {
                message = null;
            }
            return this;
        }

        public Builder requestId(String value) {
            if (value != null && value.length() > 0) {
                requestId = value.replace("*", "%");//translate any wildcard chars from GUI to mysql format
            } else {
                requestId = null;
            }
            return this;
        }

        public Builder operation(String value) {
            if (value != null && value.length() > 0) {
                operation = value.replace("*", "%");//translate any wildcard chars from GUI to mysql format
            } else {
                operation = null;
            }
            return this;
        }


        public Builder user(User user) {
            this.user = user;
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

        public Builder userName(String value) {
            if (value != null && !value.trim().isEmpty()) {
                userName = value.replace("*", "%"); //translate any wildcard chars from GUI to mysql format
            } else {
                userName = null;
            }
            return this;
        }

        public Builder userIdOrDn(String value) {
            if (value != null && !value.trim().isEmpty()) {
                userIdOrDn = value.replace("*", "%");//translate any wildcard chars from GUI to mysql format
            } else {
                userIdOrDn = null;
            }
            return this;
        }

        public Builder messageId(Integer value) {
            messageId = value;
            return this;
        }

        public Builder entityClassName(String value) {
            entityClassName = value; // Valid Card Not Supported, since the entity class name is derived from the drop-down list in the audit search pane.
            return this;
        }

        public Builder entityId(Goid value) {
            entityId = value;
            return this;
        }
        
        public Builder getFromPolicy(boolean value){
            getFromPolicy = value;
            return this;
        }

        public AuditSearchCriteria build() {
            return new AuditSearchCriteria(this);
        }
    }
}