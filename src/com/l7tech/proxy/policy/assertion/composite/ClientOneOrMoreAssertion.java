/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.composite;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.ClientCertificateException;
import com.l7tech.proxy.datamodel.exceptions.ResponseValidationException;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import org.apache.log4j.Category;
import org.xml.sax.SAXException;

import java.security.GeneralSecurityException;
import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientOneOrMoreAssertion extends ClientCompositeAssertion {
    private static final Category log = Category.getInstance(ClientOneOrMoreAssertion.class);

    public ClientOneOrMoreAssertion( OneOrMoreAssertion data ) {
        super( data );
        this.data = data;
    }

    /**
     * Modify the provided PendingRequest to conform to this policy assertion.
     * For OneOrMoreAssertion, we'll run children only until one succeeds (or we run out of children).
     * @param req
     * @return the AssertionStatus.NONE if at least one child succeeded; the rightmost-child error otherwise.
     */
    public AssertionStatus decorateRequest(PendingRequest req) throws OperationCanceledException, BadCredentialsException, GeneralSecurityException, IOException, ClientCertificateException, SAXException {
        try {
            data.mustHaveChildren();
        } catch (PolicyAssertionException e) {
            throw new RuntimeException(e);
        }
        AssertionStatus result = AssertionStatus.FAILED;
        for ( int i = 0; i < children.length; i++ ) {
            ClientAssertion assertion = children[i];
            result = assertion.decorateRequest(req);
            if (result == AssertionStatus.NONE)
                return result;
        }
        return result;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response)
            throws OperationCanceledException, BadCredentialsException, GeneralSecurityException, IOException,
                   ResponseValidationException, SAXException
    {
        try {
            data.mustHaveChildren();
        } catch (PolicyAssertionException e) {
            throw new RuntimeException(e);
        }
        AssertionStatus result = AssertionStatus.FAILED;
        for ( int i = 0; i < children.length; i++ ) {
            ClientAssertion assertion = children[i];
            result = assertion.unDecorateReply(request, response);
            if (result == AssertionStatus.NONE)
                return result;
        }
        return result;
    }

    protected OneOrMoreAssertion data;
}
