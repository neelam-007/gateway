/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.exceptions.*;

import java.security.GeneralSecurityException;
import java.io.IOException;

import org.xml.sax.SAXException;

/**
 * Interface implemented by anything that is capable of applying decorations to a PendingRequest.
 */
public interface ClientDecorator {
    /**
     * ClientProxy client-side processing of the given request.
     * @param request    The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     * @throws com.l7tech.policy.assertion.PolicyAssertionException if the policy was invalid
     */
    AssertionStatus decorateRequest(PendingRequest request)
            throws BadCredentialsException, OperationCanceledException, GeneralSecurityException,
            ClientCertificateException, IOException, SAXException, KeyStoreCorruptException,
            HttpChallengeRequiredException, PolicyRetryableException, PolicyAssertionException;
}
