/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.composite;

import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.AllAssertion;
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
public class ClientAllAssertion extends ClientCompositeAssertion {
    public ClientAllAssertion( AllAssertion data ) {
        super( data );
        this.data = data;
    }

    /**
     * Modify the provided PendingRequest to conform to this policy assertion.
     * For an AllAssertion, we'll have all our children decorate the request.
     * @param context
     * @return the AssertionStatus.NONE if no child returned an error; the rightmost-child error otherwise.
     */
    public AssertionStatus decorateRequest(PolicyApplicationContext context) throws OperationCanceledException, BadCredentialsException, GeneralSecurityException, IOException, ClientCertificateException, SAXException, KeyStoreCorruptException, HttpChallengeRequiredException, PolicyRetryableException, PolicyAssertionException, InvalidDocumentFormatException {
        mustHaveChildren(data);
        AssertionStatus result = AssertionStatus.NONE;
        for ( int i = 0; i < children.length; i++ ) {
            ClientAssertion assertion = children[i];
            result = assertion.decorateRequest(context);
            if (result != AssertionStatus.NONE) {
                rollbackPendingDecorations(context);
                return result;
            }
        }
        return result;
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context)
            throws OperationCanceledException, BadCredentialsException, GeneralSecurityException,
            IOException, ResponseValidationException, SAXException, KeyStoreCorruptException, PolicyAssertionException, InvalidDocumentFormatException
    {
        mustHaveChildren(data);
        AssertionStatus result = AssertionStatus.NONE;
        for ( int i = 0; i < children.length; i++ ) {
            ClientAssertion assertion = children[i];
            result = assertion.unDecorateReply(context);
            if (result != AssertionStatus.NONE) {
                rollbackPendingDecorations(context);
                return result;
            }
        }
        return result;
    }

    public String getName() {
        return "All assertions must evaluate to true";
    }

    protected AllAssertion data;
}
