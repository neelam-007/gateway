/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;

import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public interface ServerAssertion {
    /**
     * SSG Server-side processing of the given request.
     * @param request       (In/Out) The request to check.  May be modified by processing.
     * @param response      (Out) The response to send back.  May be replaced during processing.
     * @return AssertionStatus.NONE if this Assertion did its business successfully; otherwise, some error code
     * @throws PolicyAssertionException if processing should not continue due to a serious error
     */
    AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException;
}
