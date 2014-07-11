package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.external.assertions.xmppassertion.XMPPGetRemoteCertificateAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 23/03/12
 * Time: 12:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerXMPPGetRemoteCertificateAssertion extends AbstractServerAssertion<XMPPGetRemoteCertificateAssertion> {
    private static final Logger logger = Logger.getLogger(ServerXMPPSendToRemoteHostAssertion.class.getName());

    private final XMPPGetRemoteCertificateAssertion assertion;
    private final String[] variablesUsed;

    public ServerXMPPGetRemoteCertificateAssertion(XMPPGetRemoteCertificateAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;
        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException
    {
        try {
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
            
            X509Certificate cert = XMPPConnectionManager.getInstance().getRemoteCertificate(sessionIdLong, assertion.isInbound());
            context.setVariable(assertion.getVariableName(), cert);
            return AssertionStatus.NONE;
        } catch(NumberFormatException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "The expression \"" + assertion.getSessionId() + "\" did not evaluate to a number.");
            return AssertionStatus.FAILED;
        } catch(InboundSessionNotFoundException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "The specified XMPP inbound connection was not found.");
            return AssertionStatus.FAILED;
        } catch(OutboundSessionNotFoundException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "The specified XMPP outgoing connection was not found.");
            return AssertionStatus.FAILED;
        } catch(TLSNotStartedException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING, "The session has not yet started TLS communication.");
            return AssertionStatus.FAILED;
        }
    }
}
