/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

/**
 * Exception thrown if an element is rejected because it has the wrong number of attributes.
 */
class BadAttributeCountException extends InvalidPolicyStreamException {
    public BadAttributeCountException() {
    }

    public BadAttributeCountException(String s) {
        super(s);
    }

    public BadAttributeCountException(Throwable cause) {
        super(cause);
    }

    public BadAttributeCountException(String s, Throwable cause) {
        super(s, cause);
    }
}
