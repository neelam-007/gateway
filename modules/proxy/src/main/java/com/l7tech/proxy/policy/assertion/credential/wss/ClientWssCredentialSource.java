/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.credential.wss;

import com.l7tech.proxy.policy.assertion.ClientAssertionWithMetaSupport;
import com.l7tech.policy.assertion.Assertion;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ClientWssCredentialSource extends ClientAssertionWithMetaSupport {
    protected static final String SECURITY_NAMESPACE = "http://schemas.xmlsoap.org/ws/2002/04/secext";
    protected static final String SECURITY_NAME = "Security";

    public ClientWssCredentialSource(final Assertion assertion) {
        super(assertion);
    }
}
