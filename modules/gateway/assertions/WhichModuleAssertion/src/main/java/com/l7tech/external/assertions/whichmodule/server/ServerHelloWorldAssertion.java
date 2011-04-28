package com.l7tech.external.assertions.whichmodule.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.whichmodule.HelloWorldAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerRoutingAssertion;
import com.l7tech.message.MessageRole;
import com.l7tech.message.Message;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Logger;

public class ServerHelloWorldAssertion extends ServerRoutingAssertion<HelloWorldAssertion> {
    protected static final Logger logger = Logger.getLogger(ServerHelloWorldAssertion.class.getName());

    private final byte[] greetBytes;

    public ServerHelloWorldAssertion(HelloWorldAssertion assertion, ApplicationContext context) {
        super(assertion, context, logger);
        greetBytes = ("<greeting>Hello, " + assertion.getNameToGreet() + "!</greeting>").getBytes();
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Message request = context.getRequest();
        Message response = context.getResponse();

        response.initialize(ContentTypeHeader.XML_DEFAULT, greetBytes);

        // todo: move to abstract routing assertion
        request.notifyMessage(response, MessageRole.RESPONSE);
        response.notifyMessage(request, MessageRole.REQUEST);

        context.setRoutingStatus(RoutingStatus.ROUTED);
        return AssertionStatus.NONE;
    }
}
