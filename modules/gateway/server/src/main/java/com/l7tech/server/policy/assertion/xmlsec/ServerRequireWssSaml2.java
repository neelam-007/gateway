package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.xmlsec.RequireWssSaml2;


/**
 * Class <code>ServerRequestWssSaml</code> represents the server
 * side saml Assertion that validates the SAML requestWssSaml.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class ServerRequireWssSaml2 extends ServerRequireWssSaml<RequireWssSaml2> {

    public ServerRequireWssSaml2(RequireWssSaml2 sa) {
        super(sa);
    }
}
