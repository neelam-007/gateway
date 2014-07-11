package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.external.assertions.xmppassertion.XMPPGetAssociatedSessionIdAssertion;
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
 * Time: 2:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerXMPPGetAssociatedSessionIdAssertion extends AbstractServerAssertion<XMPPGetAssociatedSessionIdAssertion> {
    private static final Logger logger = Logger.getLogger(ServerXMPPSendToRemoteHostAssertion.class.getName());

    private final XMPPGetAssociatedSessionIdAssertion assertion;
    private final String[] variablesUsed;

    public ServerXMPPGetAssociatedSessionIdAssertion(XMPPGetAssociatedSessionIdAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;
        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException
    {
        long sessionId = -1;

        try {
            String sessionIdString = null;
            if(assertion.getSessionId() != null) {
                sessionIdString = ExpandVariables.process(assertion.getSessionId(), context.getVariableMap(variablesUsed, getAudit()), getAudit());
            }

            if(sessionIdString != null && !sessionIdString.trim().isEmpty()) {
                sessionId = Long.parseLong(sessionIdString);
            }
        } catch(NumberFormatException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "The inbound session ID was not a valid number.");
            return AssertionStatus.FAILED;
        }

        if(sessionId == -1) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING, "The specified session ID was not valid.");
            return AssertionStatus.FAILED;
        }

        Long otherSessionId = null;
        if(assertion.isInbound()) {
            otherSessionId = XMPPConnectionManager.getInstance().getAssociatedServerSessionId(sessionId);
        } else {
            otherSessionId = XMPPConnectionManager.getInstance().getAssociatedClientSessionId(sessionId);
        }

        if(otherSessionId == null) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING, "An associated session could not be found for the provided session ID.");
            return AssertionStatus.FAILED;
        }

        context.setVariable(assertion.getVariableName(), otherSessionId);
        return AssertionStatus.NONE;
    }
}
