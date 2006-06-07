package com.l7tech.server.tomcat;

import java.security.Provider;
import java.util.logging.Logger;

/**
 * Security provider that configures the TrustManagerFactory for Tomcat.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class ClientTrustingProvider extends Provider {

    //- PUBLIC

    /**
     * Register the provider and set up properties.
     */
    public ClientTrustingProvider() {
        super(PROV_NAME, PROV_VERS, PROV_INFO);

        // Register Layer 7 Trusted Client Algorithm
        // AXPK - Any X.509 Public Key
        put("TrustManagerFactory.AXPK", ClientTrustingTrustManagerFactorySpi.class.getName());
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ClientTrustingProvider.class.getName());

    private static final String PROV_NAME = "Layer7Provider";
    private static final double PROV_VERS = 1.0;
    private static final String PROV_INFO = "Provider for a TrustManager that accepts any X.509 Public Key.";
}
