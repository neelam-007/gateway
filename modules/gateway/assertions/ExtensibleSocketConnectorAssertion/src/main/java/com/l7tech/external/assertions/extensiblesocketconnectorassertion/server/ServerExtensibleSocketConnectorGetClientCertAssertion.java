package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorGetClientCertAssertion;
import com.l7tech.message.Message;
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
 * Date: 30/03/12
 * Time: 1:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerExtensibleSocketConnectorGetClientCertAssertion extends AbstractServerAssertion<ExtensibleSocketConnectorGetClientCertAssertion> {
    private static final Logger logger = Logger.getLogger(ServerExtensibleSocketConnectorGetClientCertAssertion.class.getName());

    private final ExtensibleSocketConnectorGetClientCertAssertion assertion;

    public ServerExtensibleSocketConnectorGetClientCertAssertion(ExtensibleSocketConnectorGetClientCertAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Message request = context.getRequest();
        if (request != null) {
            SslSocketTcpKnob sslKnob = request.getKnob(SslSocketTcpKnob.class);
            if (sslKnob != null && sslKnob.getClientCert() != null) {
                context.setVariable(assertion.getVariableName(), sslKnob.getClientCert());
            }
        }

        return AssertionStatus.NONE;
    }
}
