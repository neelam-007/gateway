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
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.logging.LogManager;

import java.util.logging.Logger;

import org.apache.log4j.Category;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientHttpBasic extends ClientAssertion {
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
    public AssertionStatus decorateRequest(PendingRequest request)
            throws PolicyAssertionException, OperationCanceledException
    {
        Ssg ssg = request.getSsg();
        if (!ssg.isCredentialsConfigured())
            Managers.getCredentialManager().getCredentials(ssg);
        request.setBasicAuthRequired(true);
        log.info("HttpBasic: will use HTTP basic on this request to " + request.getSsg());
        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) {
        // no action on response
        return AssertionStatus.NONE;
    }

    protected HttpBasic _data;
}
