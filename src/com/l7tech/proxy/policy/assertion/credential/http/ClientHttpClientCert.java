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
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import org.apache.log4j.Category;

import java.security.GeneralSecurityException;
import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientHttpClientCert extends ClientAssertion {
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
    public AssertionStatus decorateRequest(PendingRequest request)
            throws PolicyAssertionException, OperationCanceledException, BadCredentialsException
    {
        Ssg ssg = request.getSsg();
        if (!ssg.isCredentialsConfigured())
            Managers.getCredentialManager().getCredentials(ssg);
        if (!SsgKeyStoreManager.isClientCertAvailabile(ssg)) {
            log.info("ClientHttpClientCert: applying for client certificate");
            try {
                request.getClientProxy().obtainClientCertificate(ssg);
            } catch (GeneralSecurityException e) {
                throw new PolicyAssertionException("Unable to obtain a client certificate with SSG " + ssg, e);
            } catch (IOException e) {
                throw new PolicyAssertionException("Unable to obtain a client certificate with SSG " + ssg, e);
            }
        }
        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) {
        // no action on response
        return AssertionStatus.NONE;
    }

    protected HttpClientCert data;
}
