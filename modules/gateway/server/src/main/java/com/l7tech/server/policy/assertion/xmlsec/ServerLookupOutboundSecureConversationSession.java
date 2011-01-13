package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.identity.User;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.LookupOutboundSecureConversationSession;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.secureconversation.OutboundSecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.server.secureconversation.SessionLookupException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author ghuang
 */
public class ServerLookupOutboundSecureConversationSession extends AbstractMessageTargetableServerAssertion<LookupOutboundSecureConversationSession> {
    private static final Logger logger = Logger.getLogger(ServerLookupOutboundSecureConversationSession.class.getName());

    private final Auditor auditor;
    private final String[] variablesUsed;
    private final OutboundSecureConversationContextManager securityContextManager;

    public ServerLookupOutboundSecureConversationSession(final LookupOutboundSecureConversationSession assertion, final BeanFactory factory) {
        super(assertion, assertion);

        auditor = factory instanceof ApplicationContext ?
                new Auditor(this, (ApplicationContext)factory, logger) :
                new LogOnlyAuditor(logger);

        variablesUsed = assertion.getVariablesUsed();
        
        securityContextManager = factory.getBean("outboundSecureConversationContextManager", OutboundSecureConversationContextManager.class);
    }

    @Override
    protected AssertionStatus doCheckRequest(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext) throws IOException, PolicyAssertionException {
        // Get the authenticated user
        final User user = authContext.getLastAuthenticatedUser();
        if (user == null) {
             auditor.logAndAudit(AssertionMessages.OUTBOUND_SECURE_CONVERSATION_LOOKUP_FAILURE, "The target message does not contain an authenticated user.");
            return AssertionStatus.FALSIFIED;
        }

        // Get User ID and Service URL
        String userId = user.getId();
        String serviceUrl = ExpandVariables.process(assertion.getServiceUrl(), context.getVariableMap(variablesUsed, auditor), auditor);  // serviceUrl won't be null.  So no check for it.

        // Lookup the outbound session
        SecureConversationSession session;
        try {
            session = securityContextManager.getSession(userId, serviceUrl);
        } catch (SessionLookupException e) {
            auditor.logAndAudit(AssertionMessages.OUTBOUND_SECURE_CONVERSATION_LOOKUP_FAILURE, e.getMessage());
            return AssertionStatus.FALSIFIED;
        }

        // Set variable
        String varFullName = assertion.getVariablePrefix() + LookupOutboundSecureConversationSession.VARIABLE_SESSION;
        context.setVariable(varFullName, session);

        return AssertionStatus.NONE;
    }

    @Override
    protected Audit getAuditor() {
        return auditor;
    }
}