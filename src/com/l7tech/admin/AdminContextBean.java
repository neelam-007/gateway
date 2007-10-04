package com.l7tech.admin;

import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.security.kerberos.KerberosAdmin;
import com.l7tech.common.security.rbac.RbacAdmin;
import com.l7tech.common.transport.ftp.FtpAdmin;
import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.transport.TransportAdmin;
import com.l7tech.common.xml.schema.SchemaAdmin;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.service.ServiceAdmin;

import java.io.Serializable;

/**
 * Replacement for old 'remote' admin context.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class AdminContextBean implements AdminContext, Serializable {

    private static final long serialVersionUID = 2L;
    
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
    private final String version;
    private final String softwareVersion;

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
        this.version = version;
        this.softwareVersion = softwareVersion;
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
}
