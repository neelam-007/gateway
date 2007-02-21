package com.l7tech.external.assertions.whichmodule.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.whichmodule.HelloWorldAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerRoutingAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

/** @noinspection unchecked*/
public class ServerHelloWorldAssertion extends ServerRoutingAssertion {
    private final byte[] greetBytes;

    public ServerHelloWorldAssertion(HelloWorldAssertion assertion, ApplicationContext context) {
        super(assertion, context);
        greetBytes = ("<greeting>Hello, " + assertion.getNameToGreet() + "!</greeting>").getBytes();
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        context.getResponse().initialize(ContentTypeHeader.XML_DEFAULT, greetBytes);
        context.setRoutingStatus(RoutingStatus.ROUTED);
        return AssertionStatus.NONE;
    }
}
