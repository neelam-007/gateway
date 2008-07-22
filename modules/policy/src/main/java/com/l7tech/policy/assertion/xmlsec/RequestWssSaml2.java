package com.l7tech.policy.assertion.xmlsec;

/**
 * The <code>RequestWssSaml</code> assertion describes the common SAML constraints
 * about subject, general SAML Assertion conditions and Statement constraints: for
 * authentication, authorization and attribute statements.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class RequestWssSaml2 extends RequestWssSaml {

    public RequestWssSaml2() {
        super();
        setVersion(Integer.valueOf(2));
    }

    public RequestWssSaml2(RequestWssSaml requestWssSaml) {
        super(requestWssSaml);
    }
}
