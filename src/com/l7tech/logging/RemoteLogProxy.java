package com.l7tech.logging;

import com.l7tech.adminws.logging.Client;
import com.l7tech.adminws.ClientCredentialManager;
import com.l7tech.util.Locator;

import java.io.IOException;
import java.rmi.RemoteException;

/**
 * Layer 7 technologies, inc.
 * User: flascell
 * Date: Jul 3, 2003
 * Time: 2:32:17 PM
 *
 * console entry point to retrieve ssg server logs
 */
public class RemoteLogProxy {

    public String[] getSSGLogs(int offset, int size) throws RemoteException {
        return getStub().getSystemLog(offset, size);
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private Client getStub() throws RemoteException {
        if (localStub == null) {
            try {
                localStub = new Client(getServiceURL());
            }
            catch (Exception e) {
                throw new java.rmi.RemoteException("Exception getting admin ws stub", e);
            }
            if (localStub == null) throw new java.rmi.RemoteException("Exception getting admin ws stub");
        }
        return localStub;
    }
    private String getServiceURL() throws IOException {
        String prefUrl = com.l7tech.console.util.Preferences.getPreferences().getServiceUrl();
        if (prefUrl == null || prefUrl.length() < 1 || prefUrl.equals("null/ssg")) {
            System.err.println("com.l7tech.console.util.Preferences.getPreferences does not resolve a server address");
            prefUrl = "http://localhost:8080/ssg";
        }
        prefUrl += "/services/loggingAdmin";
        return prefUrl;
    }

    private Client localStub = null;
}
