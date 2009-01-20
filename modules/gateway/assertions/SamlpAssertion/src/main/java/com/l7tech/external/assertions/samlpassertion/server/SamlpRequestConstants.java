package com.l7tech.external.assertions.samlpassertion.server;

/**
 * Constants holder interface for SAMLP request generator classes.
 *
 * @author: vchan
 */
public interface SamlpRequestConstants {

    static final String SAML_VERSION_1_1 = "1.1";
    static final String SAML_VERSION_2_0 = "2.0";
    
    static final int SAMLP_REQUEST_ID_GENERATE = 0;
    static final int SAMLP_REQUEST_ID_FROM_VAR = 1;
    static final String SAMLP_V1_REQUEST_ID_PREFIX = "samlp-";
    static final String SAMLP_V2_REQUEST_ID_PREFIX = "samlp2-";

    static final String SAMLP_V1_REQUEST_ASSN_ID_PREFIX = "samlpAssertion-";
    static final String SAMLP_V2_REQUEST_ASSN_ID_PREFIX = "samlp2Assertion-";

    static final int SAMLP_AUTHZ_EVIDENCE_AUTOMATIC = 0;
    static final int SAMLP_AUTHZ_EVIDENCE_FROM_VAR = 1;
}
