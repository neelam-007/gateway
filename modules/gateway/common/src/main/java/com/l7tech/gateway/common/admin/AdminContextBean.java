/*
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.admin;

import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gateway.common.log.LogSinkAdmin;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gateway.common.transport.ftp.FtpAdmin;
import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.gateway.common.schema.SchemaAdmin;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.gateway.common.service.ServiceAdmin;

import java.io.Serializable;

/**
 * Replacement for old 'remote' admin context.
 */
public class AdminContextBean implements AdminContext, Serializable {

    private static final long serialVersionUID = 3L;
    
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
    private final PolicyAdmin policyAdmin;
    private final LogSinkAdmin logSinkAdmin;
    private final String version;
    private final String softwareVersion;
    private final FolderAdmin folderAdmin;

    public AdminContextBean(IdentityAdmin identityAdmin,
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
                            TransportAdmin transportAdmin,
							PolicyAdmin policyAdmin,
                            LogSinkAdmin logSinkAdmin,
                            FolderAdmin folderAdmin,
                            String version,
                            String softwareVersion) {
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
        this.policyAdmin = policyAdmin;
        this.logSinkAdmin = logSinkAdmin;
        this.version = version;
        this.softwareVersion = softwareVersion;
        this.folderAdmin = folderAdmin;
    }

    public String getVersion() {
        return version;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public IdentityAdmin getIdentityAdmin() throws SecurityException {
        return identityAdmin;
    }

    public IdentityProviderConfig getInternalProviderConfig()
      throws SecurityException, FindException {
        return identityAdmin.findIdentityProviderConfigByID(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
    }

    public ServiceAdmin getServiceAdmin() throws SecurityException {
        return serviceAdmin;
    }

    public FolderAdmin getFolderAdmin() throws SecurityException {
        return folderAdmin;
    }

    public JmsAdmin getJmsAdmin() throws SecurityException {
        return jmsAdmin;
    }

    public FtpAdmin getFtpAdmin() throws SecurityException {
        return ftpAdmin;
    }

    public TrustedCertAdmin getTrustedCertAdmin() throws SecurityException {
        return trustedCertAdmin;
    }

    public SchemaAdmin getSchemaAdmin() throws SecurityException {
        return schemaAdmin;
    }

    public CustomAssertionsRegistrar getCustomAssertionsRegistrar() throws SecurityException {
        return customAssertionsRegistrar;
    }

    public AuditAdmin getAuditAdmin() throws SecurityException {
        return auditAdmin;
    }

    public ClusterStatusAdmin getClusterStatusAdmin() throws SecurityException {
        return clusterStatusAdmin;
    }

    public KerberosAdmin getKerberosAdmin() throws SecurityException {
        return kerberosAdmin;
    }

    public RbacAdmin getRbacAdmin() throws SecurityException {
        return rbacAdmin;
    }

    public TransportAdmin getTransportAdmin() throws SecurityException {
        return transportAdmin;
    }

    public PolicyAdmin getPolicyAdmin() throws SecurityException {
        return policyAdmin;
    }

    public LogSinkAdmin getLogSinkAdmin() throws SecurityException {
        return logSinkAdmin;
    }
}
