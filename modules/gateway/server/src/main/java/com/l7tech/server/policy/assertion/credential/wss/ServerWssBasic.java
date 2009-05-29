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
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.message.Message;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * TODO [steve] auditing for target message
 *
 * @author alex
 * @version $Revision$
 */
public class ServerWssBasic extends AbstractMessageTargetableServerAssertion<WssBasic> {

    //- PUBLIC

    public ServerWssBasic(final WssBasic data, final ApplicationContext springContext) {
        super(data, data);
        this.auditor = new Auditor(this, springContext, logger);
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException
    {
        if (!assertion.getRecipientContext().localRecipient()) {
            auditor.logAndAudit(AssertionMessages.WSS_BASIC_FOR_ANOTHER_RECIPIENT);
            return AssertionStatus.NONE;
        }

        return super.checkRequest( context );
    }

    //- PROTECTED

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authContext ) throws IOException, PolicyAssertionException {
        ProcessorResult wssResults;
        try {
            if (!message.isSoap()) {
                auditor.logAndAudit(AssertionMessages.WSS_BASIC_NOT_SOAP);
                return AssertionStatus.NOT_APPLICABLE;
            }
            wssResults = message.getSecurityKnob().getProcessorResult();
        } catch (SAXException e) {
            throw new CausedIOException("Message '"+messageDescription+"' declared as XML but is not well-formed", e);
        }
        if (wssResults == null) {
            auditor.logAndAudit(AssertionMessages.WSS_BASIC_NO_CREDENTIALS);
            
            if ( isRequest() ) {
                context.setAuthenticationMissing();
                context.setRequestPolicyViolated();
            }
            return AssertionStatus.AUTH_REQUIRED;
        }

        XmlSecurityToken[] tokens = wssResults.getXmlSecurityTokens();
        for (XmlSecurityToken token : tokens) {
            if (token instanceof UsernameToken) {
                UsernameToken ut = (UsernameToken) token;

                String user = ut.getUsername();
                char[] pass = ut.getPassword();
                if (pass == null) pass = new char[0];
                LoginCredentials creds = LoginCredentials.makePasswordCredentials(user, pass, WssBasic.class);
                authContext.addCredentials(creds);
                return AssertionStatus.NONE;
            }
        }
        auditor.logAndAudit(AssertionMessages.WSS_BASIC_CANNOT_FIND_CREDENTIALS);
        // we get here because there were no credentials found in the format we want
        // therefore this assertion was violated
        if ( isRequest() ) {
            context.setRequestPolicyViolated();
        }
        return AssertionStatus.AUTH_REQUIRED;
    }

    @Override
    protected Auditor getAuditor() {
        return auditor;
    }

    //- PRIVATE

    private final Logger logger = Logger.getLogger(getClass().getName());

    private final Auditor auditor;
}
