/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp.pre32;

import java.io.IOException;

/**
 * Exception thrown if a policy document cannot be parsed into a policy tree for reasons due to the content
 * of the policy document.
 */
public class Pre32InvalidPolicyStreamException extends IOException {
    Pre32InvalidPolicyStreamException() {
    }

    Pre32InvalidPolicyStreamException(String s) {
        super(s);
    }

    public Pre32InvalidPolicyStreamException(Throwable cause) {
        super();
        initCause(cause);
    }

    Pre32InvalidPolicyStreamException(String s, Throwable cause) {
        super(s);
        initCause(cause);
    }
}
