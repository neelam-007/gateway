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
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ClientAssertion {
    /**
     * ClientProxy client-side processing of the given request.
     * @param request    The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     *
     */
    public abstract AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException, BadCredentialsException, OperationCanceledException;

    /**
     * ClientProxy clinet-side processing of the given response.
     * @param request   The request that was fed to the SSG to get this response.
     * @param response  The response we received.
     * @return AssertionStatus.NONE if this Assertion was applied to the response successfully; otherwise, some error conde
     */
    public abstract AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) throws PolicyAssertionException, BadCredentialsException, OperationCanceledException;
}
