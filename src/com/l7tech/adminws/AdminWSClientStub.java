package com.l7tech.adminws;

import com.l7tech.util.Locator;
import org.apache.axis.client.Call;

import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;

/**
 * User: flascell
 * Date: Jul 9, 2003
 * Time: 11:36:53 AM
 *
 * Base class for all Client stubs of the admin web services.
 */
public abstract class AdminWSClientStub {

    public AdminWSClientStub() {
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    /**
     * Each client imlpementation has it's own types to register before calls the
     * corresponding web service.
     */
    protected abstract void registerTypeMappings(Call call);

    protected abstract String getFullServiceTarget() throws IOException;

    protected Call createStubCall() throws RemoteException {
        ClientCredentialManager credentialManager = (ClientCredentialManager)Locator.getDefault().lookup(ClientCredentialManager.class);
        Call call = credentialManager.getAxisSession();
        if (call == null) {
            throw new RemoteException("Trying to initiate an admin ws call when ClientCredentialManager does not have a session ready.");
        }
        call.clearOperation();
        URL url = cachedurl;
        // check if this is new session
        if (call != typeSetOnCall) {
            registerTypeMappings(call);
            // remember that types are set on this object
            typeSetOnCall = call;
            // the base url could have changed since last login
            try {
                url = new URL(getFullServiceTarget());
            } catch (IOException e) {
                throw new RemoteException(e.getMessage(), e);
            }
            cachedurl = url;
        }
        // always set the target since this call object is shared across different service clients
        call.setTargetEndpointAddress(url);
        return call;
    }

    protected String getServiceBaseURL() throws IOException {
        String prefUrl = com.l7tech.console.util.Preferences.getPreferences().getServiceUrl();
        if (prefUrl == null || prefUrl.length() < 1 || prefUrl.equals("null/ssg")) {
            throw new IOException("com.l7tech.console.util.Preferences.getPreferences does not resolve a server address");
        }
        return prefUrl;
    }

    protected URL cachedurl = null;
    private Call typeSetOnCall = null;
}
