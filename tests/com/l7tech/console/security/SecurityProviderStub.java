package com.l7tech.console.security;

import com.l7tech.common.VersionException;

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
    public void login(PasswordAuthentication creds, String host)
      throws LoginException, VersionException {
        setCredentials(creds);
    }

    /**
     * Retrieve the targewt server certificate
     *
     * @param serverCertificate
     * @see SecurityProviderImpl
     */
    public void validateServerCertificate(PasswordAuthentication credentials, X509Certificate serverCertificate, String namingURL)
      throws RemoteException, SecurityException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
