package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.xmlsec.RequireWssSaml2;

/**
 * Client-side support for the SAML2 security assertion.
 */
public class ClientRequestWssSaml2 extends ClientRequestWssSaml {
    public ClientRequestWssSaml2(RequireWssSaml2 data) {
        super(data);
    }
}
