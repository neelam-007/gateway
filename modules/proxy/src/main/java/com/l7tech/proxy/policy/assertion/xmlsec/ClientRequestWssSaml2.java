package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.xmlsec.RequestWssSaml2;

/**
 * Client-side support for the SAML2 security assertion.
 */
public class ClientRequestWssSaml2 extends ClientRequestWssSaml {


    public ClientRequestWssSaml2(RequestWssSaml2 data) {
        super(data);
    }

    public String getName() {
        return isSenderVouches() ? "SAML v2 Sender-Vouches Authentication Statement" : "SAML v2 Holder-of-Key Authentication Statement";
    }
}
