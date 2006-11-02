package com.l7tech.console.security;

import com.l7tech.common.VersionException;
import com.l7tech.identity.UserBean;

import javax.security.auth.login.LoginException;
import java.net.PasswordAuthentication;
import java.security.cert.X509Certificate;
import java.rmi.RemoteException;

/**
 * The client credential manager stub mode. Just collect the credentials and
 * never throws an exception.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SecurityProviderStub extends SecurityProvider {
    /**
     * Stub mode login. Set the new credenitals with what was passed
     */
    public void login(PasswordAuthentication creds, String host, boolean validateHost)
      throws LoginException, VersionException {
        this.user = new UserBean(creds.getUserName());
    }

    public void login(String sessionId, String host, final X509Certificate expectedServerCert)
            throws LoginException, VersionException, RemoteException
    {
        this.user = new UserBean("stubAdmin");
    }
}
