/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.credential.wss;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.wss.WssDigest;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientWssDigest extends ClientWssCredentialSource {
    public ClientWssDigest( WssDigest data ) {
        this.data = data;
    }

    /**
     * ClientProxy client-side processing of the given request.
     * @param requst    The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     * @throws PolicyAssertionException if processing should not continue due to a serious error
     */
    public AssertionStatus decorateRequest(PendingRequest requst) throws PolicyAssertionException {
        return AssertionStatus.NOT_YET_IMPLEMENTED;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) throws PolicyAssertionException {
        // no action on response
        return AssertionStatus.NONE;
    }

    protected WssDigest data;
}
