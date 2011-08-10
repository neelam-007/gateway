package com.l7tech.proxy.datamodel;

import com.l7tech.util.ConfigFactory;

/**
 * A simple policy that the client side can enforce.  At the moment the only policy we support on the client
 * side is "require link level encryption if plaintext authentication is requested."
 *
 * User: mike
 * Date: Sep 23, 2003
 * Time: 4:34:22 PM
 */
public class ClientSidePolicy {
    public static final String PROPERTY_DISALLOWPLAINTEXT = "com.l7tech.proxy.disallowPlaintextPassword";

    private static class Defaults {
        private static boolean plaintextAuthDisallowed = ConfigFactory.getBooleanProperty( PROPERTY_DISALLOWPLAINTEXT, false );
    }

    private ClientSidePolicy() {
    }

    public static ClientSidePolicy getPolicy() {
        return new ClientSidePolicy();
    }

    public boolean isPlaintextAuthAllowed() {
        return !Defaults.plaintextAuthDisallowed;
    }
}
