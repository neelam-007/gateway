/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.credential.http;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import org.apache.log4j.Category;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientHttpClientCert implements ClientAssertion {
    private static final Category log = Category.getInstance(ClientHttpClientCert.class);
    public ClientHttpClientCert( HttpClientCert data ) {
        this.data = data;
    }

    /**
     * ClientProxy client-side processing of the given request.
     * @param request    The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     * @throws com.l7tech.policy.assertion.PolicyAssertionException if processing should not continue due to a serious error
     */
    public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
        if (SsgKeyStoreManager.isClientCertAvailabile(request.getSsg())) {
            request.setClientCertRequired(true);
            log.info("We appear to possess a Client Certificate for this SSG.");
            return AssertionStatus.NONE;
        }

        if (request.getSsg().getUsername() == null || request.getSsg().password() == null ||
                request.getSsg().getUsername().length() < 1)
            request.setCredentialsWouldHaveHelped(true);
        request.setClientCertWouldHaveHelped(true);
        log.info("We do not appear to have a Client Certificate for this SSG.");
        return AssertionStatus.AUTH_REQUIRED;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) throws PolicyAssertionException {
        // no action on response
        return AssertionStatus.NONE;
    }

    protected HttpClientCert data;
}
