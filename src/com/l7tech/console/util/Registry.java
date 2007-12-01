package com.l7tech.console.util;

import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.common.policy.PolicyAdmin;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.security.kerberos.KerberosAdmin;
import com.l7tech.common.security.rbac.RbacAdmin;
import com.l7tech.common.transport.ftp.FtpAdmin;
import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.transport.TransportAdmin;
import com.l7tech.common.xml.schema.SchemaAdmin;
import com.l7tech.common.log.LogSinkAdmin;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.policy.PolicyPathBuilderFactory;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.service.ServiceAdmin;


/**
 * A central place that provides initial access to all components
 * and services used in the console.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
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
    public abstract IdentityAdmin getIdentityAdmin();

    /**
     * @return the {@link IdentityProviderConfig} object for the internal identity provider
     */
    abstract public IdentityProviderConfig getInternalProviderConfig();

    /**
     * @return the service managerr
     */
    abstract public ServiceAdmin getServiceManager();

    /**
     * @return the jms provider manager
     */
    abstract public JmsAdmin getJmsManager();

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

        public boolean isAdminContextPresent() {
            return false;
        }

        public IdentityAdmin getIdentityAdmin() {
            return null;
        }

        public IdentityProviderConfig getInternalProviderConfig() {
            return null;
        }

        /**
         * @return the service managerr
         */
        public ServiceAdmin getServiceManager() {
            return null;
        }

        public JmsAdmin getJmsManager() {
            return null;
        }

        public FtpAdmin getFtpManager() {
            return null;
        }

        public TrustedCertAdmin getTrustedCertManager() {
            return null;
        }

        public SchemaAdmin getSchemaAdmin() {
            return null;
        }

        /**
         * @return the custome assertions registrar
         */
        public CustomAssertionsRegistrar getCustomAssertionsRegistrar() {
            return null;
        }

        public AuditAdmin getAuditAdmin() {
            return null;
        }

        public ClusterStatusAdmin getClusterStatusAdmin() {
            return null;
        }

        public KerberosAdmin getKerberosAdmin() {
            return null;
        }

        public RbacAdmin getRbacAdmin() {
            return null;
        }

        public TransportAdmin getTransportAdmin() {
            return null;
        }

        public PolicyAdmin getPolicyAdmin() {
            return null;
        }

        public SecurityProvider getSecurityProvider() {
            return null;
        }

        public PolicyValidator getPolicyValidator() {
            return null;
        }

        public PolicyPathBuilderFactory getPolicyPathBuilderFactory() {
            return null;
        }

        public LogSinkAdmin getLogSinkAdmin() {
            return null;
        }
    }
}
