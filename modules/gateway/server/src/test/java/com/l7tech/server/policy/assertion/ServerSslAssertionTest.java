package com.l7tech.server.policy.assertion;

import com.l7tech.server.ApplicationContexts;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.junit.Test;
import static org.junit.Assert.*;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

/**
 *
 */
public class ServerSslAssertionTest {

    @Test
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

        request.initialize( XmlUtil.stringToDocument("<blah/>"));
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        response.attachHttpResponseKnob(new HttpServletResponseKnob(hresponse));

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        ssa.checkRequest(context);

        assertTrue(context.isRequestPolicyViolated());
    }
}
