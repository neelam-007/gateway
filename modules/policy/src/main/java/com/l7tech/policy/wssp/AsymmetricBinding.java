/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import org.apache.ws.policy.PrimitiveAssertion;

/**
 * @author mike
 */
class AsymmetricBinding extends SecurityBinding {
    public AsymmetricBinding(WsspVisitor parent, PrimitiveAssertion primitiveAssertion) {
        super(parent, primitiveAssertion);
    }
}
