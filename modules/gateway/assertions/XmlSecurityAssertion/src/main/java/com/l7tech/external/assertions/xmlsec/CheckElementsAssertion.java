package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.assertion.TargetMessageType;

/**
 * Require elements matching an xpath to be present in a multivalued context variable that contains Element instances.
 *
 * TODO May also be able to check some other fact in a second parallel multivalued context variable, such as encryption alg
 */
public class CheckElementsAssertion extends NonSoapSecurityAssertionBase {
    public CheckElementsAssertion() {
        super(TargetMessageType.REQUEST);
    }
}
