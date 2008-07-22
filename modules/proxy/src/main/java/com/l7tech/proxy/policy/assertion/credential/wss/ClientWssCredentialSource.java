/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.credential.wss;

import com.l7tech.proxy.policy.assertion.ClientAssertion;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ClientWssCredentialSource extends ClientAssertion {
    protected static final String SECURITY_NAMESPACE = "http://schemas.xmlsoap.org/ws/2002/04/secext";
    protected static final String SECURITY_NAME = "Security";
}
