/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

import java.io.IOException;

/**
 *
 * User: mike
 * Date: Sep 8, 2003
 * Time: 1:34:19 PM
 */
public class InvalidPolicyStreamException extends IOException {
    public InvalidPolicyStreamException() {
    }

    public InvalidPolicyStreamException(String s) {
        super(s);
    }

    public InvalidPolicyStreamException(Throwable cause) {
        super();
        initCause(cause);
    }

    public InvalidPolicyStreamException(String s, Throwable cause) {
        super(s);
        initCause(cause);
    }
}
