/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UnknownAssertion;

/**
 * @author mike
 */
public class ClientUnknownAssertion extends UnimplementedClientAssertion {
    public ClientUnknownAssertion(Assertion data) {
        super(data);
    }

    public ClientUnknownAssertion(UnknownAssertion source) {
        super(source);
    }

    private String getDesc() {
        return hasDetailMessage() ? ((UnknownAssertion)source).getDetailMessage() : source.getClass().getName();
    }

    private boolean hasDetailMessage() {
        return source != null && source instanceof UnknownAssertion && ((UnknownAssertion)source).getDetailMessage() != null;
    }

    public String getShortName() {
        return getDesc();
    }
}
