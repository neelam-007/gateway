package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

/**
 * The <code>RequestWssSaml</code> assertion describes the common SAML constraints
 * about subject, general SAML Assertion conditions and Statement constraints: for
 * authentication, authorization and attribute statements.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class RequireWssSaml2 extends RequireWssSaml {

    public RequireWssSaml2() {
        super();
        setVersion(Integer.valueOf(2));
    }

    public RequireWssSaml2(RequireWssSaml requestWssSaml) {
        super(requestWssSaml);
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = (DefaultAssertionMetadata) super.meta();

        meta.put(AssertionMetadata.CLIENT_ASSERTION_CLASSNAME, "com.l7tech.proxy.policy.assertion.xmlsec.ClientRequestWssSaml2");        

        return meta;
    }
}
