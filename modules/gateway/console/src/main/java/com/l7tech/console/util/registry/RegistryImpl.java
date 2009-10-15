package com.l7tech.console.util.registry;

import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gateway.common.log.LogSinkAdmin;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gateway.common.transport.email.EmailListenerAdmin;
import com.l7tech.gateway.common.transport.email.EmailAdmin;
import com.l7tech.gateway.common.transport.ftp.FtpAdmin;
import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.gateway.common.schema.SchemaAdmin;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.*;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.policy.PolicyPathBuilderFactory;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.Policy;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.jdbcconnection.JdbcConnectionAdmin;
import com.l7tech.objectmodel.GuidBasedEntityManager;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A central place that provides initial access to all components
 * and services used in the console.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public final class RegistryImpl extends Registry
  implements ApplicationContextAware, ApplicationListener {
    protected final Logger logger = Logger.getLogger(RegistryImpl.class.getName());

    // When you add an admin interface don't forget to
    // add it to the reset method
    private ApplicationContext applicationContext;
    private AdminContext adminContext = null;
    private AdminLogin adminLogin;
    private IdentityAdmin identityAdmin;
    private ServiceAdmin serviceAdmin;
    private FolderAdmin folderAdmin;
    private JmsAdmin jmsAdmin;
    private FtpAdmin ftpAdmin;
    private JdbcConnectionAdmin jdbcConnectionAdmin;
    private TrustedCertAdmin trustedCertAdmin;
    private SchemaAdmin schemaAdmin;
    private CustomAssertionsRegistrar customAssertionsRegistrar;
    private AuditAdmin auditAdmin;
    private ClusterStatusAdmin clusterStatusAdmin;
    private KerberosAdmin kerberosAdmin;
    private RbacAdmin rbacAdmin;
    private TransportAdmin transportAdmin;
    private EmailListenerAdmin emailListenerAdmin;
    private EmailAdmin emailAdmin;
    private PolicyAdmin policyAdmin;
    private LogSinkAdmin logSinkAdmin;
    private UDDIRegistryAdmin uddiRegistryAdmin;
    private PolicyValidator policyValidator;
    private GuidBasedEntityManager<Policy> policyFinder;
    private PolicyPathBuilderFactory policyPathBuilderFactory;

    @Override
    public boolean isAdminContextPresent() {
        return adminContext != null;
    }

    /**
     * @return the {@link IdentityAdmin} implementation
     */
    @Override
    public synchronized AdminLogin getAdminLogin() {
        checkAdminContext();
        if (adminLogin != null) {
            return adminLogin;
        }
        adminLogin = adminContext.getAdminLogin();
        return adminLogin;
    }

    /**
     * @return the {@link IdentityAdmin} implementation
     */
    @Override
    public synchronized IdentityAdmin getIdentityAdmin() {
        checkAdminContext();
        if (identityAdmin != null) {
            return identityAdmin;
        }
        identityAdmin = adminContext.getIdentityAdmin();
        return identityAdmin;
    }


    /**
     * @return the internal identity provider
     */
    @Override
    public IdentityProviderConfig getInternalProviderConfig() {
        IdentityAdmin admin = getIdentityAdmin();
        try {
            return admin.findIdentityProviderConfigByID(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the service manager
     */
    @Override
    public synchronized ServiceAdmin getServiceManager() {
        checkAdminContext();
        if (serviceAdmin != null) {
            return serviceAdmin;
        }
        serviceAdmin = adminContext.getServiceAdmin();
        return serviceAdmin;
    }

    @Override
    public FolderAdmin getFolderAdmin() {
        checkAdminContext();
        if (folderAdmin != null) {
            return folderAdmin;
        }
        folderAdmin = adminContext.getFolderAdmin();
        return folderAdmin;
    }

    /**
     * @return the JMS manager
     */
    @Override
    public synchronized JmsAdmin getJmsManager() {
        checkAdminContext();
        if (jmsAdmin != null) {
            return jmsAdmin;
        }
        jmsAdmin = adminContext.getJmsAdmin();
        return jmsAdmin;
    }

    /**
     * @return the JDBC Connection and Pool managers
     */
    @Override
    public synchronized JdbcConnectionAdmin getJdbcConnectionAdmin() {
        checkAdminContext();
        if (jdbcConnectionAdmin != null) {
            return jdbcConnectionAdmin;
        }
        jdbcConnectionAdmin = adminContext.getJdbcConnectionAdmin();
        return jdbcConnectionAdmin;
    }

    /**
     * @return the FTP manager
     */
    @Override
    public synchronized FtpAdmin getFtpManager() {
        checkAdminContext();
        if (ftpAdmin != null) {
            return ftpAdmin;
        }
        ftpAdmin = adminContext.getFtpAdmin();
        return ftpAdmin;
    }

    /**
     * @return the Trusted Cert Manager
     */
    @Override
    public synchronized TrustedCertAdmin getTrustedCertManager() {
        checkAdminContext();
        if (trustedCertAdmin != null) {
            return trustedCertAdmin;
        }
        trustedCertAdmin = adminContext.getTrustedCertAdmin();
        return trustedCertAdmin;
    }

    @Override
    public synchronized SchemaAdmin getSchemaAdmin() {
        checkAdminContext();
        if (schemaAdmin != null) {
            return schemaAdmin;
        }
        schemaAdmin = adminContext.getSchemaAdmin();
        return schemaAdmin;
    }

    /**
     * @return the custome assertions registrar
     */
    @Override
    public synchronized CustomAssertionsRegistrar getCustomAssertionsRegistrar() {
        checkAdminContext();
        if (customAssertionsRegistrar != null) {
            return customAssertionsRegistrar;
        }
        customAssertionsRegistrar = adminContext.getCustomAssertionsRegistrar();
        return customAssertionsRegistrar;
    }

    /**
     * @return the {@link AuditAdmin} implementation
     */
    @Override
    public synchronized AuditAdmin getAuditAdmin() {
        checkAdminContext();
        if (auditAdmin !=null) {
            return auditAdmin;
        }
        auditAdmin = adminContext.getAuditAdmin();
        return auditAdmin;
    }

    @Override
    public ClusterStatusAdmin getClusterStatusAdmin() {
        checkAdminContext();
        if (clusterStatusAdmin !=null) {
            return clusterStatusAdmin;
        }
        clusterStatusAdmin = adminContext.getClusterStatusAdmin();
        return clusterStatusAdmin;
    }

    @Override
    public KerberosAdmin getKerberosAdmin() {
        checkAdminContext();
        if (kerberosAdmin != null) {
            return kerberosAdmin;
        }
        kerberosAdmin = adminContext.getKerberosAdmin();
        return kerberosAdmin;
    }

    @Override
    public RbacAdmin getRbacAdmin() {
        checkAdminContext();
        if (rbacAdmin != null) {
            return rbacAdmin;
        }
        rbacAdmin = adminContext.getRbacAdmin();
        return rbacAdmin;
    }

    @Override
    public TransportAdmin getTransportAdmin() {
        checkAdminContext();
        if (transportAdmin != null)
            return transportAdmin;
        transportAdmin = adminContext.getTransportAdmin();
        return transportAdmin;
    }

    @Override
    public EmailListenerAdmin getEmailListenerAdmin() {
        checkAdminContext();
        if (emailListenerAdmin != null) {
            return emailListenerAdmin;
        }
        emailListenerAdmin = adminContext.getEmailListenerAdmin();
        return emailListenerAdmin;
    }

    @Override
    public EmailAdmin getEmailAdmin() {
        checkAdminContext();
        if (emailAdmin != null) {
            return emailAdmin;
        }
        emailAdmin = adminContext.getEmailAdmin();
        return emailAdmin;
    }

    @Override
    public PolicyAdmin getPolicyAdmin() {
        checkAdminContext();
        if (policyAdmin != null) {
            return policyAdmin;
        }
        policyAdmin = adminContext.getPolicyAdmin();
        return policyAdmin;
    }
    @Override
    public SecurityProvider getSecurityProvider() {
        return (SecurityProvider)applicationContext.getBean("securityProvider", SecurityProvider.class);
    }

    @Override
    public GuidBasedEntityManager<Policy> getPolicyFinder() {
        checkAdminContext();
        if (policyFinder != null) return policyFinder;
        return policyFinder = (GuidBasedEntityManager<Policy>) applicationContext.getBean("managerPolicyCache");
    }

    @Override
    public PolicyValidator getPolicyValidator() {
        checkAdminContext();
        if (policyValidator != null) return policyValidator;
        return policyValidator = (PolicyValidator) applicationContext.getBean("defaultPolicyValidator", PolicyValidator.class);
    }

    @Override
    public PolicyPathBuilderFactory getPolicyPathBuilderFactory() {
        checkAdminContext();
        if (policyPathBuilderFactory != null) return policyPathBuilderFactory;
        return policyPathBuilderFactory = (PolicyPathBuilderFactory) applicationContext.getBean("policyPathBuilderFactory", PolicyPathBuilderFactory.class);
    }

    @Override
    public LogSinkAdmin getLogSinkAdmin() {
        checkAdminContext();
        if (logSinkAdmin != null) {
            return logSinkAdmin;
        }
        logSinkAdmin = adminContext.getLogSinkAdmin();
        return logSinkAdmin;
    }

    @Override
    public UDDIRegistryAdmin getUDDIRegistryAdmin() {
        checkAdminContext();
        if (uddiRegistryAdmin != null) {
            return uddiRegistryAdmin;
        }
        uddiRegistryAdmin = adminContext.getUDDIRegistryAdmin();
        return uddiRegistryAdmin;
    }

    /**
     * Set the ApplicationContext that this object runs in.
     * Normally this call will be used to initialize the object.
     * <p>Invoked after population of normal bean properties but before an init
     * callback like InitializingBean's afterPropertiesSet or a custom init-method.
     * Invoked after ResourceLoaderAware's setResourceLoader.
     *
     * @param applicationContext ApplicationContext object to be used by this object
     * @throws org.springframework.context.ApplicationContextException
     *          in case of applicationContext initialization errors
     * @throws org.springframework.beans.BeansException
     *          if thrown by application applicationContext methods
     * @see org.springframework.beans.factory.BeanInitializationException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
      * Handle an application event.
      *
      * @param event the event to respond to
      */
     @Override
     public void onApplicationEvent(ApplicationEvent event) {
         if (event instanceof LogonEvent) {
             LogonEvent le = (LogonEvent)event;
             if (le.getType() == LogonEvent.LOGOFF) {
                 onLogoff();
             } else {
                 onLogon(le);
             }
         }
     }

    /**
     * check whether the admin context is set
     *
     * @throws IllegalStateException if admin context not set
     */
    private void checkAdminContext() throws IllegalStateException {
        if (adminContext == null) {
            throw new IllegalStateException("Admin Context is required");
        }
    }

    private synchronized void resetAdminContext() {
        adminContext = null;
        adminLogin = null;
        identityAdmin = null;
        serviceAdmin = null;
        jmsAdmin = null;
        jdbcConnectionAdmin = null;
        ftpAdmin = null;
        trustedCertAdmin = null;
        schemaAdmin = null;
        customAssertionsRegistrar = null;
        auditAdmin = null;
        clusterStatusAdmin = null;
        kerberosAdmin = null;
		transportAdmin = null;
        policyAdmin = null;
        folderAdmin = null;
        rbacAdmin = null;
        logSinkAdmin = null;
        emailListenerAdmin = null;
        emailAdmin = null;
    }


     private void onLogoff() {
         if(logger.isLoggable(Level.FINER)) logger.finer("Logoff message received, invalidating admin context");
         resetAdminContext();
     }


     private void onLogon(LogonEvent le) {
         Object source = le.getSource();
         if (!(source instanceof AdminContext)) {
             throw new IllegalArgumentException("Admin Context is required");
         }
         adminContext = (AdminContext)source;
     }

}
