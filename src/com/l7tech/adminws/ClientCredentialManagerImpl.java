package com.l7tech.adminws;

import com.l7tech.jini.lookup.ServiceLookup;
import com.l7tech.common.util.Locator;
import com.l7tech.adminws.identity.IdentityService;

import javax.security.auth.login.LoginException;
import java.net.PasswordAuthentication;
import java.rmi.RemoteException;

import com.l7tech.adminws.identity.Service;
import com.l7tech.console.event.ConnectionEvent;

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
        IdentityService is = (IdentityService)Locator.getDefault().lookup(IdentityService.class);
        String remoteVersion = is.echoVersion();
        if (!Service.VERSION.equals(remoteVersion)) {
            throw new VersionException(Service.VERSION, remoteVersion);
        }
        serviceLookup =
          (ServiceLookup)Locator.getDefault().lookup(ServiceLookup.class);
    }


    /**
     * Invoked on disconnect
     * @param e describing the dosconnect event
     */
    public void onDisconnect(ConnectionEvent e) {
        logger.info("Disconnect message received, invalidating service lookup reference");
        // invalidate lookup
        serviceLookup = null;
    }
}
