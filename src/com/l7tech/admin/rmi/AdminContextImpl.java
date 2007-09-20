/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.admin.rmi;

import com.l7tech.admin.AdminContext;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.common.BuildInfo;
import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.security.kerberos.KerberosAdmin;
import com.l7tech.common.security.rbac.RbacAdmin;
import com.l7tech.common.transport.ftp.FtpAdmin;
import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.transport.TransportAdmin;
import com.l7tech.common.xml.schema.SchemaAdmin;
import com.l7tech.identity.IdentityAdmin;
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
    private final IdentityAdmin identityAdmin;
    private final AuditAdmin auditAdmin;
    private final ServiceAdmin serviceAdmin;
    private final JmsAdmin jmsAdmin;
    private final FtpAdmin ftpAdmin;
    private final TrustedCertAdmin trustedCertAdmin;
    private final SchemaAdmin schemaAdmin;
    private final CustomAssertionsRegistrar customAssertionsRegistrar;
    private final ClusterStatusAdmin clusterStatusAdmin;
    private final KerberosAdmin kerberosAdmin;
    private final RbacAdmin rbacAdmin;
    private final TransportAdmin transportAdmin;

    public AdminContextImpl(IdentityAdmin identityAdmin,
                            AuditAdmin auditAdmin,
                            ServiceAdmin serviceAdmin,
                            JmsAdmin jmsAdmin,
                            FtpAdmin ftpAdmin,
                            TrustedCertAdmin trustedCertAdmin,
                            CustomAssertionsRegistrar customAssertionsRegistrar,
                            ClusterStatusAdmin clusterStatusAdmin,
                            SchemaAdmin schemaAdmin,
                            KerberosAdmin kerberosAdmin,
                            RbacAdmin rbacAdmin,
                            TransportAdmin transportAdmin) {
        this.identityAdmin = identityAdmin;
        this.auditAdmin = auditAdmin;
        this.serviceAdmin = serviceAdmin;
        this.jmsAdmin = jmsAdmin;
        this.ftpAdmin = ftpAdmin;
        this.trustedCertAdmin = trustedCertAdmin;
        this.customAssertionsRegistrar = customAssertionsRegistrar;
        this.clusterStatusAdmin = clusterStatusAdmin;
        this.schemaAdmin = schemaAdmin;
        this.kerberosAdmin = kerberosAdmin;
        this.rbacAdmin = rbacAdmin;
        this.transportAdmin = transportAdmin;
    }

    public String getVersion() {
        return SecureSpanConstants.ADMIN_PROTOCOL_VERSION;
    }

    public String getSoftwareVersion() {
        return BuildInfo.getProductVersion();
    }

    public IdentityAdmin getIdentityAdmin() throws RemoteException, SecurityException {
        return identityAdmin;
    }

    public ServiceAdmin getServiceAdmin() throws RemoteException, SecurityException {
        return serviceAdmin;
    }

    public JmsAdmin getJmsAdmin() throws RemoteException, SecurityException {
        return jmsAdmin;
    }

    public FtpAdmin getFtpAdmin() throws RemoteException, SecurityException {
        return ftpAdmin;
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

    public KerberosAdmin getKerberosAdmin() throws RemoteException, SecurityException {
        return kerberosAdmin;
    }

    public RbacAdmin getRbacAdmin() throws RemoteException, SecurityException {
        return rbacAdmin;
    }

    public TransportAdmin getTransportAdmin() throws RemoteException, SecurityException {
        return transportAdmin;
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

        if (ftpAdmin == null) {
            throw new IllegalArgumentException("FTP Admin is required");
        }

        if (trustedCertAdmin == null) {
            throw new IllegalArgumentException("Trusted Cert Admin is required");
        }

        if (kerberosAdmin == null) {
            throw new IllegalArgumentException("Kerberos Admin is required");
        }

        if (rbacAdmin == null) {
            throw new IllegalArgumentException("RBAC Admin is required");
        }

        if (transportAdmin == null) {
            throw new IllegalArgumentException("Transport Admin is required");
        }
    }
}