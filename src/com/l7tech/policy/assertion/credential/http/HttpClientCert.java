/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.http;

import com.l7tech.credential.CredentialFinderException;
import com.l7tech.credential.http.HttpDigestCredentialFinder;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionResult;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.ClientKeyManager;

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

import org.apache.log4j.Category;

/**
 * @author alex
 * @version $Revision$
 */
public class HttpClientCert extends HttpCredentialSourceAssertion {
    private static final Category log = Category.getInstance(HttpClientCert.class);

    public AssertionStatus doCheckRequest(Request request, Response response)
            throws CredentialFinderException
    {
        X509Certificate clientCert = (X509Certificate)
                request.getTransportMetadata().getParameter("javax.servlet.request.X509Certificate");

        if (clientCert == null) {
            response.setPolicyViolated(true);
            response.addResult(new AssertionResult(this, request, AssertionStatus.NOT_FOUND,
                                                   "No Client Certificate was present in the request."));
            return AssertionStatus.NOT_FOUND;
        }

        try {
            clientCert.checkValidity();
        } catch (CertificateExpiredException e) {
            response.addResult(new AssertionResult(this, request, AssertionStatus.NOT_FOUND,
                                                   "Client Certificate has expired", e));
            return AssertionStatus.NOT_FOUND;
        } catch (CertificateNotYetValidException e) {
            response.addResult(new AssertionResult(this, request, AssertionStatus.NOT_FOUND,
                                                   "Client Certificate is not yet valid", e));
            return AssertionStatus.NOT_FOUND;
        }

        // Get DN from cert, ie "CN=testuser, OU=ssg.example.com"
        String certDn = clientCert.getSubjectDN().getName();

        // TODO: whatever needs to be done to make this work.
        return AssertionStatus.NOT_YET_IMPLEMENTED;
    }

    public Class getCredentialFinderClass() {
        return HttpDigestCredentialFinder.class;
    }

    /**
     * ClientProxy client-side processing of the given request.
     * @param request    The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     * @throws PolicyAssertionException if processing should not continue due to a serious error
     */
    public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
        if (ClientKeyManager.isClientCertAvailabile(request.getSsg())) {
            request.setClientCertRequired(true);
            log.info("We appear to possess a Client Certificate for this SSG.");
            return AssertionStatus.NONE;
        }

        if (request.getSsg().getUsername() == null || request.getSsg().getPassword() == null ||
                request.getSsg().getUsername().length() < 1)
            request.setCredentialsWouldHaveHelped(true);
        request.setClientCertWouldHaveHelped(true);
        log.info("We do not appear to have a Client Certificate for this SSG.");
        return AssertionStatus.NOT_FOUND;
    }
}
