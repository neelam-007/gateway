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
import com.l7tech.common.audit.Auditor;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.AssertionMessages;
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
        Auditor auditor = new Auditor(context.getAuditContext(), logger);

        if (!data.getRecipientContext().localRecipient()) {
            auditor.logAndAudit(AssertionMessages.WSS_BASIC_FOR_ANOTHER_RECIPIENT);
            return AssertionStatus.NONE;
        }
        ProcessorResult wssResults;
        try {
            if (!context.getRequest().isSoap()) {
                auditor.logAndAudit(AssertionMessages.WSS_BASIC_NOT_SOAP);
                return AssertionStatus.NOT_APPLICABLE;
            }
            wssResults = context.getRequest().getXmlKnob().getProcessorResult();
        } catch (SAXException e) {
            throw new CausedIOException("Request declared as XML but is not well-formed", e);
        }
        if (wssResults == null) {
            auditor.logAndAudit(AssertionMessages.WSS_BASIC_NO_CREDENTIALS);
            context.setAuthenticationMissing();
            context.setRequestPolicyViolated();
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
        auditor.logAndAudit(AssertionMessages.WSS_BASIC_CANNOT_FIND_CREDENTIALS);
        // we get here because there were no credentials found in the format we want
        // therefore this assertion was violated
        context.setRequestPolicyViolated();
        return AssertionStatus.AUTH_REQUIRED;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
