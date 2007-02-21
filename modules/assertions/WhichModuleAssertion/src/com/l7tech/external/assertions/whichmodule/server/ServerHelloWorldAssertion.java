package com.l7tech.external.assertions.whichmodule.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerRoutingAssertion;
import com.l7tech.external.assertions.whichmodule.HelloWorldAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

public class ServerHelloWorldAssertion extends ServerRoutingAssertion<HelloWorldAssertion> {
    public ServerHelloWorldAssertion(HelloWorldAssertion assertion, ApplicationContext context) {
        super(assertion, context);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        context.getResponse().initialize(ContentTypeHeader.XML_DEFAULT,
                                         ("<greeting>Hello, " + getAssertion().getNameToGreet() + "!</greeting>").getBytes());
        context.setRoutingStatus(RoutingStatus.ROUTED);
        return AssertionStatus.NONE;
    }
}
