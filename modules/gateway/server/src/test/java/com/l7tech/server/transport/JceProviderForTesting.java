package com.l7tech.server.transport;

import com.l7tech.security.prov.JceProvider;

/**
 * Test JCE provider for mocking the compatibility flag
 */
public class JceProviderForTesting extends JceProvider {
    @Override
    public String getDisplayName() {
        return "JceProviderForTesting";
    }

    public static Boolean compatibilityFlag = null;
    public Object getCompatibilityFlag( String flagName ) {
        return compatibilityFlag;
    }

}
