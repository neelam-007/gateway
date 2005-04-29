package com.l7tech.console.security;

import com.l7tech.admin.AdminContext;
import com.l7tech.admin.AdminLogin;
import com.l7tech.common.VersionException;
import com.l7tech.common.audit.LogonEvent;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.spring.remoting.rmi.ResettableRmiProxyFactoryBean;
import com.l7tech.identity.UserBean;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import javax.security.auth.login.LoginException;
import java.net.PasswordAuthentication;
import java.rmi.RemoteException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Default SSM <code>SecurityProvider</code> implementaiton that is a central security
 * component in SSM.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SecurityProviderImpl extends SecurityProvider
  implements ApplicationContextAware, ApplicationListener {
    private ApplicationContext applicationContext;

    /**
     * Determines if the passed credentials will grant access to the admin service.
     * If successful, those credentials will be cached for future admin ws calls.
     */
    public synchronized void login(PasswordAuthentication creds, String namingURL)
      throws LoginException, VersionException, RemoteException {
        boolean authenticated = false;
        resetCredentials();
        setCredentials(creds);

        try {
            AdminLogin adminLogin = getAdminLoginRemoteReference(namingURL);
            AdminContext context = adminLogin.login(creds.getUserName(), new String(creds.getPassword()));
            // version check
            String remoteVersion = context.getVersion();
            if (!SecureSpanConstants.ADMIN_PROTOCOL_VERSION.equals(remoteVersion)) {
                throw new VersionException("Version mismatch", SecureSpanConstants.ADMIN_PROTOCOL_VERSION, remoteVersion);
            }
            authenticated = true;
            LogonEvent le = new LogonEvent(context, LogonEvent.LOGON);
            applicationContext.publishEvent(le);
        } finally {
            if (!authenticated) {
                resetCredentials();
            }
        }
    }

    private AdminLogin getAdminLoginRemoteReference(String namingURL) {
        ResettableRmiProxyFactoryBean bean = (ResettableRmiProxyFactoryBean)applicationContext.getBean("&adminLogin");
        bean.setServiceUrl(namingURL);
        bean.resetStub();
        AdminLogin adminLogin = (AdminLogin)applicationContext.getBean("adminLogin");
        return adminLogin;
    }

    /**
     * Logoff the session, explicitely
     */
    public void logoff() {
        LogonEvent le = new LogonEvent(this, LogonEvent.LOGOFF);
        applicationContext.publishEvent(le);
    }

    /**
     * Retrieve the targewt server certificate
     *
     * @see com.l7tech.console.security.SecurityProviderImpl
     * @param serverCertificate
     * @param namingURL the naming url
     */
    public void validateServer(PasswordAuthentication credentials, X509Certificate serverCertificate, String namingURL)
      throws RemoteException, SecurityException {
        AdminLogin adminLogin = getAdminLoginRemoteReference(namingURL);
        byte[] certificate = adminLogin.getServerCertificate(credentials.getUserName());
        try {
            String password = new String(credentials.getPassword());
            String encodedPassword = UserBean.encodePasswd(credentials.getUserName(), password);
            java.security.MessageDigest d = java.security.MessageDigest.getInstance("SHA-1");
            final byte[] bytes = encodedPassword.getBytes();
            d.update(bytes);
            d.update(serverCertificate.getEncoded());
            d.update(bytes);
            byte[] digested = d.digest();
            if (!Arrays.equals(certificate, digested)) {
                throw new SecurityException("Unable to verify the server certificate at "+namingURL);
            }
        } catch (NoSuchAlgorithmException e) {
            throw (SecurityException)new SecurityException().initCause(e);
        } catch (CertificateEncodingException e) {
            throw (SecurityException)new SecurityException().initCause(e);
        }

    }

    /**
     * Set the ApplicationContext that this object runs in.
     * Normally this call will be used to initialize the object.
     * <p>Invoked after population of normal bean properties but before an init
     * callback like InitializingBean's afterPropertiesSet or a custom init-method.
     * Invoked after ResourceLoaderAware's setResourceLoader.
     *
     * @param ctx ApplicationContext object to be used by this object
     * @throws org.springframework.context.ApplicationContextException
     *          in case of applicationContext initialization errors
     * @throws org.springframework.beans.BeansException
     *          if thrown by application applicationContext methods
     * @see org.springframework.beans.factory.BeanInitializationException
     */
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        applicationContext = ctx;
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
                onLogoff(le);
            } else {
                onLogon(le);
            }
        }
    }

    private void onLogoff(LogonEvent e) {
        logger.finer("Disconnect message received, invalidating service lookup reference");
        resetCredentials();
    }


    private void onLogon(LogonEvent le) {
    }

}
