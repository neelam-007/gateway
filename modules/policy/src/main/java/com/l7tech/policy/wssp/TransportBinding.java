/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import org.apache.ws.policy.PrimitiveAssertion;

/**
 * @author mike
 */
class TransportBinding extends SecurityBinding {
    public TransportBinding(WsspVisitor parent, PrimitiveAssertion primitiveAssertion) {
        super(parent, primitiveAssertion);
    }
}
