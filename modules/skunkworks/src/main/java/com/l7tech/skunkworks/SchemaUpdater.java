/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.skunkworks;

import com.mysql.jdbc.Driver;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;

public class SchemaUpdater {
    private final AnnotationConfiguration config;

    public SchemaUpdater() {
        config = new AnnotationConfiguration();
        config.setProperty("hibernate.dialect", MySQLDialect.class.getName());
        config.setProperty("hibernate.connection.driver_class", Driver.class.getName());
        config.setProperty("hibernate.connection.url", "jdbc:mysql://localhost/ssg");
        config.setProperty("hibernate.connection.username", "gateway");
        config.setProperty("hibernate.connection.password", "7layer");
        config.addAnnotatedClass(com.l7tech.gateway.common.transport.SsgConnector.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.transport.jms.JmsConnection.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.transport.jms.JmsEndpoint.class);

        config.addAnnotatedClass(com.l7tech.identity.IdentityProviderConfig.class);
        config.addAnnotatedClass(com.l7tech.identity.internal.InternalGroup.class);
        config.addAnnotatedClass(com.l7tech.identity.internal.InternalUser.class);
        config.addAnnotatedClass(com.l7tech.identity.internal.InternalGroupMembership.class);
        config.addAnnotatedClass(com.l7tech.identity.ldap.LdapIdentityProviderConfig.class);
        config.addAnnotatedClass(com.l7tech.identity.ldap.BindOnlyLdapIdentityProviderConfig.class);
        config.addAnnotatedClass(com.l7tech.identity.fed.FederatedIdentityProviderConfig.class);
        config.addAnnotatedClass(com.l7tech.identity.fed.FederatedUser.class);
        config.addAnnotatedClass(com.l7tech.identity.fed.FederatedGroup.class);
        config.addAnnotatedClass(com.l7tech.identity.fed.FederatedGroupMembership.class);
        config.addAnnotatedClass(com.l7tech.identity.fed.VirtualGroup.class);

        config.addAnnotatedClass(com.l7tech.server.wsdm.subscription.Subscription.class);

        config.addAnnotatedClass(com.l7tech.gateway.common.security.rbac.Role.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.security.rbac.RoleAssignment.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.security.rbac.Permission.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.security.rbac.ScopePredicate.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.security.rbac.AttributePredicate.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.security.rbac.ObjectIdentityPredicate.class);

        config.addAnnotatedClass(com.l7tech.gateway.common.audit.AuditRecord.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.audit.AdminAuditRecord.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.audit.MessageSummaryAuditRecord.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.audit.SystemAuditRecord.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.audit.AuditDetail.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.audit.MessageSummaryAuditDetail.class);

        config.addAnnotatedClass(com.l7tech.gateway.common.mapping.MessageContextMappingKeys.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.mapping.MessageContextMappingValues.class);

        config.addAnnotatedClass(com.l7tech.gateway.common.cluster.ClusterNodeInfo.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.cluster.ClusterProperty.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.log.SinkConfiguration.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.security.RevocationCheckPolicy.class);
        config.addAnnotatedClass(com.l7tech.security.cert.TrustedCert.class);
        config.addAnnotatedClass(com.l7tech.identity.cert.CertEntryRow.class);
        config.addAnnotatedClass(com.l7tech.server.security.keystore.KeystoreFile.class);
        config.addAnnotatedClass(com.l7tech.server.security.sharedkey.SharedKeyRecord.class);

        config.addAnnotatedClass(com.l7tech.server.sla.CounterRecord.class);

        config.addAnnotatedClass(com.l7tech.gateway.common.service.PublishedService.class);
        config.addAnnotatedClass(com.l7tech.policy.Policy.class);
        config.addAnnotatedClass(com.l7tech.policy.PolicyVersion.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.service.PublishedServiceAlias.class);
        config.addAnnotatedClass(com.l7tech.policy.PolicyAlias.class);
        config.addAnnotatedClass(com.l7tech.objectmodel.folder.Folder.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.service.SampleMessage.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.service.ServiceDocument.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.service.MetricsBin.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.service.MetricsBinDetail.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.cluster.ServiceUsage.class);

        config.addAnnotatedClass(com.l7tech.gateway.common.audit.AuditDetail.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.audit.AuditRecord.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.audit.AdminAuditRecord.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.audit.MessageSummaryAuditRecord.class);
        config.addAnnotatedClass(com.l7tech.gateway.common.audit.SystemAuditRecord.class);

    }

    public static void main(String[] args) {
        SchemaUpdater me = new SchemaUpdater();
        me.doIt();
    }

    private void doIt() {
        new SchemaUpdate(config).execute(true, false);
    }
}