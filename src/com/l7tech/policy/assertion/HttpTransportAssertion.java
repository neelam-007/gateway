/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.composite.CompositeAssertion;

/**
 * Checks that a request was received through the HTTP transport layer.
 * @author alex
 * @version $Revision$
 */
public class HttpTransportAssertion extends TransportAssertion {
    public HttpTransportAssertion() {
        super();
    }
}
