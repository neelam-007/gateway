/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.message.*;

import java.util.Set;
import java.util.Collections;

/**
 * @author alex
 * @version $Revision$
 */
public class SslAssertion extends ConfidentialityAssertion {
    public AssertionError checkRequest(Request request, Response response) throws PolicyAssertionException {
        TransportMetadata tm = request.getTransportMetadata();
        if ( tm.getProtocol() == TransportProtocol.HTTPS )
            return AssertionError.NONE;
        else
            return AssertionError.FALSIFIED;
    }

    protected Set _cipherSuites = Collections.EMPTY_SET;
}
