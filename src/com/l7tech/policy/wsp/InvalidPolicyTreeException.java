/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

/**
 * Exception thrown if a policy tree cannot be serialized to XML for reasons related to the content
 * of the policy tree.
 */
class InvalidPolicyTreeException extends IllegalArgumentException {
    public InvalidPolicyTreeException() {
    }

    public InvalidPolicyTreeException(String s) {
        super(s);
    }

    public InvalidPolicyTreeException(Throwable cause) {
        super();
        initCause(cause);
    }

    public InvalidPolicyTreeException(String s, Throwable cause) {
        super(s);
        initCause(cause);
    }
}
