/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.composite;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.ClientCertificateException;
import com.l7tech.proxy.datamodel.exceptions.ResponseValidationException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.HttpChallengeRequiredException;
import com.l7tech.proxy.datamodel.exceptions.PolicyRetryableException;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.util.ClientLogger;
import org.xml.sax.SAXException;

import java.security.GeneralSecurityException;
import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientExactlyOneAssertion extends ClientCompositeAssertion {
    private static final ClientLogger log = ClientLogger.getInstance(ClientExactlyOneAssertion.class);

    public ClientExactlyOneAssertion( ExactlyOneAssertion data ) {
        super( data );
        this.data = data;
    }

    /**
     * Modify the provided PendingRequest to conform to this policy assertion.
     * For ExactlyOneAssertion, we'll run children until one succeeds or we run out.
     * @param req
     * @return AssertionStatus.NONE, or the rightmost-child's error if all children failed.
     */
    public AssertionStatus decorateRequest(PendingRequest req) throws OperationCanceledException, BadCredentialsException, GeneralSecurityException, IOException, ClientCertificateException, SAXException, KeyStoreCorruptException, HttpChallengeRequiredException, PolicyRetryableException, PolicyAssertionException {
        data.mustHaveChildren();
        AssertionStatus result = AssertionStatus.FALSIFIED;
        for ( int i = 0; i < children.length; i++ ) {
            ClientAssertion assertion = children[i];
            AssertionStatus thisResult = assertion.decorateRequest(req);
            if (thisResult == AssertionStatus.NONE)
                return thisResult;
            result = thisResult;
        }
        if (result != AssertionStatus.NONE)
            rollbackPendingDecorations(req);
        return result;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response)
            throws OperationCanceledException, BadCredentialsException, GeneralSecurityException,
                   IOException, ResponseValidationException, SAXException, KeyStoreCorruptException, PolicyAssertionException
    {
        data.mustHaveChildren();
        AssertionStatus result = AssertionStatus.FALSIFIED;
        for ( int i = 0; i < children.length; i++ ) {
            ClientAssertion assertion = children[i];
            AssertionStatus thisResult = assertion.unDecorateReply(request, response);
            if (thisResult == AssertionStatus.NONE)
                return thisResult;
            result = thisResult;
        }
        if (result != AssertionStatus.NONE)
            rollbackPendingDecorations(request);
        return result;
    }

    public String getName() {
        return "Exactly one assertion must evaluate to true";
    }

    protected ExactlyOneAssertion data;
}
