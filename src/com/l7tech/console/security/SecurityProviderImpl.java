package com.l7tech.console.security;

import com.l7tech.common.VersionException;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.Locator;
import com.l7tech.identity.IdentityAdmin;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import javax.security.auth.login.LoginException;
import java.net.PasswordAuthentication;
import java.rmi.ConnectException;
import java.rmi.RemoteException;

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
    public synchronized void login(PasswordAuthentication creds)
      throws LoginException, VersionException, RemoteException {
        boolean authenticated = false;
        resetCredentials();
        setCredentials(creds);

        try {
            // version check
            IdentityAdmin is = (IdentityAdmin)Locator.getDefault().lookup(IdentityAdmin.class);
            if (is == null) {
                throw new ConnectException("Unable to connect to the remote service");
            }
            String remoteVersion = is.echoVersion();
            if (!SecureSpanConstants.ADMIN_PROTOCOL_VERSION.equals(remoteVersion)) {
                throw new VersionException("Version mismatch", SecureSpanConstants.ADMIN_PROTOCOL_VERSION, remoteVersion);
            }
            authenticated = true;
            LogonEvent le = new LogonEvent(this, LogonEvent.LOGON);
            applicationContext.publishEvent(le);
        } finally {
            if (!authenticated) {
                resetCredentials();
            }
        }
    }

    /**
     * Logoff the session, explicitely
     */
    public void logoff() {
        LogonEvent le = new LogonEvent(this, LogonEvent.LOGOFF);
        applicationContext.publishEvent(le);
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
