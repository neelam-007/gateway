/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp.pre32;

/**
 * Exception thrown if a policy tree cannot be serialized to XML for reasons related to the content
 * of the policy tree.
 */
class Pre32InvalidPolicyTreeException extends IllegalArgumentException {
    public Pre32InvalidPolicyTreeException() {
    }

    public Pre32InvalidPolicyTreeException(String s) {
        super(s);
    }

    public Pre32InvalidPolicyTreeException(Throwable cause) {
        super();
        initCause(cause);
    }

    public Pre32InvalidPolicyTreeException(String s, Throwable cause) {
        super(s);
        initCause(cause);
    }
}
