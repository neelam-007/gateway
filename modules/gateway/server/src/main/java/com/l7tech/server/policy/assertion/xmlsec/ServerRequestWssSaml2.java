package com.l7tech.server.policy.assertion.xmlsec;

import org.springframework.context.ApplicationContext;

import com.l7tech.policy.assertion.xmlsec.RequestWssSaml2;


/**
 * Class <code>ServerRequestWssSaml</code> represents the server
 * side saml Assertion that validates the SAML requestWssSaml.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class ServerRequestWssSaml2 extends ServerRequestWssSaml<RequestWssSaml2> {

    public ServerRequestWssSaml2(RequestWssSaml2 sa, ApplicationContext context) {
        super(sa, context);
    }
}
