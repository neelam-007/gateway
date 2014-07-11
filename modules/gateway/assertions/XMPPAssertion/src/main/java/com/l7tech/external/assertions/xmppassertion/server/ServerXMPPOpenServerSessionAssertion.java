package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.external.assertions.xmppassertion.XMPPOpenServerSessionAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
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
public class ServerXMPPOpenServerSessionAssertion extends AbstractServerAssertion<XMPPOpenServerSessionAssertion> {
    private static final Logger logger = Logger.getLogger(ServerXMPPSendToRemoteHostAssertion.class.getName());

    private final XMPPOpenServerSessionAssertion assertion;

    public ServerXMPPOpenServerSessionAssertion(XMPPOpenServerSessionAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException
    {
        try {
            long sessionId = XMPPConnectionManager.getInstance().connectToServer(assertion.getXMPPConnectionId());

            if(sessionId == -1) {
                getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Unable to connect to the remote XMPP server.");
                return AssertionStatus.FAILED;
            }

            context.setVariable(XMPPOpenServerSessionAssertion.OUTBOUND_SESSION_ID_VAR_NAME, Long.toString(sessionId));
            return AssertionStatus.NONE;
        } catch(ConnectionConfigNotFoundException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "The outbound XMPP connection was not found.");
            return AssertionStatus.FAILED;
        }
    }
}
