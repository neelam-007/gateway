/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

/**
 *
 * User: mike
 * Date: Sep 8, 2003
 * Time: 1:34:31 PM
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
