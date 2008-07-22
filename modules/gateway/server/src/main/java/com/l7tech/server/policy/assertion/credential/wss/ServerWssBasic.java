/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.wss;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.security.token.UsernameToken;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.util.CausedIOException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerWssBasic extends AbstractServerAssertion implements ServerAssertion {
    final private WssBasic data;
    private final Auditor auditor;

    public ServerWssBasic(WssBasic data, ApplicationContext springContext) {
        super(data);
        this.data = data;
        this.auditor = new Auditor(this, springContext, logger);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
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
            wssResults = context.getRequest().getSecurityKnob().getProcessorResult();
        } catch (SAXException e) {
            throw new CausedIOException("Request declared as XML but is not well-formed", e);
        }
        if (wssResults == null) {
            auditor.logAndAudit(AssertionMessages.WSS_BASIC_NO_CREDENTIALS);
            context.setAuthenticationMissing();
            context.setRequestPolicyViolated();
            return AssertionStatus.AUTH_REQUIRED;
        }

        XmlSecurityToken[] tokens = wssResults.getXmlSecurityTokens();
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i] instanceof UsernameToken) {
                UsernameToken ut = (UsernameToken)tokens[i];

                String user = ut.getUsername();
                char[] pass = ut.getPassword();
                if (pass == null) pass = new char[0];
                LoginCredentials creds = LoginCredentials.makePasswordCredentials(user, pass, WssBasic.class);
                context.addCredentials(creds);
                return AssertionStatus.NONE;
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
