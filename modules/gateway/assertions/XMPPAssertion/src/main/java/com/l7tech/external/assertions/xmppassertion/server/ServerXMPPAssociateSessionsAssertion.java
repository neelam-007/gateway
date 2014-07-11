package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.external.assertions.xmppassertion.XMPPAssociateSessionsAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 03/04/12
 * Time: 12:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerXMPPAssociateSessionsAssertion extends AbstractServerAssertion<XMPPAssociateSessionsAssertion> {
    private static final Logger logger = Logger.getLogger(ServerXMPPSendToRemoteHostAssertion.class.getName());

    private final XMPPAssociateSessionsAssertion assertion;
    private final String[] variablesUsed;

    public ServerXMPPAssociateSessionsAssertion(XMPPAssociateSessionsAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;
        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException
    {
        long clientSessionId = -1;
        long serverSessionId = -1;

        try {
            String sessionId = null;
            if(assertion.getInboundSessionId() != null) {
                sessionId = ExpandVariables.process(assertion.getInboundSessionId(), context.getVariableMap(variablesUsed, getAudit()), getAudit());
            }

            if(sessionId != null && !sessionId.trim().isEmpty()) {
                clientSessionId = Long.parseLong(sessionId);
            }
        } catch(NumberFormatException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "The inbound session ID was not a valid number.");
            return AssertionStatus.FAILED;
        }

        try {
            String sessionId = null;
            if(assertion.getOutboundSessionId() != null) {
                sessionId = ExpandVariables.process(assertion.getOutboundSessionId(), context.getVariableMap(variablesUsed, getAudit()), getAudit());
            }

            if(sessionId != null && !sessionId.trim().isEmpty()) {
                serverSessionId = Long.parseLong(sessionId);
            }
        } catch(NumberFormatException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "The outbound session ID was not a valid number.");
            return AssertionStatus.FAILED;
        }

        if(clientSessionId == -1) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING, "The specified inbound session ID was not valid.");
            return AssertionStatus.FAILED;
        }
        if(serverSessionId == -1) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING, "The specified outbound session ID was not valid.");
            return AssertionStatus.FAILED;
        }

        XMPPConnectionManager.getInstance().associateSessions(clientSessionId, serverSessionId);
        return AssertionStatus.NONE;
    }
}
