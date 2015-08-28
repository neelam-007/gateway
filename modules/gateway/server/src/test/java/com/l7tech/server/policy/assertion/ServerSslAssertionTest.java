package com.l7tech.server.policy.assertion;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.*;
import com.l7tech.objectmodel.mqtt.MQTTQOS2Proxy;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.cert.X509Certificate;

import static org.junit.Assert.*;

/**
 *
 */
public class ServerSslAssertionTest {

    private static X509Certificate expiredServerCertificate;
    private static X509Certificate dotNetServerCertificate;

    @BeforeClass
    public static void beforeClass() throws Exception {
        expiredServerCertificate = TestDocuments.getExpiredServerCertificate();
        dotNetServerCertificate = TestDocuments.getDotNetServerCertificate();
    }

    @Test
    @BugId("SSG-6435")
    public void testSsg6435AllowGatherExpiredClientCert() throws Exception {
        testGatherClientCert(contextWithTlsClientCert(expiredServerCertificate), false, AssertionStatus.NONE, false);
    }

    @Test
    @BugId("SSG-6435")
    public void testSsg6435DisallowGatherExpiredClientCert() throws Exception {
        testGatherClientCert(contextWithTlsClientCert(expiredServerCertificate), true, AssertionStatus.AUTH_FAILED, true);
    }

    @Test
    @BugId("SSG-6435")
    public void testSsg6435AllowGatherNonExpiredClientCert() throws Exception {
        testGatherClientCert(contextWithTlsClientCert(dotNetServerCertificate), true, AssertionStatus.NONE, false);
    }

    @Test
    @BugId("SSG-6435")
    public void testSsg6435AllowGatherNonExpiredClientCertWithoutValidityCheck() throws Exception {
        testGatherClientCert(contextWithTlsClientCert(dotNetServerCertificate), false, AssertionStatus.NONE, false);
    }

    @Test
    public void testMqttWithInvalidCertAllowExpired() throws Exception {
        testGatherClientCert(mqttContextWithTlsClientCert(expiredServerCertificate), false, AssertionStatus.NONE, false);
    }

    @Test
    public void testMqttWithInvalidCert() throws Exception {
        testGatherClientCert(mqttContextWithTlsClientCert(expiredServerCertificate), true, AssertionStatus.AUTH_FAILED, true);
    }

    @Test
    public void testMqttWithValidCert() throws Exception {
        testGatherClientCert(mqttContextWithTlsClientCert(dotNetServerCertificate), true, AssertionStatus.NONE, false);
    }

    @Test
    public void testMqttWithValidCertAllowExpired() throws Exception {
        testGatherClientCert(mqttContextWithTlsClientCert(dotNetServerCertificate), false, AssertionStatus.NONE, false);
    }

    @Test
    public void testMqttWithNoCert() throws Exception {
        testGatherClientCert(mqttContextWithTlsClientCert(null), true, AssertionStatus.AUTH_REQUIRED, true);
    }

    private static void testGatherClientCert(PolicyEnforcementContext context, boolean checkValidity, AssertionStatus expectedAssertionStatus, boolean expectedPolicyViolated) throws Exception {
        SslAssertion sa = new SslAssertion();
        sa.setOption(SslAssertion.REQUIRED);
        sa.setRequireClientAuthentication(true);
        sa.setCheckCertValidity(checkValidity);
        ServerSslAssertion ssa = new ServerSslAssertion(sa);

        AssertionStatus status = ssa.checkRequest(context);
        assertEquals(expectedAssertionStatus, status);
        assertEquals(expectedPolicyViolated, context.isRequestPolicyViolated());
    }

    private static PolicyEnforcementContext contextWithTlsClientCert(final X509Certificate clientCert) throws SAXException {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse hresponse = new MockHttpServletResponse();

        Message request = new Message();
        Message response = new Message();

        request.initialize( XmlUtil.stringToDocument("<blah/>"));
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest) {
            @Override
            public boolean isSecure() {
                return true;
            }

            @Override
            public X509Certificate[] getClientCertificate() throws IOException {
                return new X509Certificate[] { clientCert };
            }
        });
        response.attachHttpResponseKnob(new HttpServletResponseKnob(hresponse));

        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    private static PolicyEnforcementContext mqttContextWithTlsClientCert(final X509Certificate cert) throws SAXException {
        Message request = new Message();
        request.initialize(XmlUtil.stringToDocument("<blah/>"));
        request.attachKnob(new MQTTRequestKnob() {

            @NotNull
            @Override
            public MessageType getMessageType() {
                return null;
            }

            @Nullable
            @Override
            public String getClientIdentifier() {
                return null;
            }

            @Nullable
            @Override
            public String getUserName() {
                return "blah";
            }

            @Nullable
            @Override
            public String getUserPassword() {
                return null;
            }

            @Override
            public boolean isSecure() {
                return true;
            }

            @Nullable
            @Override
            public X509Certificate[] getClientCertificate() {
                return new X509Certificate[]{cert};
            }

            @Nullable
            @Override
            public MQTTConnectParameters getMQTTConnectParameters() {
                return null;
            }

            @Nullable
            @Override
            public MQTTDisconnectParameters getMQTTDisconnectParameters() {
                return null;
            }

            @Nullable
            @Override
            public MQTTPublishParameters getMQTTPublishParameters() {
                return null;
            }

            @Nullable
            @Override
            public MQTTSubscribeParameters getMQTTSubscribeParameters() {
                return null;
            }

            @Nullable
            @Override
            public MQTTUnsubscribeParameters getMQTTUnsubscribeParameters() {
                return null;
            }

            @Nullable
            @Override
            public MQTTQOS2Proxy getMQTTQOS2Proxy() {
                return null;
            }

            @Override
            public String getRemoteAddress() {
                return null;
            }

            @Override
            public String getRemoteHost() {
                return null;
            }

            @Override
            public int getRemotePort() {
                return 0;
            }

            @Override
            public String getLocalAddress() {
                return null;
            }

            @Override
            public String getLocalHost() {
                return null;
            }

            @Override
            public int getLocalPort() {
                return 0;
            }

            @Override
            public int getLocalListenerPort() {
                return 0;
            }
        }, MQTTRequestKnob.class, TlsKnob.class, TcpKnob.class);

        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
    }

    @Test
    @BugId("SSG-2042")
    @BugNumber(4327)
    public void testBug4327PolicyViolationNotFlaggedIfNotSsl() throws Exception {
        SslAssertion sa = new SslAssertion();
        sa.setOption(SslAssertion.REQUIRED);
        sa.setRequireClientAuthentication(true);
        ServerSslAssertion ssa = new ServerSslAssertion(sa);
        
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse hresponse = new MockHttpServletResponse();

        Message request = new Message();
        Message response = new Message();

        request.initialize( XmlUtil.stringToDocument("<blah/>"));
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        response.attachHttpResponseKnob(new HttpServletResponseKnob(hresponse));

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        ssa.checkRequest(context);

        assertTrue(context.isRequestPolicyViolated());
    }
}
