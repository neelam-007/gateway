package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.external.assertions.xmppassertion.XMPPCloseSessionAssertion;
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
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 14/03/12
 * Time: 3:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerXMPPCloseSessionAssertion extends AbstractServerAssertion<XMPPCloseSessionAssertion> {
    private static final Logger logger = Logger.getLogger(ServerXMPPSendToRemoteHostAssertion.class.getName());

    private final XMPPCloseSessionAssertion assertion;
    private final String[] variablesUsed;

    public ServerXMPPCloseSessionAssertion(XMPPCloseSessionAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;
        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException
    {
        try {
            String value = ExpandVariables.process(assertion.getSessionId(), context.getVariableMap(variablesUsed, getAudit()), getAudit());
            if(value == null) {
                getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING, "Invalid session ID.");
                return AssertionStatus.FAILED;
            }
            
            long sessionId = Long.parseLong(value);

            if(assertion.isInbound()) {
                XMPPConnectionManager.getInstance().closeClientConnection(sessionId);
            } else {
                XMPPConnectionManager.getInstance().closeServerConnection(sessionId);
            }
            return AssertionStatus.NONE;
        } catch(NumberFormatException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING, "Invalid session ID.");
            return AssertionStatus.FAILED;
        } catch(InboundSessionNotFoundException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING, "The inbound session was not found.");
            return AssertionStatus.FAILED;
        } catch(OutboundSessionNotFoundException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING, "The outbound session was not found.");
            return AssertionStatus.FAILED;
        }
    }
}
