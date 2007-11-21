package com.l7tech.server.policy.assertion;

import com.l7tech.common.ApplicationContexts;
import com.l7tech.common.message.HttpServletRequestKnob;
import com.l7tech.common.message.HttpServletResponseKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import java.util.logging.Logger;

/**
 *
 */
public class ServerSslAssertionTest extends TestCase {
    private static final Logger log = Logger.getLogger(ServerSslAssertionTest.class.getName());


    public ServerSslAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ServerSslAssertionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testBug4327PolicyViolationNotFlaggedIfNotSsl() throws Exception {
        SslAssertion sa = new SslAssertion();
        sa.setOption(SslAssertion.REQUIRED);
        sa.setRequireClientAuthentication(true);
        ServerSslAssertion ssa = new ServerSslAssertion(sa, ApplicationContexts.getTestApplicationContext());
        
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse hresponse = new MockHttpServletResponse();

        Message request = new Message();
        Message response = new Message();

        request.initialize(XmlUtil.stringToDocument("<blah/>"));
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        response.attachHttpResponseKnob(new HttpServletResponseKnob(hresponse));

        PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
        ssa.checkRequest(context);

        assertTrue(context.isRequestPolicyViolated());
    }
}
