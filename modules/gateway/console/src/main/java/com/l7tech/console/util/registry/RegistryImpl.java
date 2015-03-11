package com.l7tech.console.util.registry;

import com.l7tech.gateway.common.cassandra.CassandraConnectionManagerAdmin;
import com.l7tech.gateway.common.siteminder.SiteMinderAdmin;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.EntityNameResolver;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.*;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gateway.common.audit.LogonEvent;
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
import com.l7tech.gateway.common.workqueue.WorkQueueManagerAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.GuidBasedEntityManager;
import com.l7tech.objectmodel.HeaderBasedEntityFinder;
import com.l7tech.policy.*;
import com.l7tech.util.Either;
import com.l7tech.util.Eithers;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.util.Option.none;
import static com.l7tech.util.Option.optional;


/**
 * A central place that provides initial access to all components
 * and services used in the console.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public final class RegistryImpl extends Registry
  implements ApplicationContextAware, ApplicationListener {
    private final Logger logger = Logger.getLogger(RegistryImpl.class.getName());

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
    private JdbcAdmin jdbcAdmin;
    private CassandraConnectionManagerAdmin cassandraConnectionAdmin;
    private SiteMinderAdmin siteMinderAdmin;
    private TrustedCertAdmin trustedCertAdmin;
    private ResourceAdmin resourceAdmin;
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
    private EncapsulatedAssertionAdmin encapsulatedAssertionAdmin;
    private PolicyBackedServiceAdmin policyBackedServiceAdmin;
    private CustomKeyValueStoreAdmin customKeyValueStoreAdmin;
    private PolicyValidator policyValidator;
    private GuidBasedEntityManager<Policy> policyFinder;
    private PolicyPathBuilderFactory policyPathBuilderFactory;
    private EntityNameResolver entityNameResolver;
    private WorkQueueManagerAdmin workQueueManagerAdmin;
    // When you add an admin interface don't forget to
    // add it to the reset method

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
            return admin.findIdentityProviderConfigByID(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID);
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
    public synchronized JdbcAdmin getJdbcConnectionAdmin() {
        checkAdminContext();
        if (jdbcAdmin != null) {
            return jdbcAdmin;
        }
        jdbcAdmin = adminContext.getJdbcConnectionAdmin();
        return jdbcAdmin;
    }

    @Override
    public synchronized CassandraConnectionManagerAdmin getCassandraConnectionAdmin() {
        checkAdminContext();
        if (cassandraConnectionAdmin != null) {
            return cassandraConnectionAdmin;
        }
        cassandraConnectionAdmin = adminContext.getCassandraConnecitonAdmin();
        return cassandraConnectionAdmin;
    }

    /**
     *
     * @return the SiteMinder Configuration manager
     */
    @Override
    public synchronized SiteMinderAdmin getSiteMinderConfigurationAdmin() {
        checkAdminContext();
        if (siteMinderAdmin != null) {
            return siteMinderAdmin;
        }
        siteMinderAdmin = adminContext.getSiteMinderConfigurationAdmin();
        return siteMinderAdmin;
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
    public synchronized ResourceAdmin getResourceAdmin() {
        checkAdminContext();
        if (resourceAdmin != null) {
            return resourceAdmin;
        }
        resourceAdmin = adminContext.getResourceAdmin();
        return resourceAdmin;
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
        return applicationContext.getBean("securityProvider", SecurityProvider.class);
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public GuidBasedEntityManager<Policy> getPolicyFinder() {
        checkAdminContext();
        if (policyFinder != null) return policyFinder;
        return policyFinder = (GuidBasedEntityManager<Policy>) applicationContext.getBean("managerPolicyCache" , GuidBasedEntityManager.class);
    }

    @NotNull
    @Override
    public HeaderBasedEntityFinder getEntityFinder() {
        checkAdminContext();
        return applicationContext.getBean("headerBasedEntityFinder", HeaderBasedEntityFinder.class);
    }

    @Override
    public PolicyValidator getPolicyValidator() {
        checkAdminContext();
        if (policyValidator != null) return policyValidator;
        return policyValidator = applicationContext.getBean("defaultPolicyValidator", PolicyValidator.class);
    }

    @Override
    public PolicyPathBuilderFactory getPolicyPathBuilderFactory() {
        checkAdminContext();
        if (policyPathBuilderFactory != null) return policyPathBuilderFactory;
        return policyPathBuilderFactory = applicationContext.getBean("policyPathBuilderFactory", PolicyPathBuilderFactory.class);
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

    @Override
    public EncapsulatedAssertionAdmin getEncapsulatedAssertionAdmin() {
        checkAdminContext();
        if (encapsulatedAssertionAdmin != null) {
            return encapsulatedAssertionAdmin;
        }
        encapsulatedAssertionAdmin = adminContext.getAdminInterface(EncapsulatedAssertionAdmin.class);
        return encapsulatedAssertionAdmin;
    }

    @Override
    public PolicyBackedServiceAdmin getPolicyBackedServiceAdmin() {
        checkAdminContext();
        if (policyBackedServiceAdmin != null) {
            return policyBackedServiceAdmin;
        }
        policyBackedServiceAdmin = adminContext.getAdminInterface(PolicyBackedServiceAdmin.class);
        return policyBackedServiceAdmin;
    }

    @Override
    public CustomKeyValueStoreAdmin getCustomKeyValueStoreAdmin() {
        checkAdminContext();
        if (customKeyValueStoreAdmin != null) {
            return customKeyValueStoreAdmin;
        }
        customKeyValueStoreAdmin = adminContext.getAdminInterface(CustomKeyValueStoreAdmin.class);
        return customKeyValueStoreAdmin;
    }

    @Override
    public synchronized WorkQueueManagerAdmin getWorkQueueManagerAdmin() {
        checkAdminContext();
        if (workQueueManagerAdmin != null) {
            return workQueueManagerAdmin;
        }
        workQueueManagerAdmin = adminContext.getWorkQueueAdmin();
        return workQueueManagerAdmin;
    }

    @Override
    public EntityNameResolver getEntityNameResolver() {
        checkAdminContext();
        if (entityNameResolver == null) {
            entityNameResolver = new EntityNameResolver(getServiceManager(),
                    getPolicyAdmin(),
                    getTrustedCertManager(),
                    getResourceAdmin(),
                    getFolderAdmin(),
                    getClusterStatusAdmin(),
                    TopComponents.getInstance().getAssertionRegistry(),
                    TopComponents.getInstance().getPaletteFolderRegistry(),
                    TopComponents.getInstance().getRootNode().getName());
        }
        return entityNameResolver;
    }

    @Override
    public <T> T getExtensionInterface(final Class<T> interfaceClass, final String instanceIdentifier) {
        //noinspection unchecked
        return (T)Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[] { interfaceClass }, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Either<Throwable,Option<Object>> result = getClusterStatusAdmin().invokeExtensionMethod(interfaceClass.getName(), instanceIdentifier, method.getName(), method.getParameterTypes(), args);
                return Eithers.extract( result ).toNull();
            }
        });
    }

    @Override
    public <T> Option<T> getAdminInterface(final Class<T> interfaceClass) {
        try {
            return optional( adminContext.getAdminInterface( interfaceClass ) );
        } catch ( final IllegalStateException e ) {
            return none();
        }
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
        jdbcAdmin = null;
        ftpAdmin = null;
        trustedCertAdmin = null;
        resourceAdmin = null;
        customAssertionsRegistrar = null;
        auditAdmin = null;
        clusterStatusAdmin = null;
        kerberosAdmin = null;
		transportAdmin = null;
        policyAdmin = null;
        folderAdmin = null;
        rbacAdmin = null;
        logSinkAdmin = null;
        uddiRegistryAdmin = null;
        encapsulatedAssertionAdmin = null;
        policyBackedServiceAdmin = null;
        customKeyValueStoreAdmin = null;
        emailListenerAdmin = null;
        emailAdmin = null;
        entityNameResolver = null;
        siteMinderAdmin = null;
        cassandraConnectionAdmin = null;
        workQueueManagerAdmin = null;
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