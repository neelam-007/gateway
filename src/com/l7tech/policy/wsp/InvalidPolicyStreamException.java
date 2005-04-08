/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

import java.io.IOException;

/**
 * Exception thrown if a policy document cannot be parsed into a policy tree for reasons due to the content
 * of the policy document.
 */
public class InvalidPolicyStreamException extends IOException {
    InvalidPolicyStreamException() {
    }

    InvalidPolicyStreamException(String s) {
        super(s);
    }

    public InvalidPolicyStreamException(Throwable cause) {
        super();
        initCause(cause);
    }

    InvalidPolicyStreamException(String s, Throwable cause) {
        super(s);
        initCause(cause);
    }
}
