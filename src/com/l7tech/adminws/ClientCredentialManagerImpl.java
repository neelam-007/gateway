package com.l7tech.adminws;

import com.l7tech.jini.lookup.ServiceLookup;
import com.l7tech.util.Locator;

import javax.security.auth.login.LoginException;
import java.net.PasswordAuthentication;

/**
 * Default <code>ClientCredentialManager</code> implementaiton that validates
 * the credentials against the live SSG.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class ClientCredentialManagerImpl extends ClientCredentialManager {
    /**
     * Determines if the passed credentials will grant access to the admin web service.
     * If successful, those credentials will be cached for future admin ws calls.
     *
     * This requires the URL to be available in com.l7tech.console.util.Preferences.getPreferences().getServiceUrl();
     * IOException might be thrown otherwise.
     */
    public synchronized void login(PasswordAuthentication creds)
      throws LoginException, VersionException {
        resetCredentials();
        try {
            setCredentials(creds);
            Object serviceLookup =
                    Locator.getDefault().lookup(ServiceLookup.class);
            // version check
        } catch (Exception e) {
            LoginException le = new LoginException();
            le.initCause(e); // no constructor with nested throwable
            throw le;
        }
    }
}
