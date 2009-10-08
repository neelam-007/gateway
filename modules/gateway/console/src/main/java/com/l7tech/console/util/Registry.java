package com.l7tech.console.util;

import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.admin.KerberosAdmin;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.admin.AdminLogin;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.transport.ftp.FtpAdmin;
import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gateway.common.transport.email.EmailListenerAdmin;
import com.l7tech.gateway.common.transport.email.EmailAdmin;
import com.l7tech.gateway.common.schema.SchemaAdmin;
import com.l7tech.gateway.common.log.LogSinkAdmin;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.policy.PolicyPathBuilderFactory;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.Policy;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.jdbcconnection.JdbcConnectionAdmin;
import com.l7tech.objectmodel.GuidBasedEntityManager;


/**
 * A central place that provides initial access to all components
 * and services used in the console.
 *
 * @author Emil Marceta
 */
public abstract class Registry {
    /**
     * A dummy registry that never returns any services.
     */
    public static final Registry EMPTY = new Empty();
    /**
     * default instance
     */
    private static Registry instance;

    /**
     * Static method to obtain the global locator.
     *
     * @return the global lookup in the system
     */
    public static synchronized Registry getDefault() {
        if (instance != null) {
            return instance;
        }
        return EMPTY;
    }

    /**
     * Static method to set the global registry
     *
     * @param registry the new registry
     */
    public static synchronized void setDefault(Registry registry) {
        instance = registry;
    }
    /**
     * Empty constructor for use by subclasses.
     */
    protected Registry() {
    }

    public abstract boolean isAdminContextPresent();

    /**
     * @return the {@link IdentityAdmin} implementation
     */
    public abstract AdminLogin getAdminLogin();

    /**
     * @return the {@link IdentityAdmin} implementation
     */
    public abstract IdentityAdmin getIdentityAdmin();

    /**
     * @return the {@link IdentityProviderConfig} object for the internal identity provider
     */
    abstract public IdentityProviderConfig getInternalProviderConfig();

    /**
     * @return the service managerr
     */
    abstract public ServiceAdmin getServiceManager();

    abstract public FolderAdmin getFolderAdmin();
    /**
     * @return the jms provider manager
     */
    abstract public JmsAdmin getJmsManager();

     /**
     * @return the JDBC Connection and Pool managers
     */
    abstract public JdbcConnectionAdmin getJdbcConnectionAdmin();

    abstract public FtpAdmin getFtpManager();

    abstract public TrustedCertAdmin getTrustedCertManager();

    abstract public SchemaAdmin getSchemaAdmin();
    /**
     * @return the custome assertions registrar
     */
    abstract public CustomAssertionsRegistrar getCustomAssertionsRegistrar();

    /**
     * @return the audit admin interface implementation.
     */
    abstract public AuditAdmin getAuditAdmin();

    /**
     * @return the cluster status admin interface implementation.
     */
    abstract public ClusterStatusAdmin getClusterStatusAdmin();

    /**
     * @return the kerberos admin interface implementation.
     */
    abstract public KerberosAdmin getKerberosAdmin();

    /**
     * @return the RBAC admin interface implementation.
     */
    public abstract RbacAdmin getRbacAdmin();

    /**
     * @return the transport admin interface implementation.
     */
    public abstract TransportAdmin getTransportAdmin();

    /**
     * @return the email listener admin interface implementation.
     */
    public abstract EmailListenerAdmin getEmailListenerAdmin();

    /**
     * @return the email admin interface implementation.
     */
    public abstract EmailAdmin getEmailAdmin();

    /**
     * @return the Policy admin interface implementation.
     */
    public abstract PolicyAdmin getPolicyAdmin();

    /**
     * @return the security provider implementation.
     */
    abstract public SecurityProvider getSecurityProvider();

    public abstract PolicyValidator getPolicyValidator();

    /**
     * @return the policy path builder factory
     */
    public abstract PolicyPathBuilderFactory getPolicyPathBuilderFactory();

    /**
     * Get the PolicyFinder to use for policy lookup.
     *
     * @return The PolicyFinder.
     */
    public abstract GuidBasedEntityManager<Policy> getPolicyFinder();

    /**
     * @return the log sink admin interface implementation
     */
    public abstract LogSinkAdmin getLogSinkAdmin();

    public ConsoleLicenseManager getLicenseManager() {
        return ConsoleLicenseManager.getInstance();
    }

    /**
     * Implementation of the default 'no-op' registry
     */
    private static final class Empty extends Registry {
        Empty() {
        }

        @Override
        public boolean isAdminContextPresent() {
            return false;
        }

        @Override
        public AdminLogin getAdminLogin() {
            return null;
        }

        @Override
        public IdentityAdmin getIdentityAdmin() {
            return null;
        }

        @Override
        public IdentityProviderConfig getInternalProviderConfig() {
            return null;
        }

        /**
         * @return the service managerr
         */
        @Override
        public ServiceAdmin getServiceManager() {
            return null;
        }

        @Override
        public FolderAdmin getFolderAdmin() {
            return null;
        }

        @Override
        public JmsAdmin getJmsManager() {
            return null;
        }

        @Override
        public JdbcConnectionAdmin getJdbcConnectionAdmin() {
            return null;
        }

        @Override
        public FtpAdmin getFtpManager() {
            return null;
        }

        @Override
        public TrustedCertAdmin getTrustedCertManager() {
            return null;
        }

        @Override
        public SchemaAdmin getSchemaAdmin() {
            return null;
        }

        /**
         * @return the custome assertions registrar
         */
        @Override
        public CustomAssertionsRegistrar getCustomAssertionsRegistrar() {
            return null;
        }

        @Override
        public AuditAdmin getAuditAdmin() {
            return null;
        }

        @Override
        public ClusterStatusAdmin getClusterStatusAdmin() {
            return null;
        }

        @Override
        public KerberosAdmin getKerberosAdmin() {
            return null;
        }

        @Override
        public RbacAdmin getRbacAdmin() {
            return null;
        }

        @Override
        public TransportAdmin getTransportAdmin() {
            return null;
        }

        @Override
        public EmailListenerAdmin getEmailListenerAdmin() {
            return null;
        }

        @Override
        public EmailAdmin getEmailAdmin() {
            return null;
        }

        @Override
        public PolicyAdmin getPolicyAdmin() {
            return null;
        }

        @Override
        public SecurityProvider getSecurityProvider() {
            return null;
        }

        @Override
        public PolicyValidator getPolicyValidator() {
            return null;
        }

        @Override
        public PolicyPathBuilderFactory getPolicyPathBuilderFactory() {
            return null;
        }

        @Override
        public GuidBasedEntityManager<Policy> getPolicyFinder() {
            return null;
        }

        @Override
        public LogSinkAdmin getLogSinkAdmin() {
            return null;
        }
    }
}
