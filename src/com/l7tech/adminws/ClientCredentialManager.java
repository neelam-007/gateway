package com.l7tech.adminws;

import java.io.IOException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 5, 2003
 *
 */
public class ClientCredentialManager {

    /**
     * Determines if the passed credentials will grant access to the admin web service. If successful, those credentials
     * will be cached for future admin ws calls.
     *
     * This requires the URL to be available in com.l7tech.console.util.Preferences.getPreferences().getServiceUrl();
     * IOException might be thrown otherwise.
     */
    public static boolean validateAdminCredentials(java.net.PasswordAuthentication creds) throws IOException {
        String targetUrl = getServiceURL();
        com.l7tech.adminws.identity.Client testStub = new com.l7tech.adminws.identity.Client(targetUrl, creds.getUserName(), new String(creds.getPassword()));
        try {
            String remoteVersion = testStub.echoVersion();
            if (remoteVersion == null) return false;
            username = creds.getUserName();
            passwd = new String(creds.getPassword());
            if (!remoteVersion.equals(com.l7tech.adminws.identity.Service.VERSION)) {
                // todo, change this so that this check is propagated to console somehow
                System.err.println("ERROR in com.l7tech.adminws.ClientCredentialManager. version mismatch between client and server implementation.");
            }
        } catch (java.rmi.RemoteException e) {
            e.printStackTrace(System.err);
            return false;
        }
        return true;
    }

    public static String getCachedUsername() {
        return username;
    }

    public static String getCachedPasswd() {
        return passwd;
    }

    private static String getServiceURL() throws IOException {
        String prefUrl = com.l7tech.console.util.Preferences.getPreferences().getServiceUrl();
        if (prefUrl == null || prefUrl.length() < 1 || prefUrl.equals("null/ssg")) {
            System.err.println("com.l7tech.console.util.Preferences.getPreferences does not resolve a server address");
            prefUrl = "http://localhost:8080/ssg";
        }
        prefUrl += "/services/identityAdmin";
        return prefUrl;
    }

    private static String username;
    private static String passwd;
}
