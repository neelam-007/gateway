package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.Assertion;

/**
 * Abstract implementation for the getAssertion
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 11, 2006<br/>
 * $Id$
 */
public abstract class AbstractServerAssertion implements ServerAssertion {
    final private Assertion assertion;
    public AbstractServerAssertion(Assertion assertion) {
        this.assertion = assertion;
    }

    public Assertion getAssertion() {
        return assertion;
    }
}
