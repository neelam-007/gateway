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
import com.l7tech.common.security.RevocationCheckPolicy;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.transport.SsgConnector;
import com.l7tech.common.xml.schema.SchemaEntry;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.identity.mapping.AttributeConfig;
import com.l7tech.identity.mapping.IdentityMapping;
import com.l7tech.identity.mapping.SecurityTokenMapping;
import com.l7tech.objectmodel.Entity;
import static com.l7tech.objectmodel.EntityType.UNDEFINED;
import com.l7tech.service.MetricsBin;
import com.l7tech.service.PublishedService;
import com.l7tech.service.SampleMessage;

import java.util.Comparator;

/**
 * @author alex
 */
public enum EntityType {
    ANY("<any>", Entity.class, UNDEFINED, true),

    ID_PROVIDER_CONFIG("Identity Provider", IdentityProviderConfig.class, com.l7tech.objectmodel.EntityType.ID_PROVIDER_CONFIG, true),
    USER("User", User.class, com.l7tech.objectmodel.EntityType.USER, true),
    GROUP("Group", Group.class, com.l7tech.objectmodel.EntityType.GROUP, true),
    SERVICE("Published Service", PublishedService.class, com.l7tech.objectmodel.EntityType.SERVICE, true),
    JMS_CONNECTION("JMS Connection", JmsConnection.class, com.l7tech.objectmodel.EntityType.JMS_CONNECTION, true),
    JMS_ENDPOINT("JMS Endpoint", JmsEndpoint.class, com.l7tech.objectmodel.EntityType.JMS_ENDPOINT, true),
    TRUSTED_CERT("Trusted Certificate", TrustedCert.class, com.l7tech.objectmodel.EntityType.TRUSTED_CERT, true),
    REVOCATION_CHECK_POLICY("Revocation Check Policy", RevocationCheckPolicy.class, com.l7tech.objectmodel.EntityType.REVOCATION_CHECK_POLICY, true),
    SSG_KEY_ENTRY("Private Key", Entity.class, com.l7tech.objectmodel.EntityType.PRIVATE_KEY, true), // TODO find a good home for SsgKeyEntry.class
    ALERT_TRIGGER("Alert Event", AlertEvent.class, com.l7tech.objectmodel.EntityType.ALERT_TRIGGER, false),
    ALERT_ACTION("Alert Notification", Notification.class, com.l7tech.objectmodel.EntityType.ALERT_ACTION, false),
    SAMPLE_MESSAGE("Sample Message", SampleMessage.class, com.l7tech.objectmodel.EntityType.SAMPLE_MESSAGE, true),

    MAP_ATTRIBUTE("Attribute Configuration", AttributeConfig.class, UNDEFINED, false),
    MAP_IDENTITY("Identity Provider Attribute Mapping", IdentityMapping.class, UNDEFINED, false),
    MAP_TOKEN("Security Token Attribute Mapping", SecurityTokenMapping.class, UNDEFINED, false),

    CLUSTER_PROPERTY("Cluster Property", ClusterProperty.class, UNDEFINED, true),
    CLUSTER_INFO("Cluster Node Information", ClusterNodeInfo.class, UNDEFINED, true),
    SERVICE_USAGE("Service Usage Record", ServiceUsage.class, UNDEFINED, true),
    SCHEMA_ENTRY("Schema Entry", SchemaEntry.class, UNDEFINED, true),
    METRICS_BIN("Service Metrics Bin", MetricsBin.class, UNDEFINED, true),

    RBAC_ROLE("Access Control Role", Role.class, com.l7tech.objectmodel.EntityType.RBAC_ROLE, true),

    AUDIT_RECORD("Audit Record <any type>", AuditRecord.class, UNDEFINED, true),
    AUDIT_MESSAGE("Audit Record (Message)", MessageSummaryAuditRecord.class, UNDEFINED, true),
    AUDIT_ADMIN("Audit Record (Admin)", AdminAuditRecord.class, UNDEFINED, true),
    AUDIT_SYSTEM("Audit Record (System)", SystemAuditRecord.class, UNDEFINED, true),
    SSG_CONNECTOR("Listen Port", SsgConnector.class, com.l7tech.objectmodel.EntityType.CONNECTOR, true),
    ;

    private final String name;
    private final Class<? extends Entity> entityClass;
    private final com.l7tech.objectmodel.EntityType oldEntityType;
    private final boolean displayedInGui;

    public Class<? extends Entity> getEntityClass() {
        return entityClass;
    }

    public String getName() {
        return name;
    }

    private EntityType(String name, Class<? extends Entity> entityClass, com.l7tech.objectmodel.EntityType oldEntityType, boolean displayInGui) {
        this.name = name;
        this.entityClass = entityClass;
        this.oldEntityType = oldEntityType;
        this.displayedInGui = displayInGui;
    }

    public boolean isDisplayedInGui() {
        return displayedInGui;
    }

    public com.l7tech.objectmodel.EntityType getOldEntityType() {
        return oldEntityType;
    }

    public String toString() {
        return name;
    }

    public static final NameComparator NAME_COMPARATOR = new NameComparator();

    private static class NameComparator implements Comparator<EntityType> {
        public int compare(EntityType o1, EntityType o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }
}
