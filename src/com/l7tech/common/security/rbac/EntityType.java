/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.cluster.ClusterProperty;
import com.l7tech.cluster.ServiceUsage;
import com.l7tech.common.alert.AlertEvent;
import com.l7tech.common.alert.Notification;
import com.l7tech.common.audit.AdminAuditRecord;
import com.l7tech.common.audit.AuditRecord;
import com.l7tech.common.audit.MessageSummaryAuditRecord;
import com.l7tech.common.audit.SystemAuditRecord;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.xml.schema.SchemaEntry;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.PersistentGroup;
import com.l7tech.identity.PersistentUser;
import com.l7tech.identity.mapping.AttributeConfig;
import com.l7tech.identity.mapping.IdentityMapping;
import com.l7tech.identity.mapping.SecurityTokenMapping;
import com.l7tech.objectmodel.Entity;
import static com.l7tech.objectmodel.EntityType.UNDEFINED;
import com.l7tech.service.MetricsBin;
import com.l7tech.service.PublishedService;
import com.l7tech.service.SampleMessage;

/**
 * @author alex
 */
public enum EntityType {
    ANY("<any>", Entity.class, UNDEFINED),

    ID_PROVIDER_CONFIG("Identity Provider", IdentityProviderConfig.class, com.l7tech.objectmodel.EntityType.ID_PROVIDER_CONFIG),
    USER("User", PersistentUser.class, com.l7tech.objectmodel.EntityType.USER),
    GROUP("Group", PersistentGroup.class, com.l7tech.objectmodel.EntityType.GROUP),
    SERVICE("Published Service", PublishedService.class, com.l7tech.objectmodel.EntityType.SERVICE),
    JMS_CONNECTION("JMS Connection", JmsConnection.class, com.l7tech.objectmodel.EntityType.JMS_CONNECTION),
    JMS_ENDPOINT("JMS Endpoint", JmsEndpoint.class, com.l7tech.objectmodel.EntityType.JMS_ENDPOINT),
    TRUSTED_CERT("Trusted Certificate", TrustedCert.class, com.l7tech.objectmodel.EntityType.TRUSTED_CERT),
    ALERT_TRIGGER("Alert Event", AlertEvent.class, com.l7tech.objectmodel.EntityType.ALERT_TRIGGER),
    ALERT_ACTION("Alert Notification", Notification.class, com.l7tech.objectmodel.EntityType.ALERT_ACTION),
    SAMPLE_MESSAGE("Sample Message", SampleMessage.class, com.l7tech.objectmodel.EntityType.SAMPLE_MESSAGE),

    MAP_ATTRIBUTE("Attribute Configuration", AttributeConfig.class, UNDEFINED),
    MAP_IDENTITY("Identity Provider Attribute Mapping", IdentityMapping.class, UNDEFINED),
    MAP_TOKEN("Security Token Attribute Mapping", SecurityTokenMapping.class, UNDEFINED),

    CLUSTER_PROPERTY("Cluster Property", ClusterProperty.class, UNDEFINED),
    CLUSTER_INFO("Cluster Node Information", ClusterNodeInfo.class, UNDEFINED),
    SERVICE_USAGE("Service Usage Record", ServiceUsage.class, UNDEFINED),
    SCHEMA_ENTRY("Schema Entry", SchemaEntry.class, UNDEFINED),
    METRICS_BIN("Service Metrics Bin", MetricsBin.class, UNDEFINED),

    RBAC_ROLE("RBAC Role", Role.class, com.l7tech.objectmodel.EntityType.RBAC_ROLE),

    AUDIT_RECORD("Audit Record", AuditRecord.class, UNDEFINED),
    AUDIT_MESSAGE("Message Audit Record", MessageSummaryAuditRecord.class, UNDEFINED),
    AUDIT_ADMIN("Admin Audit Record", AdminAuditRecord.class, UNDEFINED),
    AUDIT_SYSTEM("System Audit Record", SystemAuditRecord.class, UNDEFINED),
    ;

    private final String name;
    private final Class<? extends Entity> entityClass;
    private final com.l7tech.objectmodel.EntityType oldEntityType;

    public Class<? extends Entity> getEntityClass() {
        return entityClass;
    }

    public String getName() {
        return name;
    }

    private EntityType(String name, Class<? extends Entity> entityClass, com.l7tech.objectmodel.EntityType oldEntityType) {
        this.name = name;
        this.entityClass = entityClass;
        this.oldEntityType = oldEntityType;
    }

    public com.l7tech.objectmodel.EntityType getOldEntityType() {
        return oldEntityType;
    }

    public String toString() {
        return name;
    }

}
