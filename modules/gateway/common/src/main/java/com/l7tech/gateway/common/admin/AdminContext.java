package com.l7tech.gateway.common.admin;

import com.l7tech.gateway.common.siteminder.SiteMinderAdmin;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gateway.common.log.LogSinkAdmin;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gateway.common.transport.email.EmailListenerAdmin;
import com.l7tech.gateway.common.transport.email.EmailAdmin;
import com.l7tech.gateway.common.transport.ftp.FtpAdmin;
import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;

public interface AdminContext {

    /**
     * @return the {@link IdentityAdmin} implementation
     * @throws SecurityException on security error accessing the interface
     */
    AdminLogin getAdminLogin() throws SecurityException;

    /**
     * @return the {@link IdentityAdmin} implementation
     * @throws SecurityException on security error accessing the interface
     */
    IdentityAdmin getIdentityAdmin() throws SecurityException;

    /**
     * @return the service manager
     * @throws SecurityException on security error accessing the interface
     */
    ServiceAdmin getServiceAdmin() throws SecurityException;

    /**
     * @return the {@link FolderAdmin} implementation
     * @throws SecurityException on security error accessing the interface
     */
    FolderAdmin getFolderAdmin() throws SecurityException;
    /**
     * @return the jms provider manager
     * @throws SecurityException on security error accessing the interface
     */
    JmsAdmin getJmsAdmin() throws SecurityException;

    /**
     * @return the jdbc connection and pool managers
     * @throws SecurityException on security error accessing the interface
     */
    JdbcAdmin getJdbcConnectionAdmin() throws SecurityException;

    /**
     *
     * @return  the SiteMinder configuration manager
     * @throws SecurityException on security error accessing the interface
     */
    SiteMinderAdmin getSiteMinderConfigurationAdmin() throws SecurityException;

    /**
     * @return the FTP manager
     * @throws SecurityException on security error accessing the interface
     */
    FtpAdmin getFtpAdmin() throws SecurityException;

    /**
     * @return the trusted certificates manager
     * @throws SecurityException on security error accessing the interface
     */
    TrustedCertAdmin getTrustedCertAdmin() throws SecurityException;

    /**
     * Get the resource admininstration implementation.
     *
     * @return The resource administration implementation.
     * @throws SecurityException on security error accessing the interface
     */
    ResourceAdmin getResourceAdmin() throws SecurityException;

    /**
     * @return the custome assertions registrar
     * @throws SecurityException on security error accessing the interface
     */
    CustomAssertionsRegistrar getCustomAssertionsRegistrar() throws SecurityException;

    /**
     * @return the audit admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     */
    AuditAdmin getAuditAdmin() throws SecurityException;

    /**
     * @return the cluster status admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     */
    ClusterStatusAdmin getClusterStatusAdmin() throws SecurityException;

    /**
     * @return the kerberos admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     */
    KerberosAdmin getKerberosAdmin() throws SecurityException;

    /**
     * @return the RBAC admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     */
    RbacAdmin getRbacAdmin() throws SecurityException;

    /**
     * @return the transport admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     */
    TransportAdmin getTransportAdmin() throws SecurityException;

    /**
     * @return the email listener admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     */
    EmailListenerAdmin getEmailListenerAdmin() throws SecurityException;

    /**
     * @return the Policy admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     */
    PolicyAdmin getPolicyAdmin() throws SecurityException;

    /**
     * @return the Log sink admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     */
    LogSinkAdmin getLogSinkAdmin() throws SecurityException;

    /**
     * @return the email admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     */
    EmailAdmin getEmailAdmin() throws SecurityException;

    /**
     * @return the UDDI Registry admin interface implementation.
     * @throws SecurityException on security error accessing the interface
     */
    UDDIRegistryAdmin getUDDIRegistryAdmin() throws SecurityException;

    /**
     * Get the administrative interface of the given type.
     *
     * @param adminInterfaceClass
     * @param <AI> The interface type
     * @return The interface or null if not supported.
     * @throws SecurityException on security error accessing the interface
     */
    <AI> AI getAdminInterface( Class<AI> adminInterfaceClass ) throws SecurityException;
}