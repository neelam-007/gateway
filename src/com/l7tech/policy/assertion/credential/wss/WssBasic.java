/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.wss;

import com.l7tech.credential.CredentialFinderException;
import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.credential.wss.WssBasicCredentialFinder;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.datamodel.PendingRequest;

/**
 * @author alex
 * @version $Revision$
 */
public class WssBasic extends WssCredentialSourceAssertion {
    public AssertionStatus checkCredentials(Request request, Response response) throws CredentialFinderException {
        // this is only called once we have credentials
        // there is nothing more to check here, if the creds were not in the right format,
        // the WssBasicCredentialFinder would not have returned credentials

        // (just to make sure)
        PrincipalCredentials pc = request.getPrincipalCredentials();
        // yes, we're good
        if (pc != null) return AssertionStatus.NONE;
        else return AssertionStatus.AUTH_REQUIRED;
    }

    public Class getCredentialFinderClass() {
        return WssBasicCredentialFinder.class;
    }

    /**
     * decorate the xml soap message with a WSS header containing the username and password
     * @param request
     * @return
     * @throws PolicyAssertionException
     */
    public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
        String username = request.getSsg().getUsername();
        char[] password = request.getSsg().getPassword();
        if (username == null || password == null || username.length() < 1) {
            //log.info("HttpBasic: no credentials configured for the SSG " + request.getSsg());
            request.setCredentialsWouldHaveHelped(true);
            return AssertionStatus.AUTH_REQUIRED;
        }
        // todo, what if there is already such a header element? let's make sure we dont override it
        // add the sec element to the request
        // org.apache.axis.message.SOAPHeaderElement soapheader = new org.apache.axis.message.SOAPHeaderElement(element);
        // todo, change this return value
        return AssertionStatus.NOT_YET_IMPLEMENTED;
    }
}
