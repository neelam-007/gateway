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
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.alert.AlertEvent;
import com.l7tech.common.alert.Notification;
import com.l7tech.common.xml.schema.SchemaEntry;
import com.l7tech.common.audit.MessageSummaryAuditRecord;
import com.l7tech.common.audit.SystemAuditRecord;
import com.l7tech.common.audit.AdminAuditRecord;
import com.l7tech.objectmodel.Entity;
import com.l7tech.cluster.ClusterProperty;

/**
 * @author alex
 */
public enum EntityType {
    ANY(Entity.class),

    ID_PROVIDER_CONFIG(IdentityProviderConfig.class),
    USER(PersistentUser.class),
    GROUP(PersistentGroup.class),
    SERVICE(PublishedService.class),
    JMS_CONNECTION(JmsConnection.class),
    JMS_ENDPOINT(JmsEndpoint.class),
    TRUSTED_CERT(TrustedCert.class),
    ALERT_TRIGGER(AlertEvent.class),
    ALERT_ACTION(Notification.class),
    SAMPLE_MESSAGE(SampleMessage.class),

    MAP_ATTRIBUTE(AttributeConfig.class),
    MAP_IDENTITY(IdentityMapping.class),
    MAP_TOKEN(SecurityTokenMapping.class),

    CLUSTER_PROPERTY(ClusterProperty.class),
    SCHEMA_ENTRY(SchemaEntry.class),

    RBAC_ROLE(Role.class),
    RBAC_PERMISSION(Permission.class),
    RBAC_PREDICATE(ScopePredicate.class),
    RBAC_ASSIGNMENT(IdentityRoleAssignment.class),

    AUDIT_MESSAGE(MessageSummaryAuditRecord.class),
    AUDIT_ADMIN(AdminAuditRecord.class),
    AUDIT_SYSTEM(SystemAuditRecord.class),
    ;

    private final Class<? extends Entity> entityClass;

    public Class<? extends Entity> getEntityClass() {
        return entityClass;
    }

    private EntityType(Class<? extends Entity> entityClass) {
        this.entityClass = entityClass;
    }
}
