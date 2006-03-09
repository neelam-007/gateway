package com.l7tech.server.policy.assertion;

import com.l7tech.common.message.HttpResponseKnob;
import com.l7tech.common.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.EchoRoutingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Assertion that echoes the request into the response
 * @author emil
 * @version 21-Mar-2005
 */
public class ServerEchoRoutingAssertion extends ServerRoutingAssertion {
    public ServerEchoRoutingAssertion(EchoRoutingAssertion ea, ApplicationContext applicationContext) {
        super(ea, applicationContext);
    }

    /**
     * SSG Server-side processing of the given request.
     *
     * @param context
     * @return AssertionStatus.NONE if this Assertion did its business successfully; otherwise, some error code
     * @throws com.l7tech.policy.assertion.PolicyAssertionException
     *          something is wrong in the policy dont throw this if there is an issue with the request or the response
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Message response = context.getResponse();
        HttpResponseKnob hrk = (HttpResponseKnob)response.getKnob(HttpResponseKnob.class);
        response.initialize(context.getRequest());
        response.attachHttpResponseKnob(hrk);
        hrk.setStatus(HttpServletResponse.SC_OK);
        return AssertionStatus.NONE;
    }
}
