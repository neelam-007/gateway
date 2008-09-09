/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.admin;

import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.util.BuildInfo;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gateway.common.log.LogSinkAdmin;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gateway.common.transport.ftp.FtpAdmin;
import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.gateway.common.schema.SchemaAdmin;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.admin.*;
import com.l7tech.gateway.common.service.ServiceAdmin;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;

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
    private final PolicyAdmin policyAdmin;
    private final LogSinkAdmin logSinkAdmin;
    private final FolderAdmin folderAdmin;

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
							TransportAdmin transportAdmin,
							PolicyAdmin policyAdmin,
                            LogSinkAdmin logSinkAdmin,
                            FolderAdmin folderAdmin)
    {
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
        this.folderAdmin = folderAdmin;
    }

    public String getVersion() {
        return SecureSpanConstants.ADMIN_PROTOCOL_VERSION;
    }

    public String getSoftwareVersion() {
        return BuildInfo.getProductVersion();
    }

    public IdentityAdmin getIdentityAdmin() throws SecurityException {
        return identityAdmin;
    }

    public ServiceAdmin getServiceAdmin() throws SecurityException {
        return serviceAdmin;
    }

    public FolderAdmin getFolderAdmin() throws SecurityException{
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

    public void afterPropertiesSet() throws Exception {
        check(identityAdmin, "Identity Admin is required");
        check(auditAdmin, "Audit Admin is required");
        check(customAssertionsRegistrar, "Custom Assertions Registrar is required");
        check(serviceAdmin, "Service Admin is required");
        check(folderAdmin, "Folder Admin is required");
        check(jmsAdmin, "Jms Admin is required");
        check(ftpAdmin, "FTP Admin is required");
        check(trustedCertAdmin, "Trusted Cert Admin is required");
        check(kerberosAdmin, "Kerberos Admin is required");
        check(rbacAdmin, "RBAC Admin is required");
        check(transportAdmin, "Transport Admin is required");
        check(policyAdmin, "Policy Admin is required");
        check(logSinkAdmin, "Log Sink Admin is required");
    }

    private void check(final Object what, final String why) {
        if (what == null) throw new IllegalArgumentException(why);
    }
}
