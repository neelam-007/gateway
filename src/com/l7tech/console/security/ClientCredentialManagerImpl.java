package com.l7tech.console.security;

import com.l7tech.remote.jini.lookup.ServiceLookup;
import com.l7tech.common.util.Locator;
import com.l7tech.common.VersionException;
import com.l7tech.common.protocol.SecureSpanConstants;

import javax.security.auth.login.LoginException;
import java.net.PasswordAuthentication;
import java.rmi.RemoteException;
import java.rmi.ConnectException;

import com.l7tech.console.event.ConnectionEvent;
import com.l7tech.identity.IdentityAdmin;

/**
 * Default <code>ClientCredentialManager</code> implementaiton that validates
 * the credentials against the live SSG.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class ClientCredentialManagerImpl extends ClientCredentialManager {
    private ServiceLookup serviceLookup;

    /**
     * Determines if the passed credentials will grant access to the admin service.
     * If successful, those credentials will be cached for future admin ws calls.
     */
    public synchronized void login(PasswordAuthentication creds)
      throws LoginException, VersionException, RemoteException {
        resetCredentials();
        setCredentials(creds);
        // version check
        IdentityAdmin is = (IdentityAdmin)Locator.getDefault().lookup(IdentityAdmin.class);
        if (is == null) {
            throw new ConnectException("Unable to connect to the remote service");
        }
        String remoteVersion = is.echoVersion();
        if (!SecureSpanConstants.ADMIN_PROTOCOL_VERSION.equals(remoteVersion)) {
            throw new VersionException(SecureSpanConstants.ADMIN_PROTOCOL_VERSION, remoteVersion);
        }
        serviceLookup = (ServiceLookup)Locator.getDefault().lookup(ServiceLookup.class);
    }


    /**
     * Invoked on disconnect
     * @param e describing the dosconnect event
     */
    public void onDisconnect(ConnectionEvent e) {
        logger.finer("Disconnect message received, invalidating service lookup reference");
        resetCredentials();
        // invalidate lookup
        serviceLookup = null;
        Locator.recycle();
    }
}
