package com.l7tech.adminws.identity;

import com.l7tech.identity.IdentityProviderConfig;
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
    }
    // ************************************************
    // PRIVATES
    // ************************************************
    protected Client getStub() throws java.rmi.RemoteException {
        if (localStub == null) {
            try {
                localStub = new Client(getServiceURL()/*, getAdminUsername(), getAdminPassword()*/);
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
            throw new IOException("com.l7tech.console.util.Preferences.getPreferences does not resolve a server address");
            // System.err.println("com.l7tech.console.util.Preferences.getPreferences does not resolve a server address");
            // prefUrl = "http://localhost:8080/ssg";
        }
        prefUrl += Service.SERVICE_DEPENDENT_URL_PORTION;
        return prefUrl;
    }

    protected Client localStub = null;
    protected IdentityProviderConfig config;
}
