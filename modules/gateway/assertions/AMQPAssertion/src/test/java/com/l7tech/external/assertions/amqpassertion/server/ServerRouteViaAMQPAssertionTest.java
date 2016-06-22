package com.l7tech.external.assertions.amqpassertion.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.amqpassertion.RouteViaAMQPAssertion;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test the RouteViaAMQPAssertion.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerRouteViaAMQPAssertionTest {

    @Mock
    private static ApplicationContext applicationContext;
    @Mock
    private ServerAMQPDestinationManager manager;
    private PolicyEnforcementContext peCtx;
    private RouteViaAMQPAssertion assertion;

    @Before
    public void setUp() {
        // Get the policy enforcement context
        peCtx = makeContext("<myrequest/>", "<myresponse/>");
        ServerAMQPDestinationManager.setInstance(manager);
        when(manager.queueMessage(any(Goid.class), anyString(), any(Message.class), any(Message.class), any(HashMap.class))).thenReturn(true);

        assertion = new RouteViaAMQPAssertion();
        assertion.setSsgActiveConnectorGoid(new Goid(1, 0));
        assertion.setRequestTarget(new MessageTargetableSupport(TargetMessageType.REQUEST));
        assertion.setResponseTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE));
        assertion.setRoutingKeyExpression("my-routing-key");
    }

    @Test
    public void testSuccessWithDefaulRequestAndResponse() throws Exception {
        ServerRouteViaAMQPAssertion serverAssertion = new ServerRouteViaAMQPAssertion(assertion, applicationContext);
        AssertionStatus status = serverAssertion.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testSuccessWithDefaulRequestAndResponseWithContextVariable() throws Exception {
        assertion.setRoutingKeyExpression("PREFIX-${var1}-BODY-${var2}-SUFFIX");
        peCtx.setVariable("var1", "One");
        peCtx.setVariable("var2", "Two");

        ServerRouteViaAMQPAssertion serverAssertion = new ServerRouteViaAMQPAssertion(assertion, applicationContext);
        AssertionStatus status = serverAssertion.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testSuccessWithOtherRequestAndDefaultResponse() throws Exception {
        assertion.setRequestTarget(new MessageTargetableSupport("myMsg"));
        Message message = new Message();
        message.initialize(XmlUtil.stringAsDocument("<myrequest/>"));
        peCtx.setVariable("myMsg", message);

        ServerRouteViaAMQPAssertion serverAssertion = new ServerRouteViaAMQPAssertion(assertion, applicationContext);
        AssertionStatus status = serverAssertion.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testSuccessWithOtherRequestAndOtherResponse() throws Exception {
        assertion.setRequestTarget(new MessageTargetableSupport("myRequest"));
        assertion.setResponseTarget(new MessageTargetableSupport("myResponse"));
        Message message = new Message();
        message.initialize(XmlUtil.stringAsDocument("<myrequest/>"));
        peCtx.setVariable("myRequest", message);
        message.initialize(XmlUtil.stringAsDocument("<myresponse/>"));
        peCtx.setVariable("myResponse", message);

        ServerRouteViaAMQPAssertion serverAssertion = new ServerRouteViaAMQPAssertion(assertion, applicationContext);
        AssertionStatus status = serverAssertion.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testSuccessWithOtherRequestAndInvalidResponseVar() throws Exception {
        assertion.setRequestTarget(new MessageTargetableSupport("myRequest"));
        assertion.setResponseTarget(new MessageTargetableSupport("myResponse"));
        Message message = new Message();
        message.initialize(XmlUtil.stringAsDocument("<myrequest/>"));
        peCtx.setVariable("myRequest", message);

        ServerRouteViaAMQPAssertion serverAssertion = new ServerRouteViaAMQPAssertion(assertion, applicationContext);
        AssertionStatus status = serverAssertion.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testFailWithInvalidRequest() throws Exception {
        assertion.setRequestTarget(new MessageTargetableSupport("myRequest"));
        assertion.setResponseTarget(new MessageTargetableSupport("myResponse"));

        ServerRouteViaAMQPAssertion serverAssertion = new ServerRouteViaAMQPAssertion(assertion, applicationContext);
        AssertionStatus status = serverAssertion.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void testFailWhenQueueingFails() throws Exception {
        when(manager.queueMessage(any(Goid.class), anyString(), any(Message.class), any(Message.class), any(HashMap.class))).thenReturn(false);

        ServerRouteViaAMQPAssertion serverAssertion = new ServerRouteViaAMQPAssertion(assertion, applicationContext);
        AssertionStatus status = serverAssertion.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    private PolicyEnforcementContext makeContext(String req, String res) {
        Message request = new Message();
        request.initialize(XmlUtil.stringAsDocument(req));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument(res));
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }
}
