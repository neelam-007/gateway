package com.l7tech.adminws;

import org.apache.axis.client.Call;
import com.l7tech.util.Locator;
import java.net.MalformedURLException;

/**
 * User: flascell
 * Date: Jul 9, 2003
 * Time: 11:36:53 AM
 *
 * Base class for all Client stubs of the admin web services.
 */
public abstract class AdminWSClientStub {

    public AdminWSClientStub(String targetURL) {
        this.url = targetURL;
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    /**
     * Each client imlpementation has it's own types to register before calls the
     * corresponding web service.
     */
    protected abstract void registerTypeMappings(Call call);

    protected Call createStubCall() throws java.rmi.RemoteException {
        ClientCredentialManager credentialManager = (ClientCredentialManager)Locator.getDefault().lookup(ClientCredentialManager.class);
        Call call = credentialManager.getAxisSession();
        call.clearOperation();
        try {
            call.setTargetEndpointAddress(new java.net.URL(url));
        } catch (MalformedURLException e) {
            throw new java.rmi.RemoteException(e.getMessage(), e);
        }
        if (call != typeSetOnCall) {
            registerTypeMappings(call);
            typeSetOnCall = call;
        }
        return call;
    }

    protected String url = null;
    private Call typeSetOnCall = null;
}
