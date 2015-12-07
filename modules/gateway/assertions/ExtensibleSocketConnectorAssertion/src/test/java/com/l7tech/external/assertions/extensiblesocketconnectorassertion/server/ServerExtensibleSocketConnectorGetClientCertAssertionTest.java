package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorGetClientCertAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.ApplicationContext;

import java.net.InetSocketAddress;
import java.security.cert.X509Certificate;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by mobna01 on 27/04/15.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Message.class)
public class ServerExtensibleSocketConnectorGetClientCertAssertionTest {

    @Test
    public void testClientCertAssertion() throws Exception {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        PolicyEnforcementContext peCtx = mock(PolicyEnforcementContext.class);
        ExtensibleSocketConnectorGetClientCertAssertion assertion = mock(ExtensibleSocketConnectorGetClientCertAssertion.class);

        Message message = mock(Message.class);
        PowerMockito.mockStatic(Message.class);

        InetSocketAddress localAddress = mock(InetSocketAddress.class);
        InetSocketAddress remoteAddress = mock(InetSocketAddress.class);
        X509Certificate cert = mock(X509Certificate.class);
        SslSocketTcpKnob knob = new SslSocketTcpKnob(localAddress, remoteAddress, cert);

        when(message.getKnob(SslSocketTcpKnob.class)).thenReturn(knob);
        when(peCtx.getRequest()).thenReturn(message);

        ServerExtensibleSocketConnectorGetClientCertAssertion serverAssertion = new ServerExtensibleSocketConnectorGetClientCertAssertion(assertion, applicationContext);
        AssertionStatus status = serverAssertion.checkRequest(peCtx);

        assertEquals(AssertionStatus.NONE, status);
    }
}
