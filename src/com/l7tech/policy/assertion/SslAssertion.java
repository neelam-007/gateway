/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.message.*;
import com.l7tech.proxy.datamodel.PendingRequest;

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

    /**
     * ClientProxy client-side processing of the given request.
     * @param request    The request to decorate.
     * @return AssertionError.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     * @throws PolicyAssertionException if processing should not continue due to a serious error
     */
    public AssertionError decorateRequest(PendingRequest request) throws PolicyAssertionException {
        request.setSslRequired(true);
        return AssertionError.NONE;
    }

    protected Set _cipherSuites = Collections.EMPTY_SET;
}
