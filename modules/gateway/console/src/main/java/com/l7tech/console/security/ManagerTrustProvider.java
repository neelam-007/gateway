package com.l7tech.console.security;

import java.security.Provider;

/**
 * Security provider that configures the TrustManagerFactory for SSL.
 *
 * @author Steve Jones, $Author: steve $
 */
public class ManagerTrustProvider extends Provider {

    //- PUBLIC

    /**
     * Register the provider and set up properties.
     */
    public ManagerTrustProvider() {
        super(PROV_NAME, PROV_VERS, PROV_INFO);

        // Register Layer 7 Algorithms
        put("TrustManagerFactory.L7TA", ManagerTrustManagerFactorySpi.class.getName());
    }

    //- PRIVATE

    private static final String PROV_NAME = "Layer7Provider";
    private static final double PROV_VERS = 1.0;
    private static final String PROV_INFO = "Provider for Layer 7 Technologies SecureSpan Manager.";
}
