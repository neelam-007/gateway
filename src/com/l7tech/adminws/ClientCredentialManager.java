package com.l7tech.adminws;

import com.l7tech.adminws.identity.Client;
import com.l7tech.adminws.identity.Service;

import java.io.IOException;
import java.net.PasswordAuthentication;
import java.rmi.RemoteException;
import javax.security.auth.login.LoginException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 5, 2003
 *
 */
public abstract class ClientCredentialManager {

    /**
     * Subclasses implement this method to provide the concrete login implementation.
     *
     * @param creds the credentials to authenticate
     * @see ClientCredentialManagerImpl
     */
    public abstract void login(PasswordAuthentication creds)
      throws LoginException, VersionException;


    /**
     * @return the username that was last authenticated. Emtpy string
     * otherwise, never <b>null</b>
     */
    public final String getUsername() {
        return cachedCredentials.getUserName();
    }

    /**
     * @return the password that was last authenticated. Emtpy string
     * otherwise, never <b>null</b>
     */
    public final String getPassword() {
        return String.valueOf(cachedCredentials.getPassword());
    }

    /**
     * Subclasses reset the credentials using this method.
     */
    protected final void resetCredentials() {
        synchronized (ClientCredentialManager.class) {
            cachedCredentials = new PasswordAuthentication("", new char[]{});
        }
    }

    /**
     * Subclasses update the credentials using this method.
     * @param pa the username/password instance
     */
    protected final void setCredentials(PasswordAuthentication pa) {
        synchronized (ClientCredentialManager.class) {
            cachedCredentials = pa;
        }

    }

    private String getServiceURL() throws IOException {
        String prefUrl = com.l7tech.console.util.Preferences.getPreferences().getServiceUrl();
        if (prefUrl == null || prefUrl.length() < 1 || prefUrl.equals("null/ssg")) {
            System.err.println("com.l7tech.console.util.Preferences.getPreferences does not resolve a server address");
            prefUrl = "http://localhost:8080/ssg";
        }
        prefUrl += "/services/identityAdmin";
        return prefUrl;
    }

    protected static PasswordAuthentication cachedCredentials = new PasswordAuthentication("", new char[]{});

}
