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
    /**
     * default constructor, required by XML serializers
     */
    public SslAssertion() {}

    public AssertionStatus checkRequest(Request request, Response response) throws PolicyAssertionException {
        TransportMetadata tm = request.getTransportMetadata();
        if ( tm.getProtocol() == TransportProtocol.HTTPS )
            return AssertionStatus.NONE;
        else
            return AssertionStatus.FALSIFIED;
    }

    /**
     * ClientProxy client-side processing of the given request.
     * @param request    The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     * @throws PolicyAssertionException if processing should not continue due to a serious error
     */
    public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
        request.setSslRequired(true);
        return AssertionStatus.NONE;
    }

    protected Set _cipherSuites = Collections.EMPTY_SET;
}
