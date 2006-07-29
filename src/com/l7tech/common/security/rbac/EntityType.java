/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

import com.l7tech.identity.*;
import com.l7tech.identity.mapping.AttributeConfig;
import com.l7tech.identity.mapping.IdentityMapping;
import com.l7tech.identity.mapping.SecurityTokenMapping;
import com.l7tech.service.PublishedService;
import com.l7tech.service.SampleMessage;
import com.l7tech.service.MetricsBin;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.alert.AlertEvent;
import com.l7tech.common.alert.Notification;
import com.l7tech.common.xml.schema.SchemaEntry;
import com.l7tech.common.audit.MessageSummaryAuditRecord;
import com.l7tech.common.audit.SystemAuditRecord;
import com.l7tech.common.audit.AdminAuditRecord;
import com.l7tech.common.audit.AuditRecord;
import com.l7tech.objectmodel.Entity;
import com.l7tech.cluster.ClusterProperty;
import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.cluster.ServiceUsage;

/**
 * @author alex
 */
public enum EntityType {
    ANY("<any>", Entity.class),

    ID_PROVIDER_CONFIG("Identity Provider", IdentityProviderConfig.class),
    USER("User", PersistentUser.class),
    GROUP("Group", PersistentGroup.class),
    SERVICE("Published Service", PublishedService.class),
    JMS_CONNECTION("JMS Connection", JmsConnection.class),
    JMS_ENDPOINT("JMS Endpoint", JmsEndpoint.class),
    TRUSTED_CERT("Trusted Certificate", TrustedCert.class),
    ALERT_TRIGGER("Alert Event", AlertEvent.class),
    ALERT_ACTION("Alert Notification", Notification.class),
    SAMPLE_MESSAGE("Sample Message", SampleMessage.class),

    MAP_ATTRIBUTE("Attribute Configuration", AttributeConfig.class),
    MAP_IDENTITY("Identity Provider Attribute Mapping", IdentityMapping.class),
    MAP_TOKEN("Security Token Attribute Mapping", SecurityTokenMapping.class),

    CLUSTER_PROPERTY("Cluster Property", ClusterProperty.class),
    CLUSTER_INFO("Cluster Node Information", ClusterNodeInfo.class),
    SERVICE_USAGE("Service Usage Record", ServiceUsage.class),
    SCHEMA_ENTRY("Schema Entry", SchemaEntry.class),
    METRICS_BIN("Service Metrics Bin", MetricsBin.class),

    RBAC_ROLE("RBAC Role", Role.class),
    RBAC_PERMISSION("RBAC Permission", Permission.class),
    RBAC_PREDICATE("RBAC Scope Predicate", ScopePredicate.class),
    RBAC_ASSIGNMENT("RBAC User Assignment", UserRoleAssignment.class),

    AUDIT_RECORD("Audit Record", AuditRecord.class),
    AUDIT_MESSAGE("Message Audit Record", MessageSummaryAuditRecord.class),
    AUDIT_ADMIN("Admin Audit Record", AdminAuditRecord.class),
    AUDIT_SYSTEM("System Audit Record", SystemAuditRecord.class),
    ;

    private final String name;
    private final Class<? extends Entity> entityClass;

    public Class<? extends Entity> getEntityClass() {
        return entityClass;
    }

    public String getName() {
        return name;
    }

    private EntityType(String name, Class<? extends Entity> entityClass) {
        this.name = name;
        this.entityClass = entityClass;
    }

    public String toString() {
        return name;
    }

}
