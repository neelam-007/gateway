/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.composite;

import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientExactlyOneAssertion extends ClientCompositeAssertion {
    public ClientExactlyOneAssertion( ExactlyOneAssertion data ) {
        super( data );
        this.data = data;
    }

    /**
     * Modify the provided PendingRequest to conform to this policy assertion.
     * For ExactlyOneAssertion, we'll run children until one succeeds or we run out.
     * @param context
     * @return AssertionStatus.NONE, or the rightmost-child's error if all children failed.
     */
    public AssertionStatus decorateRequest(PolicyApplicationContext context) throws OperationCanceledException, BadCredentialsException, GeneralSecurityException, IOException, ClientCertificateException, SAXException, KeyStoreCorruptException, HttpChallengeRequiredException, PolicyRetryableException, PolicyAssertionException, InvalidDocumentFormatException {
        mustHaveChildren(data);
        AssertionStatus result = AssertionStatus.FALSIFIED;
        for ( int i = 0; i < children.length; i++ ) {
            ClientAssertion assertion = children[i];
            AssertionStatus thisResult = assertion.decorateRequest(context);
            if (thisResult == AssertionStatus.NONE)
                return thisResult;
            result = thisResult;
        }
        if (result != AssertionStatus.NONE)
            rollbackPendingDecorations(context);
        return result;
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context)
            throws OperationCanceledException, BadCredentialsException, GeneralSecurityException,
            IOException, ResponseValidationException, SAXException, KeyStoreCorruptException, PolicyAssertionException, InvalidDocumentFormatException
    {
        mustHaveChildren(data);
        AssertionStatus result = AssertionStatus.FALSIFIED;
        for ( int i = 0; i < children.length; i++ ) {
            ClientAssertion assertion = children[i];
            AssertionStatus thisResult = assertion.unDecorateReply(context);
            if (thisResult == AssertionStatus.NONE)
                return thisResult;
            result = thisResult;
        }
        if (result != AssertionStatus.NONE)
            rollbackPendingDecorations(context);
        return result;
    }

    public String getName() {
        return "Exactly one assertion must evaluate to true";
    }

    protected ExactlyOneAssertion data;
}
