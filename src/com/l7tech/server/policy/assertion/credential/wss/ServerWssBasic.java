/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.wss;

import com.l7tech.common.security.xml.WssProcessor;
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
import java.util.logging.Level;
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
        if (!(request instanceof SoapRequest)) {
            throw new PolicyAssertionException("This type of assertion is only supported with SOAP type of messages");
        }
        SoapRequest req = (SoapRequest)request;
        WssProcessor.ProcessorResult wssResults = req.getWssProcessorOutput();
        if (wssResults == null) {
            throw new PolicyAssertionException("This request was not processed for WSS level security.");
        }
        WssProcessor.SecurityToken[] tokens = wssResults.getSecurityTokens();
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].asObject() instanceof LoginCredentials) {
                LoginCredentials creds = (LoginCredentials)tokens[i].asObject();
                if (creds.getFormat() == CredentialFormat.CLEARTEXT) {
                    request.setPrincipalCredentials(creds);
                    return AssertionStatus.NONE;
                }
            }
        }
        logger.log(Level.SEVERE, "cannot find credentials");
        return AssertionStatus.AUTH_REQUIRED;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
