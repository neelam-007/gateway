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
public abstract class AbstractServerAssertion<AT extends Assertion> implements ServerAssertion<AT> {
    protected final AT assertion;

    public AbstractServerAssertion(AT assertion) {
        this.assertion = assertion;
    }

    @Override
    public AT getAssertion() {
        return assertion;
    }

    /**
     * Override to cleanup any resources allocated in the ServerAssertion.
     * Caller is responsible for ensuring that no requests are currently using -- or will ever
     * again use -- this ServerAssertion after close() is called.
     */
    @Override
    public void close() {
    }
}
