package com.l7tech.adminws;

import com.l7tech.adminws.identity.Client;
import com.l7tech.adminws.identity.Service;

import java.io.IOException;
import java.net.PasswordAuthentication;
import java.rmi.RemoteException;
import javax.security.auth.login.LoginException;
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
    public void login(PasswordAuthentication creds)
      throws LoginException, VersionException {
        resetCredentials();
        try {
            String targetUrl = getServiceURL();
            Client testStub = new Client(targetUrl, creds.getUserName(), new String(creds.getPassword()));
            String remoteVersion = testStub.echoVersion();
            if (remoteVersion == null) {
                throw new VersionException("Unknown version");
            }
            if (!remoteVersion.equals(Service.VERSION)) {
                throw new VersionException("Version mismatch ", Service.VERSION, remoteVersion);
            }
            setCredentials(creds);

        } catch (RemoteException e) {
            LoginException le = new LoginException();
            le.initCause(e); // no constructor with nested throwable
            throw le;
        } catch (IOException e) {
            LoginException le = new LoginException("Unable to obtain the SSG service url");
            le.initCause(e); // no constructor with nested throwable
            throw le;
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

}
