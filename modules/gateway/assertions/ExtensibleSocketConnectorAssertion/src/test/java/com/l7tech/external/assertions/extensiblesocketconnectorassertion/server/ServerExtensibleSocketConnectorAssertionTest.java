package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorAssertion;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.ApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: ashah
 * Date: 02/05/12
 * Time: 4:57 PM
 * To change this template use File | Settings | File Templates.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(SocketConnectorManager.class)
public class ServerExtensibleSocketConnectorAssertionTest {
    private ApplicationContext applicationContext;
    private ExtensibleSocketConnectorAssertion assertion;
    private PolicyEnforcementContext peCtx;
    private MessageTargetableSupport responseTarget;
    private static SocketConnectorManager manager;

    private ServerExtensibleSocketConnectorAssertion serverExtensibleSocketConnectorAssertion;

    @Before
    public void setUp() throws Exception {
        applicationContext = mock(ApplicationContext.class);
        MessageTargetableSupport requestTarget = new MessageTargetableSupport(TargetMessageType.REQUEST, false);
        responseTarget = new MessageTargetableSupport(TargetMessageType.RESPONSE, false);
        Message message = new Message();

        assertion = new ExtensibleSocketConnectorAssertion();
        assertion.setRequestTarget(requestTarget);
        assertion.setResponseTarget(responseTarget);
        assertion.setSocketConnectorGoid(new Goid(1, 0));

        peCtx = mock(PolicyEnforcementContext.class);
        when(peCtx.getResponse()).thenReturn(message);
        when(peCtx.getRequest()).thenReturn(message);
        when(peCtx.getTargetMessage(assertion.getRequestTarget())).thenReturn(message);

        SocketConnectorManager.OutgoingMessageResponse outgoingMessageResponse = mock(SocketConnectorManager.OutgoingMessageResponse.class);
        PowerMockito.mockStatic(SocketConnectorManager.OutgoingMessageResponse.class);

        manager = mock(SocketConnectorManager.class);
        PowerMockito.mockStatic(SocketConnectorManager.class);
        when(SocketConnectorManager.getInstance()).thenReturn(manager);
        when(manager.sendMessage(any(Message.class), any(Goid.class), any(String.class), any(Boolean.class))).thenReturn(outgoingMessageResponse);
        when(outgoingMessageResponse.getContentType()).thenReturn("text/plain");
        when(outgoingMessageResponse.getMessageBytes()).thenReturn("message".getBytes());

        serverExtensibleSocketConnectorAssertion = new ServerExtensibleSocketConnectorAssertion(assertion, applicationContext);
    }

    @Test
    public void testResponseTargetInitialization() throws Exception {
        responseTarget.setTarget(TargetMessageType.RESPONSE);

        AssertionStatus status = serverExtensibleSocketConnectorAssertion.checkRequest(peCtx);

        assertEquals(AssertionStatus.NONE, status);
        assertEquals(7, peCtx.getResponse().getMimeKnob().getContentLength());
        assertEquals("text/plain", peCtx.getResponse().getMimeKnob().getOuterContentType().getMainValue());
    }

    @Test
    public void testRequestTargetInitialization() throws Exception {
        responseTarget.setTarget(TargetMessageType.REQUEST);
        AssertionStatus status = serverExtensibleSocketConnectorAssertion.checkRequest(peCtx);

        assertEquals(AssertionStatus.NONE, status);
        assertEquals(7, peCtx.getRequest().getMimeKnob().getContentLength());
        assertEquals("text/plain", peCtx.getRequest().getMimeKnob().getOuterContentType().getMainValue());
    }

    @Test
    public void testOthertTargetInitialization() throws Exception {
        responseTarget.setTarget(TargetMessageType.OTHER);
        responseTarget.setOtherTargetMessageVariable("msg");

        AssertionStatus status = serverExtensibleSocketConnectorAssertion.checkRequest(peCtx);

        assertEquals(AssertionStatus.NONE, status);
    }

}
