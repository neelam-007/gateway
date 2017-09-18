package com.l7tech.console.security;

import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.InvalidPasswordException;

import javax.security.auth.login.LoginException;
import java.net.PasswordAuthentication;
import java.security.cert.X509Certificate;

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
    public void login(PasswordAuthentication creds, String host, boolean validateHost, String newPassword)
      throws LoginException, VersionException, InvalidPasswordException {
        this.user = new UserBean(creds.getUserName());
    }

    public void login(String sessionId, String host)
            throws LoginException, VersionException {
        this.user = new UserBean("stubAdmin");
    }

    /**
     * Change password implementation that throws IllegalArgumentException
     */
    public void changePassword(PasswordAuthentication auth, PasswordAuthentication newAuth) throws LoginException {
        throw new IllegalArgumentException("Password changing not supported.");
    }

    /**
     * Logoff the session, default implementation, that does nothing
     */
    public void logoff() {
    }

    public void acceptServerCertificate(X509Certificate certificate) {
    }
}
