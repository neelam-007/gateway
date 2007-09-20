package com.l7tech.console.util.registry;

import com.l7tech.admin.AdminContext;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.common.audit.LogonEvent;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.security.kerberos.KerberosAdmin;
import com.l7tech.common.security.rbac.RbacAdmin;
import com.l7tech.common.transport.ftp.FtpAdmin;
import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.transport.TransportAdmin;
import com.l7tech.common.xml.schema.SchemaAdmin;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.service.ServiceAdmin;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.rmi.RemoteException;
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

    private ApplicationContext applicationContext;
    private AdminContext adminContext = null;
    private IdentityAdmin identityAdmin;
    private ServiceAdmin serviceAdmin;
    private JmsAdmin jmsAdmin;
    private FtpAdmin ftpAdmin;
    private TrustedCertAdmin trustedCertAdmin;
    private SchemaAdmin schemaAdmin;
    private CustomAssertionsRegistrar customAssertionsRegistrar;
    private AuditAdmin auditAdmin;
    private ClusterStatusAdmin clusterStatusAdmin;
    private KerberosAdmin kerberosAdmin;
    private RbacAdmin rbacAdmin;
    private TransportAdmin transportAdmin;

    public boolean isAdminContextPresent() {
        return adminContext != null;
    }

    /**
     * @return the {@link IdentityAdmin} implementation
     */
    public synchronized IdentityAdmin getIdentityAdmin() {
        checkAdminContext();
        if (identityAdmin != null) {
            return identityAdmin;
        }
        try {
            identityAdmin = adminContext.getIdentityAdmin();
            return identityAdmin;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * @return the internal identity provider
     */
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
    public synchronized ServiceAdmin getServiceManager() {
        checkAdminContext();
        if (serviceAdmin != null) {
            return serviceAdmin;
        }
        try {
            serviceAdmin = adminContext.getServiceAdmin();
            return serviceAdmin;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the JMS manager
     */
    public synchronized JmsAdmin getJmsManager() {
        checkAdminContext();
        if (jmsAdmin != null) {
            return jmsAdmin;
        }
        try {
            jmsAdmin = adminContext.getJmsAdmin();
            return jmsAdmin;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the FTP manager
     */
    public synchronized FtpAdmin getFtpManager() {
        checkAdminContext();
        if (ftpAdmin != null) {
            return ftpAdmin;
        }
        try {
            ftpAdmin = adminContext.getFtpAdmin();
            return ftpAdmin;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the Trusted Cert Manager
     */
    public synchronized TrustedCertAdmin getTrustedCertManager() {
        checkAdminContext();
        if (trustedCertAdmin != null) {
            return trustedCertAdmin;
        }
        try {
            trustedCertAdmin = adminContext.getTrustedCertAdmin();
            return trustedCertAdmin;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized SchemaAdmin getSchemaAdmin() {
        checkAdminContext();
        if (schemaAdmin != null) {
            return schemaAdmin;
        }
        try {
            schemaAdmin = adminContext.getSchemaAdmin();
            return schemaAdmin;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the custome assertions registrar
     */
    public synchronized CustomAssertionsRegistrar getCustomAssertionsRegistrar() {
        checkAdminContext();
        if (customAssertionsRegistrar != null) {
            return customAssertionsRegistrar;
        }
        try {
            customAssertionsRegistrar = adminContext.getCustomAssertionsRegistrar();
            return customAssertionsRegistrar;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the {@link AuditAdmin} implementation
     */
    public synchronized AuditAdmin getAuditAdmin() {
        checkAdminContext();
        if (auditAdmin !=null) {
            return auditAdmin;
        }
        try {
            auditAdmin = adminContext.getAuditAdmin();
            return auditAdmin;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public ClusterStatusAdmin getClusterStatusAdmin() {
        checkAdminContext();
        if (clusterStatusAdmin !=null) {
            return clusterStatusAdmin;
        }
        try {
            clusterStatusAdmin = adminContext.getClusterStatusAdmin();
            return clusterStatusAdmin;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public KerberosAdmin getKerberosAdmin() {
        checkAdminContext();
        if (kerberosAdmin != null) {
            return kerberosAdmin;
        }
        try {
            kerberosAdmin = adminContext.getKerberosAdmin();
            return kerberosAdmin;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public RbacAdmin getRbacAdmin() {
        checkAdminContext();
        if (rbacAdmin != null) {
            return rbacAdmin;
        }
        try {
            rbacAdmin = adminContext.getRbacAdmin();
            return rbacAdmin;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public TransportAdmin getTransportAdmin() {
        checkAdminContext();
        if (transportAdmin != null)
            return transportAdmin;
        try {
            transportAdmin = adminContext.getTransportAdmin();
            return transportAdmin;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public SecurityProvider getSecurityProvider() {
        return (SecurityProvider)applicationContext.getBean("securityProvider");
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
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
      * Handle an application event.
      *
      * @param event the event to respond to
      */
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
            throw new IllegalStateException("Admin Context is requred");
        }
    }

    private synchronized void resetAdminContext() {
        adminContext = null;
        identityAdmin = null;
        serviceAdmin = null;
        jmsAdmin = null;
        ftpAdmin = null;
        trustedCertAdmin = null;
        schemaAdmin = null;
        customAssertionsRegistrar = null;
        auditAdmin = null;
        clusterStatusAdmin = null;
        kerberosAdmin = null;
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
