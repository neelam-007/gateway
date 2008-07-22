/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.annotation.ProcessesRequest;

/**
 * Checks that a request was received through the HTTP transport layer.
 * @author alex
 * @version $Revision$
 */
@ProcessesRequest
public class HttpTransportAssertion extends TransportAssertion {
    public HttpTransportAssertion() {
        super();
    }
}
