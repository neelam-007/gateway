/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.credential.http;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.HttpChallengeRequiredException;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertionWithMetaSupport;

import java.net.PasswordAuthentication;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientHttpBasic extends ClientAssertionWithMetaSupport {
    private static final Logger log = Logger.getLogger(ClientHttpBasic.class.getName());

    public ClientHttpBasic( HttpBasic data ) {
        super(data);
        _data = data;
    }

    /**
     * Set up HTTP Basic auth on the PendingRequest.
     * @param context
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     */
    public AssertionStatus decorateRequest(PolicyApplicationContext context)
            throws OperationCanceledException, HttpChallengeRequiredException
    {
        if (context.getSsg().isFederatedGateway()) {
            log.info("this is a Federated SSG.  Assertion therefore fails.");
            return AssertionStatus.FAILED;
        }

        PasswordAuthentication pw = context.getCachedCredentialsForTrustedSsg();
        if (pw == null || pw.getUserName() == null || pw.getUserName().length() < 1) {
            log.info("HttpBasic: no username/password credentials available.  Assertion therefore fails.");
            context.getDefaultAuthenticationContext().setAuthenticationMissing();
            return AssertionStatus.FAILED;
        }

        context.setBasicAuthRequired(true);
        if (!context.getClientSidePolicy().isPlaintextAuthAllowed())
            context.setSslRequired(true); // force SSL when using HTTP Basic
        log.info("HttpBasic: will use HTTP basic on this request to " + context.getSsg());
        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context) {
        // no action on response
        return AssertionStatus.NONE;
    }

    protected HttpBasic _data;
}
