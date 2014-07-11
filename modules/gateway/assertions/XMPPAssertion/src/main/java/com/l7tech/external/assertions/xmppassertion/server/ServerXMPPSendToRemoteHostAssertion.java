package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.xmppassertion.XMPPSendToRemoteHostAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerRoutingAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 12/03/12
 * Time: 10:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class ServerXMPPSendToRemoteHostAssertion extends ServerRoutingAssertion<XMPPSendToRemoteHostAssertion> {
    private static final Logger logger = Logger.getLogger(ServerXMPPSendToRemoteHostAssertion.class.getName());

    private final XMPPSendToRemoteHostAssertion assertion;
    private final String[] variablesUsed;

    public ServerXMPPSendToRemoteHostAssertion(XMPPSendToRemoteHostAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion, context);

        this.assertion = assertion;
        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException
    {
        try {
            Message request = context.getTargetMessage(assertion.getRequestTarget());
            String sessionId = null;
            if(assertion.getSessionId() != null) {
                sessionId = ExpandVariables.process(assertion.getSessionId(), context.getVariableMap(variablesUsed, getAudit()), getAudit());
            }
            
            long sessionIdLong = -1;
            if(sessionId != null && !sessionId.trim().isEmpty()) {
                sessionIdLong = Long.parseLong(sessionId);
            }
            
            if(sessionIdLong == -1) {
                getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING, "The specified session ID was not valid.");
                return AssertionStatus.FAILED;
            }

            if(assertion.isToOutboundConnection()) {
                sessionIdLong = XMPPConnectionManager.getInstance().sendMessageToServer(request, sessionIdLong);
            } else {
                XMPPConnectionManager.getInstance().sendMessageToClient(request, sessionIdLong);
            }
            return AssertionStatus.NONE;
        } catch(NoSuchVariableException e) {
            getAudit().logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, assertion.getRequestTarget().getOtherTargetMessageVariable());
            return AssertionStatus.FAILED;
        } catch(NumberFormatException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "The expression \"" + assertion.getSessionId() + "\" did not evaluate to a number.");
            return AssertionStatus.FAILED;
        } catch(ConnectionConfigNotFoundException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "The specified XMPP connection configuration no longer exists.");
            return AssertionStatus.FAILED;
        } catch(InboundSessionNotFoundException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "The specified XMPP inbound connection was not found.");
            return AssertionStatus.FAILED;
        } catch(OutboundSessionNotFoundException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "The specified XMPP outgoing connection was not found.");
            return AssertionStatus.FAILED;
        } catch(NoSuchPartException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "An error occurred while reading the input message.");
            return AssertionStatus.FAILED;
        }
    }
}
