/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;

/**
 * @author alex
 * @version $Revision$
 */
public interface ClientAssertion {
    /**
     * ClientProxy client-side processing of the given request.
     * @param request    The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     * @throws PolicyAssertionException if processing should not continue due to a serious error
     */
    AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException;

    /**
     * ClientProxy clinet-side processing of the given response.
     * @param request   The request that was fed to the SSG to get this response.
     * @param response  The response we received.
     * @return AssertionStatus.NONE if this Assertion was applied to the response successfully; otherwise, some error conde
     * @throws PolicyAssertionException if processing should not continue due to a serious error
     */
    AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) throws PolicyAssertionException;
}
