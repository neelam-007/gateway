package com.l7tech.adminws.identity;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.adminws.ClientCredentialManager;
import com.l7tech.util.Locator;

import java.io.IOException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 24, 2003
 *
 */
public class IdentityManagerClient {

    public IdentityManagerClient(IdentityProviderConfig config) {
        this.config = config;
        credentialManager =
          (ClientCredentialManager)Locator.getDefault().lookup(ClientCredentialManager.class);
        if (credentialManager == null) { // bug
            throw new RuntimeException("No credential manager configured in services");
        }
    }
    // ************************************************
    // PRIVATES
    // ************************************************
    protected Client getStub() throws java.rmi.RemoteException {
        if (localStub == null) {
            try {
                localStub = new Client(getServiceURL(), getAdminUsername(), getAdminPassword());
            }
            catch (Exception e) {
                throw new java.rmi.RemoteException("cannot instantiate the admin service stub", e);
            }
        }
        return localStub;
    }
    protected String getServiceURL() throws IOException {
        String prefUrl = com.l7tech.console.util.Preferences.getPreferences().getServiceUrl();
        if (prefUrl == null || prefUrl.length() < 1 || prefUrl.equals("null/ssg")) {
            System.err.println("com.l7tech.console.util.Preferences.getPreferences does not resolve a server address");
            prefUrl = "http://localhost:8080/ssg";
        }
        prefUrl += "/services/identityAdmin";
        return prefUrl;
        //return "http://localhost:8080/UneasyRooster/services/identities";
    }

    protected String getAdminUsername() throws IOException {
        return credentialManager.getUsername();
    }

    protected String getAdminPassword() throws IOException {
        return credentialManager.getPassword();
    }

    private ClientCredentialManager credentialManager = null;
    protected Client localStub = null;
    protected IdentityProviderConfig config;
}
