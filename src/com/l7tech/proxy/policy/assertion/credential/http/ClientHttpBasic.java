/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.credential.http;

import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.logging.LogManager;

import java.util.logging.Logger;

import org.apache.log4j.Category;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientHttpBasic implements ClientAssertion {
    private static final Category log = Category.getInstance(ClientHttpBasic.class);

    public ClientHttpBasic( HttpBasic data ) {
        _data = data;
    }

    /**
     * Set up HTTP Basic auth on the PendingRequest.
     * @param request    The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     * @throws PolicyAssertionException if processing should not continue due to a serious error
     */
    public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
        String username = request.getSsg().getUsername();
        char[] password = request.getSsg().password();
        if (username == null || password == null || username.length() < 1) {
            log.info("HttpBasic: no credentials configured for the SSG " + request.getSsg());
            request.setCredentialsWouldHaveHelped(true);
            return AssertionStatus.AUTH_REQUIRED;
        }
        request.setBasicAuthRequired(true);
        request.setHttpBasicUsername(username);
        request.setHttpBasicPassword(password);
        log.info("HttpBasic: setting credentials for SSG " + request.getSsg());
        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) throws PolicyAssertionException {
        // no action on response
        return AssertionStatus.NONE;
    }

    protected HttpBasic _data;
}
