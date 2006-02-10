/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.admin.rmi;

import com.l7tech.admin.AdminContext;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.xml.schema.SchemaAdmin;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.service.ServiceAdmin;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;

import java.rmi.RemoteException;

/**
 * @author emil
 * @version Dec 2, 2004
 */
public class AdminContextImpl
  extends ApplicationObjectSupport implements AdminContext, InitializingBean {
    private IdentityAdmin identityAdmin;
    private AuditAdmin auditAdmin;
    private ServiceAdmin serviceAdmin;
    private JmsAdmin jmsAdmin;
    private TrustedCertAdmin trustedCertAdmin;
    private SchemaAdmin schemaAdmin;
    private CustomAssertionsRegistrar customAssertionsRegistrar;
    private ClusterStatusAdmin clusterStatusAdmin;

    public AdminContextImpl(IdentityAdmin identityAdmin, AuditAdmin auditAdmin, ServiceAdmin serviceAdmin,
                            JmsAdmin jmsAdmin, TrustedCertAdmin trustedCertAdmin, CustomAssertionsRegistrar customAssertionsRegistrar,
                            ClusterStatusAdmin clusterStatusAdmin, SchemaAdmin schemaAdmin) {
        this.identityAdmin = identityAdmin;
        this.auditAdmin = auditAdmin;
        this.serviceAdmin = serviceAdmin;
        this.jmsAdmin = jmsAdmin;
        this.trustedCertAdmin = trustedCertAdmin;
        this.customAssertionsRegistrar = customAssertionsRegistrar;
        this.clusterStatusAdmin = clusterStatusAdmin;
        this.schemaAdmin = schemaAdmin;
    }

    public String getVersion() {
        return SecureSpanConstants.ADMIN_PROTOCOL_VERSION;
    }

    public IdentityAdmin getIdentityAdmin() throws RemoteException, SecurityException {
        return identityAdmin;
    }

    public IdentityProviderConfig getInternalProviderConfig()
      throws RemoteException, SecurityException, FindException {
        try {
            return identityAdmin.findIdentityProviderConfigByID(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
        } finally {
        }
    }

    public ServiceAdmin getServiceAdmin() throws RemoteException, SecurityException {
        return serviceAdmin;
    }

    public JmsAdmin getJmsAdmin() throws RemoteException, SecurityException {
        return jmsAdmin;
    }

    public TrustedCertAdmin getTrustedCertAdmin() throws RemoteException, SecurityException {
        return trustedCertAdmin;
    }

    public SchemaAdmin getSchemaAdmin() throws RemoteException, SecurityException {
        return schemaAdmin;
    }

    public CustomAssertionsRegistrar getCustomAssertionsRegistrar() throws RemoteException, SecurityException {
        return customAssertionsRegistrar;
    }

    public AuditAdmin getAuditAdmin() throws RemoteException, SecurityException {
        return auditAdmin;
    }

    public ClusterStatusAdmin getClusterStatusAdmin() throws RemoteException, SecurityException {
        return clusterStatusAdmin;
    }

    public void afterPropertiesSet() throws Exception {
        checkServices();
    }

    private void checkServices() {
        if (identityAdmin == null) {
            throw new IllegalArgumentException("Identity Admin is required");
        }
        if (auditAdmin == null) {
            throw new IllegalArgumentException("Audit Admin is required");
        }

        if (customAssertionsRegistrar == null) {
            throw new IllegalArgumentException("Custom Assertions Registrar is required");
        }

        if (serviceAdmin == null) {
            throw new IllegalArgumentException("Service Admin is required");
        }

        if (jmsAdmin == null) {
            throw new IllegalArgumentException("Jms Admin is required");
        }

        if (trustedCertAdmin == null) {
            throw new IllegalArgumentException("Trusted Cert Admin is required");
        }
    }
}