/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.wss;

import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.token.UsernameToken;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerWssBasic implements ServerAssertion {
    private WssBasic data;
    public ServerWssBasic(WssBasic data) {
        this.data = data;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (!data.getRecipientContext().localRecipient()) {
            logger.fine("This is intended for another recipient, nothing to validate.");
            return AssertionStatus.NONE;
        }
        ProcessorResult wssResults;
        try {
            if (!context.getRequest().isSoap()) {
                logger.info("Request not SOAP: Cannot check for WS-Security UsernameToken");
                return AssertionStatus.NOT_APPLICABLE;
            }
            wssResults = context.getRequest().getXmlKnob().getProcessorResult();
        } catch (SAXException e) {
            throw new CausedIOException("Request declared as XML but is not well-formed", e);
        }
        if (wssResults == null) {
            logger.info("Request did not include WSS Basic credentials.");
            context.setAuthenticationMissing(true);
            context.setPolicyViolated(true);
            return AssertionStatus.AUTH_REQUIRED;
        }
        SecurityToken[] tokens = wssResults.getSecurityTokens();
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i] instanceof UsernameToken) {
                LoginCredentials creds = ((UsernameToken)tokens[i]).asLoginCredentials();
                if (creds.getFormat() == CredentialFormat.CLEARTEXT) {
                    creds.setCredentialSourceAssertion(WssBasic.class);
                    context.setCredentials(creds);
                    return AssertionStatus.NONE;
                }
            }
        }
        logger.info("cannot find credentials");
        // we get here because there were no credentials found in the format we want
        // therefore this assertion was violated
        context.setPolicyViolated(true);
        return AssertionStatus.AUTH_REQUIRED;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
