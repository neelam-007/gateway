/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.async;

import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.composite.ServerCompositeAssertion;

import java.util.Stack;

/**
 * @author alex
 * @version $Revision$
 */
public class AsyncMessageContext {
    public AsyncMessageContext() {
    }

    public synchronized void setPolicy(ServerAssertion assertion) {
        if (assertion == null) throw new IllegalArgumentException("assertion must not be null");
        if (policyStack == null)  {
            policyStack = new Stack();
            policyStack.push(assertion);
            policyPointer = null;
        } else
            throw new IllegalStateException("Policy already set");
    }

    public synchronized void executePolicy() {
        if (policyStack == null || policyStack.isEmpty()) throw new IllegalStateException("Policy not set");
        if (policyPointer == null) {

        }
        ServerAssertion ass = (ServerAssertion)policyStack.peek();
        if (ass instanceof ServerCompositeAssertion) {

        } else {
        }
    }

    private Stack policyStack;
    private ServerAssertion policyPointer;
    private RequestState state = RequestState.IDLE;
}
