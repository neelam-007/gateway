/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.common.util.Locator;
import com.l7tech.console.security.ClientCredentialManager;
import com.l7tech.console.util.Preferences;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.service.ServiceAdmin;

import javax.security.auth.Subject;
import java.net.PasswordAuthentication;
import java.security.PrivilegedAction;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class SsgAdminSession {
    protected SsgAdminSession( String[] args ) throws Exception {
        if ( args.length >= 1 ) {
            System.setProperty( Preferences.SERVICE_URL, args[0] );

            if ( args.length >= 3 ) {
                _adminlogin = args[1];
                _adminpass = args[2];
            }
        }

        Preferences prefs = Preferences.getPreferences();
        prefs.updateFromProperties(System.getProperties(), false);
        /* so it is visible in help/about */
        prefs.updateSystemProperties();
        System.setProperty("com.l7tech.common.locator.properties",
                           "/com/l7tech/console/resources/services.properties");
    }

    protected void doIt() throws Exception {
        final Subject current = new Subject();

        PrivilegedAction action = new PrivilegedAction() {
            public Object run() {
                try {
                    ClientCredentialManager ccm = getCredentialManager();
                    ccm.login( new PasswordAuthentication( _adminlogin, _adminpass.toCharArray() ) );
                    return doSomething();
                } catch ( Exception e ) {
                    e.printStackTrace();
                    return null;
                }
            }
        };

        Subject.doAs( current, action );
    }

    protected abstract Object doSomething() throws Exception;

    private static ClientCredentialManager getCredentialManager() {
        ClientCredentialManager credentialManager =
                (ClientCredentialManager) Locator.getDefault().lookup(ClientCredentialManager.class);
        if (credentialManager == null) { // bug
            throw new IllegalStateException("No credential manager configured in services");
        }
        return credentialManager;
    }

    protected ServiceAdmin getServiceAdmin() {
        if ( _serviceAdmin == null ) {
            _serviceAdmin =
                    (ServiceAdmin) Locator.getDefault().lookup(ServiceAdmin.class);
            if (_serviceAdmin == null) { // bug
                throw new IllegalStateException("No ServiceAdmin configured in services");
            }
        }
        return _serviceAdmin;
    }

    protected IdentityAdmin getIdentityAdmin() {
        if ( _identityAdmin == null ) {
            _identityAdmin =
                (IdentityAdmin) Locator.getDefault().lookup(IdentityAdmin.class);
            if (_identityAdmin == null) { // bug
                throw new IllegalStateException("No ServiceAdmin configured in services");
            }
        }
        return _identityAdmin;
    }

    protected IdentityProviderConfigManager getIdentityProviderConfigManager() {
        if ( _identityProviderConfigManager == null ) {
            _identityProviderConfigManager = (IdentityProviderConfigManager)Locator.getDefault().lookup( IdentityProviderConfigManager.class );
            if ( _identityProviderConfigManager == null ) {
                throw new IllegalStateException("No IdentityProviderConfigManager configured in services");
            }
        }
        return _identityProviderConfigManager;
    }

    private String _adminlogin = "admin";
    private String _adminpass = "password";

    private ServiceAdmin _serviceAdmin;
    private IdentityAdmin _identityAdmin;
    private IdentityProviderConfigManager _identityProviderConfigManager;
}
