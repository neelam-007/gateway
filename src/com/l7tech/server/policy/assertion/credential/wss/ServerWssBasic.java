/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.wss;

import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.SecurityToken;
import com.l7tech.common.security.xml.processor.UsernameToken;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.SoapRequest;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.server.policy.assertion.ServerAssertion;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerWssBasic implements ServerAssertion {
    public ServerWssBasic(WssBasic data) {
        // nothing interesting in here
    }

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        if (!(request instanceof SoapRequest) || !((SoapRequest)request).isSoap()) {
            logger.info("This type of assertion is only supported with SOAP type of messages");
            return AssertionStatus.NOT_APPLICABLE;
        }
        SoapRequest req = (SoapRequest)request;
        ProcessorResult wssResults = req.getWssProcessorOutput();
        if (wssResults == null) {
            throw new PolicyAssertionException("This request was not processed for WSS level security.");
        }
        SecurityToken[] tokens = wssResults.getSecurityTokens();
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i] instanceof UsernameToken) {
                LoginCredentials creds = ((UsernameToken)tokens[i]).asLoginCredentials();
                if (creds.getFormat() == CredentialFormat.CLEARTEXT) {
                    creds.setCredentialSourceAssertion(WssBasic.class);
                    request.setPrincipalCredentials(creds);
                    return AssertionStatus.NONE;
                }
            }
        }
        logger.info("cannot find credentials");
        return AssertionStatus.AUTH_REQUIRED;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
