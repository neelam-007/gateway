/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

/**
 * @author mike
 */
public class RequestWssReplayProtection extends Assertion {
    public RequestWssReplayProtection() {
    }

    public RequestWssReplayProtection(CompositeAssertion parent) {
        super(parent);
    }
}
