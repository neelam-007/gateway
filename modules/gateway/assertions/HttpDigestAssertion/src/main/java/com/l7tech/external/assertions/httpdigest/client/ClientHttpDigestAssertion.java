/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.external.assertions.httpdigest.client;

import com.l7tech.external.assertions.httpdigest.HttpDigestAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.proxy.datamodel.exceptions.HttpChallengeRequiredException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertionWithMetaSupport;

import java.net.PasswordAuthentication;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision: 24416 $
 */
public class ClientHttpDigestAssertion extends ClientAssertionWithMetaSupport {
    public static final Logger log = Logger.getLogger(ClientHttpDigestAssertion.class.getName());

    public ClientHttpDigestAssertion( HttpDigestAssertion data ) {
        super(data);
        this.data = data;
    }

    /**
     * ClientProxy client-side processing of the given request.
     * @param context
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     */
    public AssertionStatus decorateRequest(PolicyApplicationContext context)
            throws OperationCanceledException, HttpChallengeRequiredException {
        if (context.getSsg().isFederatedGateway()) {
            log.info("this is a Federated SSG.  Assertion therefore fails.");
            return AssertionStatus.FAILED;
        }
        PasswordAuthentication pw = context.getCachedCredentialsForTrustedSsg();
        if (pw == null || pw.getUserName() == null || pw.getUserName().length() < 1) {
            log.info("HttpDigest: No username/password credentials available for HTTP digest.  Assertion therefore fails.");
            context.getDefaultAuthenticationContext().setAuthenticationMissing();
            return AssertionStatus.FAILED;
        }
        context.setDigestAuthRequired(true);
        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context) {
        // no action on response
        return AssertionStatus.NONE;
    }

    protected HttpDigestAssertion data;
}

