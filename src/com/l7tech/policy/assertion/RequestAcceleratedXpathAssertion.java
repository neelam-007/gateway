/**
 * $Id$
 */
package com.l7tech.policy.assertion;

import com.l7tech.common.xml.XpathExpression;

public class RequestAcceleratedXpathAssertion extends RequestXpathAssertion {
    public RequestAcceleratedXpathAssertion() {
        super();
    }

    public RequestAcceleratedXpathAssertion( XpathExpression xpath ) {
        super(xpath);
    }
}
