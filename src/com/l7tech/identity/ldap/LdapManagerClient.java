package com.l7tech.identity.ldap;

import com.l7tech.adminws.identity.Client;

import java.io.IOException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 19, 2003
 *
 */
public class LdapManagerClient {

    public LdapManagerClient(LdapIdentityProviderConfig config) {
        this.config = config;
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
        return com.l7tech.adminws.ClientCredentialManager.getCachedUsername();
    }
    protected String getAdminPassword() throws IOException {
        return com.l7tech.adminws.ClientCredentialManager.getCachedPasswd();
    }

    protected Client localStub = null;
    protected LdapIdentityProviderConfig config;
}
