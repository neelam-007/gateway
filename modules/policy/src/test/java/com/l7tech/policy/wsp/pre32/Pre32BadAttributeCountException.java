/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp.pre32;

/**
 * Exception thrown if an element is rejected because it has the wrong number of attributes.
 */
class Pre32BadAttributeCountException extends Pre32InvalidPolicyStreamException {
    public Pre32BadAttributeCountException() {
    }

    public Pre32BadAttributeCountException(String s) {
        super(s);
    }

    public Pre32BadAttributeCountException(Throwable cause) {
        super(cause);
    }

    public Pre32BadAttributeCountException(String s, Throwable cause) {
        super(s, cause);
    }
}
