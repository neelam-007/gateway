package com.l7tech.adminws;

import javax.security.auth.login.LoginException;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.Iterator;

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

    public synchronized void registerForInvalidation(CredentialsInvalidatorCallback callback) {
        if (callback != null && !credsInvalidationCallbacks.contains(callback)) credsInvalidationCallbacks.add(callback);
    }

    /**
     * Subclasses reset the credentials using this method.
     */
    protected final void resetCredentials() {
        synchronized (ClientCredentialManager.class) {
            cachedCredentials = new PasswordAuthentication("", new char[]{});
            Iterator i = credsInvalidationCallbacks.iterator();
            while (i.hasNext()) {
                CredentialsInvalidatorCallback callbackdude = (CredentialsInvalidatorCallback)i.next();
                callbackdude.invalidateCredentials();
            }
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

    protected static PasswordAuthentication cachedCredentials = new PasswordAuthentication("", new char[]{});
    protected static ArrayList credsInvalidationCallbacks = new ArrayList();
}
