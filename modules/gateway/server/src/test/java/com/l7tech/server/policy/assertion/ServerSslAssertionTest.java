package com.l7tech.server.policy.assertion;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
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

    @Test
    @BugId("SSG-6435")
    public void testSsg6435AllowGatherExpiredClientCert() throws Exception {
        testGatherClientCert(TestDocuments.getExpiredServerCertificate(), false, AssertionStatus.NONE, false);
    }

    @Test
    @BugId("SSG-6435")
    public void testSsg6435DisallowGatherExpiredClientCert() throws Exception {
        testGatherClientCert(TestDocuments.getExpiredServerCertificate(), true, AssertionStatus.AUTH_FAILED, true);
    }

    @Test
    @BugId("SSG-6435")
    public void testSsg6435AllowGatherNonExpiredClientCert() throws Exception {
        testGatherClientCert(TestDocuments.getDotNetServerCertificate(), true, AssertionStatus.NONE, false);
    }

    @Test
    @BugId("SSG-6435")
    public void testSsg6435AllowGatherNonExpiredClientCertWithoutValidityCheck() throws Exception {
        testGatherClientCert(TestDocuments.getDotNetServerCertificate(), false, AssertionStatus.NONE, false);
    }

    private static void testGatherClientCert(X509Certificate clientCert, boolean checkValidity, AssertionStatus expectedAssertionStatus, boolean expectedPolicyViolated) throws Exception {
        SslAssertion sa = new SslAssertion();
        sa.setOption(SslAssertion.REQUIRED);
        sa.setRequireClientAuthentication(true);
        sa.setCheckCertValidity(checkValidity);
        ServerSslAssertion ssa = new ServerSslAssertion(sa);

        PolicyEnforcementContext context = contextWithTlsClientCert(clientCert);

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
