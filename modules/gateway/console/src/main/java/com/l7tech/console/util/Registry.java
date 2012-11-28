package com.l7tech.console.util;

import com.l7tech.console.security.SecurityProvider;
import com.l7tech.gateway.common.admin.*;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gateway.common.log.LogSinkAdmin;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gateway.common.transport.email.EmailAdmin;
import com.l7tech.gateway.common.transport.email.EmailListenerAdmin;
import com.l7tech.gateway.common.transport.ftp.FtpAdmin;
import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.GuidBasedEntityManager;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyPathBuilderFactory;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static com.l7tech.util.Option.none;

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
    @NotNull
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

    /**
     * Is the admin context available? Must return true for each getXXXAdmin() method to return successfully.
     * @return true if the admin context is available
     */
    public abstract boolean isAdminContextPresent();

    /**
     * @return the {@link IdentityAdmin} implementation. Never null.
     * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
     */
    public abstract AdminLogin getAdminLogin();

    /**
     * @return the {@link IdentityAdmin} implementation. Never null.
     * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
     */
    public abstract IdentityAdmin getIdentityAdmin();

    /**
     * @return the {@link IdentityProviderConfig} object for the internal identity provider. Never null.
     * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
     */
    abstract public IdentityProviderConfig getInternalProviderConfig();

    /**
     * @return the service manager. Never null.
     * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
     */
    abstract public ServiceAdmin getServiceManager();

    /**
     * @return the folder admin. Never null.
     * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
     */
    abstract public FolderAdmin getFolderAdmin();

    /**
     * @return the jms provider manager. Never null.
     * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
     */
    abstract public JmsAdmin getJmsManager();

     /**
     * @return the JDBC Connection and Pool managers. Never null.
     * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
     */
    abstract public JdbcAdmin getJdbcConnectionAdmin();

    /**
    * @return the Ftp Admin. Never null.
    * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
    */
    abstract public FtpAdmin getFtpManager();

    /**
    * @return the trusted cert admin. Never null.
    * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
    */
    abstract public TrustedCertAdmin getTrustedCertManager();

    /**
    * @return the resource admin. Never null.
    * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
    */
    abstract public ResourceAdmin getResourceAdmin();

    /**
     * @return the custome assertions registrar. Never null.
     * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
     */
    abstract public CustomAssertionsRegistrar getCustomAssertionsRegistrar();

    /**
     * @return the audit admin interface implementation. Never null.
     * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
     */
    abstract public AuditAdmin getAuditAdmin();

    /**
     * @return the cluster status admin interface implementation.
     */
    abstract public ClusterStatusAdmin getClusterStatusAdmin();

    /**
     * @return the kerberos admin interface implementation. Never null.
     * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
     */
    abstract public KerberosAdmin getKerberosAdmin();

    /**
     * @return the RBAC admin interface implementation. Never null.
     * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
     */
    public abstract RbacAdmin getRbacAdmin();

    /**
     * @return the transport admin interface implementation. Never null.
     * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
     */
    public abstract TransportAdmin getTransportAdmin();

    /**
     * @return the email listener admin interface implementation. Never null.
     * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
     */
    public abstract EmailListenerAdmin getEmailListenerAdmin();

    /**
     * @return the email admin interface implementation. Never null.
     * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
     */
    public abstract EmailAdmin getEmailAdmin();

    /**
     * @return the Policy admin interface implementation. Never null.
     * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
     */
    public abstract PolicyAdmin getPolicyAdmin();

    /**
     * @return the security provider implementation. Never null.
     * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
     */
    abstract public SecurityProvider getSecurityProvider();

    /**
     * @return the policy validator implementation. Never null.
     * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
     */
    public abstract PolicyValidator getPolicyValidator();

    /**
     * @return the policy path builder factory. Never null.
     * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
     */
    public abstract PolicyPathBuilderFactory getPolicyPathBuilderFactory();

    /**
     * Get the PolicyFinder to use for policy lookup.
     *
     * @return The PolicyFinder. Never null.
     * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
     */
    public abstract GuidBasedEntityManager<Policy> getPolicyFinder();

    /**
     * @return the log sink admin interface implementation. Never null.
     * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
     */
    public abstract LogSinkAdmin getLogSinkAdmin();

    /**
     * @return the UDDI Registry admin interface implementation. Never null.
     * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
     */
    public abstract UDDIRegistryAdmin getUDDIRegistryAdmin();


    /**
     * @return the encapsulated assertion admin interface implementation.  Never null.
     * @throws IllegalStateException if the AdminContext is not available. See isAdminContextPresent()
     */
    public abstract EncapsulatedAssertionAdmin getEncapsulatedAssertionAdmin();

    /**
     * Get a local proxy for an admin extension interface.
     * <p/>
     * Note that currently this method will succeed even if no such interface is registered on the server.
     * In such cases, the first attempt to use the returned proxy will throw an exception.
     *
     * @param interfaceClass the interface class for which the caller desires an implementation.  Required.
     * @param instanceIdentifier instance identifier, or null if there is only ever one registered implementation of this interface.
     * @return a local proxy instance implementing the interface class.  Methods called on the local proxy will be forwarded to the server.  Never null.
     */
    public abstract <T> T getExtensionInterface(Class<T> interfaceClass, @Nullable String instanceIdentifier);

    /**
     * Get a standard administrative interface.
     *
     * <p>This is a safer to use alternative to the "getUDDIRegistryAdmin"
     * methods which throw unchecked exceptions.</p>
     *
     * @param interfaceClass The class for the desired type.
     * @param <T> The interface type
     * @return The (optional) instance, which is "none" if the interface is unavailable.
     */
    public abstract <T> Option<T> getAdminInterface(final Class<T> interfaceClass);

    public ConsoleLicenseManager getLicenseManager() {
        return ConsoleLicenseManager.getInstance();
    }

    /**
     * Implementation of the default 'no-op' registry
     *
     * WARNING: DO NOT BREAK THE INTERFACE CONTRACTS IN ANY NEW METHOD IMPLEMENTATIONS
     */
    private static final class Empty extends Registry {
        private static final String ILLEGAL_STATE_MSG = "This method should not be called when no admin context is present.";

        Empty() {
        }

        @Override
        public boolean isAdminContextPresent() {
            return false;
        }

        @Override
        public AdminLogin getAdminLogin() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public IdentityAdmin getIdentityAdmin() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public IdentityProviderConfig getInternalProviderConfig() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        /**
         * @return the service managerr
         */
        @Override
        public ServiceAdmin getServiceManager() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public FolderAdmin getFolderAdmin() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public JmsAdmin getJmsManager() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public JdbcAdmin getJdbcConnectionAdmin() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public FtpAdmin getFtpManager() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public TrustedCertAdmin getTrustedCertManager() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public ResourceAdmin getResourceAdmin() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        /**
         * @return the custome assertions registrar
         */
        @Override
        public CustomAssertionsRegistrar getCustomAssertionsRegistrar() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public AuditAdmin getAuditAdmin() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public ClusterStatusAdmin getClusterStatusAdmin() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public KerberosAdmin getKerberosAdmin() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public RbacAdmin getRbacAdmin() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public TransportAdmin getTransportAdmin() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public EmailListenerAdmin getEmailListenerAdmin() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public EmailAdmin getEmailAdmin() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public PolicyAdmin getPolicyAdmin() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public SecurityProvider getSecurityProvider() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public PolicyValidator getPolicyValidator() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public PolicyPathBuilderFactory getPolicyPathBuilderFactory() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public GuidBasedEntityManager<Policy> getPolicyFinder() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public LogSinkAdmin getLogSinkAdmin() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public UDDIRegistryAdmin getUDDIRegistryAdmin() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public EncapsulatedAssertionAdmin getEncapsulatedAssertionAdmin() {
            throw new IllegalStateException(ILLEGAL_STATE_MSG);
        }

        @Override
        public <T> T getExtensionInterface(Class<T> interfaceClass, String instanceIdentifier) {
            //noinspection unchecked
            return (T)Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[] { interfaceClass }, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    throw new IllegalStateException(ILLEGAL_STATE_MSG);
                }
            });
        }

        @Override
        public <T> Option<T> getAdminInterface( final Class<T> interfaceClass ) {
            return none();
        }
    }
}