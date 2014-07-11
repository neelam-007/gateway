package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.external.assertions.xmppassertion.XMPPStartTLSAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 16/03/12
 * Time: 12:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerXMPPStartTLSAssertion extends AbstractServerAssertion<XMPPStartTLSAssertion> {
    private static final Logger logger = Logger.getLogger(ServerXMPPSendToRemoteHostAssertion.class.getName());

    private final XMPPStartTLSAssertion assertion;
    private final String[] variablesUsed;

    public ServerXMPPStartTLSAssertion(XMPPStartTLSAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;
        variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException
    {
        try {
            long sessionId = Long.parseLong(ExpandVariables.process(assertion.getSessionId(), context.getVariableMap(variablesUsed, getAudit()), getAudit()));
            XMPPConnectionManager.getInstance().startTLSForSession(sessionId, assertion, context.getTargetMessage(assertion.getRequestTarget()));

            return AssertionStatus.NONE;
        } catch(NumberFormatException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "The session ID was not valid.");
            return AssertionStatus.FAILED;
        } catch(OutboundSessionNotFoundException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "The outbound session was not found.");
            return AssertionStatus.FAILED;
        } catch(InboundSessionNotFoundException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "The inbound session was not found.");
            return AssertionStatus.FAILED;
        } catch(NoSuchVariableException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "The request target was a context variable, but the variable is not set.");
            return AssertionStatus.FAILED;
        }
    }
}
