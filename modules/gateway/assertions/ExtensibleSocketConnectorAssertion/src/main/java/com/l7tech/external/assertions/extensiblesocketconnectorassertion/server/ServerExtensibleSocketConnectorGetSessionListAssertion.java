package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorGetSessionListAssertion;
import com.l7tech.gateway.common.audit.Messages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 24/01/14
 * Time: 4:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerExtensibleSocketConnectorGetSessionListAssertion extends AbstractServerAssertion<ExtensibleSocketConnectorGetSessionListAssertion> {
    private static final Logger logger = Logger.getLogger(ServerExtensibleSocketConnectorGetSessionListAssertion.class.getName());

    private ExtensibleSocketConnectorGetSessionListAssertion assertion = null;

    public ServerExtensibleSocketConnectorGetSessionListAssertion(final ExtensibleSocketConnectorGetSessionListAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);
        this.assertion = assertion;
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        List<String> sessionIdList = null;
        try {
            sessionIdList = SocketConnectorManager.getInstance().getSessionsFromConnection(assertion.getSocketConnectorGoid());
            context.setVariable(assertion.getTargetVariable(), sessionIdList);
        } catch (Exception e) {
            getAudit().logAndAudit(
                    Messages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[] {"Error while getting remote socket sessions: " + ExceptionUtils.getMessageWithCause(e) },
                    e);
            return AssertionStatus.FAILED;
        }

        return AssertionStatus.NONE;
    }
}
