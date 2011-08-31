package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.identity.User;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.LookupOutboundSecureConversationSession;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.secureconversation.OutboundSecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import org.springframework.beans.factory.BeanFactory;

import java.io.IOException;

/**
 * @author ghuang
 */
public class ServerLookupOutboundSecureConversationSession extends AbstractMessageTargetableServerAssertion<LookupOutboundSecureConversationSession> {
    private final String[] variablesUsed;
    private final OutboundSecureConversationContextManager securityContextManager;

    public ServerLookupOutboundSecureConversationSession(final LookupOutboundSecureConversationSession assertion, final BeanFactory factory) {
        super(assertion);
        variablesUsed = assertion.getVariablesUsed();
        securityContextManager = factory.getBean("outboundSecureConversationContextManager", OutboundSecureConversationContextManager.class);
    }

    @Override
    protected AssertionStatus doCheckRequest(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext) throws IOException, PolicyAssertionException {
        // Get the authenticated user
        final User user = authContext.getLastAuthenticatedUser();
        if (user == null) {
             logAndAudit(AssertionMessages.OUTBOUND_SECURE_CONVERSATION_LOOKUP_FAILURE, "The target message does not contain an authenticated user.");
            return AssertionStatus.FALSIFIED;
        }

        // Get User ID and Service URL
        final String serviceUrl = ExpandVariables.process(assertion.getServiceUrl(), context.getVariableMap(variablesUsed, getAudit()), getAudit());  // serviceUrl won't be null.  So no check for it.

        // Lookup the outbound session
        SecureConversationSession session;
        try {
            session = securityContextManager.getSession( OutboundSecureConversationContextManager.newSessionKey( user, serviceUrl ) );
        } catch ( IllegalArgumentException e ) {
            logAndAudit(AssertionMessages.OUTBOUND_SECURE_CONVERSATION_LOOKUP_FAILURE, e.getMessage());
            return AssertionStatus.FALSIFIED;
        }

        if (session == null) {
            logAndAudit(AssertionMessages.OUTBOUND_SECURE_CONVERSATION_LOOKUP_FAILURE, "The session (with User ID: " + user.getId() + " and Service URL: " + serviceUrl + ") is either expired or not found.");
            return AssertionStatus.FALSIFIED;
        }

        // Set variable
        StringBuilder varFullName = new StringBuilder(assertion.getVariablePrefix()).append(".").append(LookupOutboundSecureConversationSession.VARIABLE_SESSION);
        context.setVariable(varFullName.toString(), session);

        return AssertionStatus.NONE;
    }
}