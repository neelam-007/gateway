/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy;

import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.Assertion;

/**
 * Exception thrown if a server policy assertion can't be created.
 */
public class ServerPolicyException extends PolicyAssertionException {
    public ServerPolicyException(Assertion ass) {
        super(ass);
    }

    public ServerPolicyException(Assertion ass, String message) {
        super(ass, message);
    }

    public ServerPolicyException(Assertion ass, Throwable cause) {
        super(ass, cause);
    }

    public ServerPolicyException(Assertion ass, String message, Throwable cause) {
        super(ass, message, cause);
    }
}
